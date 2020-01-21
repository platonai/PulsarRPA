package ai.platon.pulsar.proxy

import ai.platon.pulsar.common.NetUtil
import ai.platon.pulsar.common.RuntimeUtils
import ai.platon.pulsar.common.SimpleLogger
import ai.platon.pulsar.common.StringUtil
import ai.platon.pulsar.common.config.AppConstants.*
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.proxy.NoProxyException
import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.common.proxy.ProxyPool
import com.github.monkeywie.proxyee.exception.HttpProxyExceptionHandle
import com.github.monkeywie.proxyee.intercept.HttpProxyInterceptInitializer
import com.github.monkeywie.proxyee.intercept.HttpProxyInterceptPipeline
import com.github.monkeywie.proxyee.intercept.common.FullRequestIntercept
import com.github.monkeywie.proxyee.intercept.common.FullResponseIntercept
import com.github.monkeywie.proxyee.proxy.ProxyConfig
import com.github.monkeywie.proxyee.proxy.ProxyType
import com.github.monkeywie.proxyee.server.HttpProxyServer
import com.github.monkeywie.proxyee.server.HttpProxyServerConfig
import io.netty.channel.Channel
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponse
import io.netty.util.ResourceLeakDetector
import org.slf4j.LoggerFactory
import org.springframework.util.SocketUtils
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.min

class InternalProxyServer(
        private val proxyPool: ProxyPool,
        private val conf: ImmutableConfig
): AutoCloseable {
    private val log = LoggerFactory.getLogger(InternalProxyServer::class.java)

    private var forwardServer: HttpProxyServer? = null
    private var forwardServerThread: Thread? = null
    private val watcherThread = Thread(this::startWatcher)
    private val watcherStarted = AtomicBoolean()
    private val closed = AtomicBoolean()
    private val connected = AtomicBoolean()
    private val connectedLock: Lock = ReentrantLock()
    private val connectedCond: Condition = connectedLock.newCondition()
    private val disconnectedCond: Condition = connectedLock.newCondition()
    private val condPollingInterval = Duration.ofMillis(100)
    private val condTimeout = Duration.ofMinutes(1)
    var idleTimeout = conf.getDuration(PROXY_INTERNAL_SERVER_IDLE_TIMEOUT, Duration.ofMinutes(5))

    private val numBossGroupThreads = conf.getInt(PROXY_INTERNAL_SERVER_BOSS_THREADS, 1)
    private val numWorkerGroupThreads = conf.getInt(PROXY_INTERNAL_SERVER_WORKER_THREADS, 2)
    private val httpProxyServerConfig = HttpProxyServerConfig()

    val isEnabled get() = ProxyPool.isProxyEnabled() && conf.getBoolean(PROXY_ENABLE_INTERNAL_SERVER, true)
    val isDisabled get() = !isEnabled
    var port = -1
        private set
    var numTotalConnects = 0
        private set
    val numRunningTasks = AtomicInteger()
    // TODO: thread safety?
    var lastActiveTime = Instant.now()
    var idleTime = Duration.ZERO
    private var idleCount = 0
    var report: String = ""
    var showReport = false
    val isWatcherStarted get() = watcherStarted.get()
    val isConnected get() = connected.get()
    val isClosed get() = closed.get()

    var lastProxyEntry: ProxyEntry? = null
        private set
    var proxyEntry: ProxyEntry? = null
        private set

    init {
        httpProxyServerConfig.bossGroupThreads = numBossGroupThreads
        httpProxyServerConfig.workerGroupThreads = numWorkerGroupThreads
        httpProxyServerConfig.isHandleSsl = false
    }

    @Synchronized
    fun start() {
        if (isDisabled) {
            log.warn("IPS is disabled")
            return
        }

        if (watcherStarted.compareAndSet(false, true)) {
            watcherThread.isDaemon = true
            watcherThread.start()
        }
    }

    inline fun <R> runAnyway(task: () -> R): R {
        return if (isDisabled) {
            task()
        } else {
            run(task)
        }
    }

    inline fun <R> run(task: () -> R): R {
        if (isClosed || isDisabled) {
            throw NoProxyException("IPS is " + if (isClosed) "closed" else "disabled")
        }

        idleTime = Duration.ZERO
        numRunningTasks.incrementAndGet()

        if (!ensureAvailable()) {
            throw NoProxyException("Failed to wait for IPS to be available")
        }

        return try {
            task()
        } catch (e: Exception) {
            throw e
        } finally {
            lastActiveTime = Instant.now()
            numRunningTasks.decrementAndGet()
        }
    }

    fun ensureAvailable(): Boolean {
        if (isDisabled || isClosed) {
            return false
        }

        connectedLock.withLock {
            if (isConnected) {
                return true
            }

            log.info("Waiting for IPS to connect ...")

            try {
                var signaled = false
                var round = 0
                while (!isClosed && !isConnected && round++ < condTimeout.toMillis()) {
                    signaled = connectedCond.await(condPollingInterval.toMillis(), TimeUnit.MILLISECONDS)
                }

                if (!signaled && !isClosed && !isConnected) {
                    log.warn("Timeout to wait for IPS to be ready after $round round")
                }
            } catch (e: InterruptedException) {
                log.warn("Interrupted from waiting for IPS")
                Thread.currentThread().interrupt()
            }
        }

        return !isClosed && isConnected
    }

    fun changeProxyIfRunning(excludedProxy: ProxyEntry) {
        if (isDisabled || isClosed) {
            return
        }

        if (!ensureAvailable()) {
            return
        }

        if (excludedProxy == this.proxyEntry) {
            tryConnectToNext()
        }
    }

    private fun startWatcher() {
        var tick = 0
        while (!isClosed && tick++ < Int.MAX_VALUE && !Thread.currentThread().isInterrupted) {
            try {
                try {
                    TimeUnit.SECONDS.sleep(1)
                } catch (e: InterruptedException) {
                    log.info("IPS loop interrupted after {} rounds", tick)
                    Thread.currentThread().interrupt()
                    return
                }

                checkAndReport(tick)
            } catch (e: Throwable) {
                log.error("Unexpected IPS error: ", e)
            }
        }

        if (isClosed) {
            log.info("Quit IPS loop on close after {} rounds", tick)
        } else {
            log.error("Quit IPS loop abnormally after {} rounds", tick)
        }
    }

    private fun checkAndReport(tick: Int) {
        // Wait for 5 seconds
        if (tick % 5 != 0) {
            return
        }

        if (RuntimeUtils.hasLocalFileCommand(CMD_INTERNAL_PROXY_SERVER_DISCONNECT, Duration.ofSeconds(15))) {
            log.info("Find fcmd $CMD_INTERNAL_PROXY_SERVER_DISCONNECT, disconnect proxy")
            disconnect(true)
            return
        }

        val lastProxy = proxyEntry
        val proxyAlive = checkAndUpdateProxyStatus()

        // always false, feature disabled
        val isIdle = isIdle()
        if (isIdle) {
            if (proxyAlive && lastProxy != null) {
                proxyPool.retire(lastProxy)
                // all free proxies are very likely be expired
                log.info("IPS is idle, clear proxy pool")
                proxyPool.clear()
            }

            log.info("IPS is idle, disconnect proxy")
            disconnect(true)
        } else {
            if (!proxyAlive || !isConnected) {
                tryConnectToNext()
            }
        }

        idleCount = if (isIdle) idleCount++ else 0
        val duration = min(20 + idleCount / 5, 120)
        if (tick % duration == 0) {
            generateReport(isIdle, proxyAlive, lastProxy)
            if (showReport) {
                log.info(report)
            }
        }
    }

    private fun checkAndUpdateProxyStatus(): Boolean {
        val lastProxy = proxyEntry
        val proxyAlive = when {
            lastProxy == null -> false
            lastProxy.willExpireAfter(Duration.ofMinutes(1)) -> false
            !lastProxy.test() -> false
            else -> true
        }

        if (!proxyAlive && lastProxy != null) {
            log.info("Proxy <{}> is force retired", lastProxy.display)
            proxyPool.retire(lastProxy)
            proxyEntry = null
        }

        return proxyAlive
    }

    private fun generateReport(isIdle: Boolean, proxyAlive: Boolean, lastProxy: ProxyEntry? = null) {
        report = String.format("IPS - %s%s running tasks, %s | %s",
                if (isIdle) "[Idle] " else "",
                numRunningTasks,
                formatProxy(proxyAlive, lastProxy),
                proxyPool)
    }

    private fun formatProxy(proxyAlive: Boolean, lastProxy: ProxyEntry?): String {
        if (lastProxy != null) {
            return "proxy: ${lastProxy.display}" + if (proxyAlive) "" else "(retired)"
        }

        return "proxy: <none>"
    }

    private fun isIdle(): Boolean {
        var isIdle = false
        if (numRunningTasks.get() == 0) {
            idleTime = Duration.between(lastActiveTime, Instant.now())
            if (idleTime > idleTimeout) {
                // do not waste the proxy resource, they are expensive!
                isIdle = true
            }

            if (RuntimeUtils.hasLocalFileCommand(CMD_INTERNAL_PROXY_SERVER_FORCE_IDLE, Duration.ZERO)) {
                isIdle = true
            }
        }
        return isIdle
    }

    private fun tryConnectToNext() {
        if (isClosed || isDisabled) {
            return
        }

        disconnect(notifyAll = false)

        val proxy = proxyPool.poll()
        if (proxy != null) {
            connectTo(proxy)
        }
    }

    @Synchronized
    private fun connectTo(proxy: ProxyEntry?) {
        if (isClosed || isDisabled) {
            return
        }

        if (isConnected) {
            log.warn("IPS is already running")
            return
        }

        val nextPort = SocketUtils.findAvailableTcpPort(INTERNAL_PROXY_SERVER_PORT_BASE)
        if (log.isTraceEnabled) {
            log.trace("Ready to start IPS at {} with {}",
                    nextPort,
                    if (proxy != null) " <${proxy.display}>" else "no proxy")
        }

        val server = initForwardProxyServer(proxy)
        val thread = Thread { server.start(nextPort) }
        thread.isDaemon = true
        thread.start()

        var i = 0
        while (!isClosed && !NetUtil.testNetwork("127.0.0.1", nextPort)) {
            if (i++ > 3) {
                log.warn("Waited {}s for IPS to start ...", i)
            }
            if (i > 20) {
                disconnect(notifyAll = true)
                throw TimeoutException("Timeout to wait for IPS")
            }
            TimeUnit.SECONDS.sleep(1)
        }

        log.info("IPS is started at {} with {}",
                nextPort,
                if (proxy != null) "external proxy <${proxy.display}>" else "no proxy")

        forwardServer = server
        forwardServerThread = thread
        port = nextPort
        lastProxyEntry = proxyEntry
        proxyEntry = proxy
        ++numTotalConnects

        connected.set(true)
        connectedLock.withLock {
            connectedCond.signalAll()
        }
    }

    @Synchronized
    private fun disconnect(notifyAll: Boolean = true) {
        connected.set(false)
        // notify all to exit waiting
        if (notifyAll) {
            connectedLock.withLock {
                // TODO: who need this signal?
                disconnectedCond.signalAll()
            }
        }

        val server = forwardServer
        if (server != null) {
            log.info("Disconnecting IPS with {} ...", server.proxyConfig?.hostPort)
        }

        // TODO: only disconnect the internet proxy connections
        forwardServer?.use { it.close() }
        forwardServerThread?.interrupt()
        forwardServerThread?.join()
        forwardServer = null
        forwardServerThread = null
    }

    @Synchronized
    override fun close() {
        if (isDisabled || closed.compareAndSet(false, true)) {
            return
        }

        log.info("Closing IPS ...")

        try {
            disconnect(notifyAll = true)
            watcherThread.interrupt()
            watcherThread.join()
        } catch (e: Throwable) {
            log.error("Failed to close IPS - {}", e)
        }
    }

    private fun initForwardProxyServer(externalProxy: ProxyEntry?): HttpProxyServer {
        val server = HttpProxyServer()
        server.serverConfig(httpProxyServerConfig)

        if (externalProxy != null) {
            val proxyConfig = ProxyConfig(ProxyType.HTTP, externalProxy.host, externalProxy.port)
            server.proxyConfig(proxyConfig)
        }

        server.proxyInterceptInitializer(object : HttpProxyInterceptInitializer() {
            override fun init(pipeline: HttpProxyInterceptPipeline) {
                pipeline.addLast(object : FullRequestIntercept() {
                    override fun match(httpRequest: HttpRequest, pipeline: HttpProxyInterceptPipeline): Boolean {
                        return log.isTraceEnabled
                    }

                    override fun handelRequest(httpRequest: FullHttpRequest, pipeline: HttpProxyInterceptPipeline) {
                        val message = String.format("Ready to download %s", httpRequest.headers())
                        PROXY_LOG.write(SimpleLogger.DEBUG, "[proxy]", message)
                    }
                })

                pipeline.addLast(object : FullResponseIntercept() {
                    override fun match(httpRequest: HttpRequest, httpResponse: HttpResponse, pipeline: HttpProxyInterceptPipeline): Boolean {
                        return log.isTraceEnabled
                    }

                    override fun handelResponse(httpRequest: HttpRequest, httpResponse: FullHttpResponse, pipeline: HttpProxyInterceptPipeline) {
                        val message = String.format("Got resource %s, %s", httpResponse.status(), httpResponse.headers())
                        PROXY_LOG.write(SimpleLogger.DEBUG, "[proxy]", message)
                    }
                })
            }
        })

        server.httpProxyExceptionHandle(object: HttpProxyExceptionHandle() {
            override fun beforeCatch(clientChannel: Channel, cause: Throwable) {
                // log.warn("Internal proxy error - {}", StringUtil.stringifyException(cause))
            }

            override fun afterCatch(clientChannel: Channel, proxyChannel: Channel, cause: Throwable) {
                var message = cause.message
                when (cause) {
                    is io.netty.handler.proxy.ProxyConnectException -> {
                        // TODO: handle io.netty.handler.proxy.ProxyConnectException: http, none, /117.69.129.113:4248 => img59.ddimg.cn:80, disconnected
                        message = StringUtil.simplifyException(cause)
                    }
                }

                if (message == null) {
                    log.warn(StringUtil.stringifyException(cause))
                    return
                }

                // log.warn(StringUtil.simplifyException(cause))
                PROXY_LOG.write(SimpleLogger.WARN, javaClass, message)
            }
        })

        // ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.ADVANCED)
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.SIMPLE)

        return server
    }

    companion object {
        val PROXY_LOG = SimpleLogger(HttpProxyServer.PATH, SimpleLogger.INFO)
    }
}

fun main() {
    val conf = ImmutableConfig()
    val proxyPool = ProxyPool(conf)
    val server = InternalProxyServer(proxyPool, conf)
    server.start()
}
