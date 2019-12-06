package ai.platon.pulsar.proxy

import ai.platon.pulsar.PulsarEnv
import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.PulsarConstants.*
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
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
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
    private val env = PulsarEnv.initialize()

    private var runningWithoutProxy = false
    private var forwardServer: HttpProxyServer? = null
    private var forwardServerThread: Thread? = null
    private val loopThread = Thread(this::startLoop)
    private val loopStarted = AtomicBoolean()
    private val closed = AtomicBoolean()
    private val connected = AtomicBoolean()
    private val lock: Lock = ReentrantLock()
    private val connectedCond: Condition = lock.newCondition()
    private var waitForReadyTimeout = Duration.ofMinutes(2)
    private var idleTimeout = conf.getDuration(PROXY_INTERNAL_SERVER_IDLE_TIMEOUT, Duration.ofMinutes(5))

    private val bossGroupThreads = conf.getInt(PROXY_INTERNAL_SERVER_BOSS_THREADS, 5)
    private val workerGroupThreads = conf.getInt(PROXY_INTERNAL_SERVER_WORKER_THREADS, 20)
    private val httpProxyServerConfig = HttpProxyServerConfig()

    val isEnabled get() = env.useProxy && conf.getBoolean(PROXY_ENABLE_INTERNAL_SERVER, true)
    val isDisabled get() = !isEnabled
    val port = INTERNAL_PROXY_SERVER_PORT
    val totalConnects = AtomicInteger()
    val numRunningTasks = AtomicInteger()
    var lastActiveTime = Instant.now()
    private var idleTime = Duration.ZERO
    private var idleCount = 0
    private var reconnectPending = AtomicBoolean()
    var report: String = ""
    val isLoopStarted get() = loopStarted.get()
    val isConnected get() = connected.get()
    val isClosed get() = closed.get()

    var proxyEntry: ProxyEntry? = null
        private set

    init {
        httpProxyServerConfig.bossGroupThreads = bossGroupThreads
        httpProxyServerConfig.workerGroupThreads = workerGroupThreads
        httpProxyServerConfig.isHandleSsl = false
    }

    fun start() {
        if (isDisabled) {
            log.warn("IPS is disabled")
            return
        }

        if (loopStarted.compareAndSet(false, true)) {
            loopThread.isDaemon = true
            loopThread.start()
        }
    }

    inline fun <R> run(block: () -> R): R {
        if (isClosed || isDisabled) {
            throw NoProxyException("IPS is " + if (isClosed) "closed" else "disabled")
        }

        if (!waitUntilRunning()) {
            throw NoProxyException("Failed to wait for IPS to run")
        }

        numRunningTasks.incrementAndGet()
        return try {
            block()
        } catch (e: Exception) {
            throw e
        } finally {
            lastActiveTime = Instant.now()
            numRunningTasks.decrementAndGet()
        }
    }

    fun waitUntilRunning(): Boolean {
        if (isDisabled || isClosed) {
            return false
        }

        if (!reconnectPending.get() && isConnected) {
            return true
        }

        log.info("Waiting for IPS to connect ...")

        lastActiveTime = Instant.now()
        idleTime = Duration.ZERO

        lock.withLock {
            try {
                val success = connectedCond.await(waitForReadyTimeout.seconds, TimeUnit.SECONDS)
                if (!success) {
                    log.warn("Timeout to wait for IPS to be ready")
                }
            } catch (e: InterruptedException) {
                log.warn("Interrupted from waiting for IPS")
                Thread.currentThread().interrupt()
            }
        }

        return !isClosed && isConnected
    }

    fun proxyExpired(): Boolean {
        reconnectPending.set(true)
        return waitUntilRunning()
    }

    private fun startLoop() {
        var tick = 0
        while (!isClosed && tick++ < Int.MAX_VALUE && !Thread.currentThread().isInterrupted) {
            try {
                waitAndCheck(tick)
            } catch (e: Throwable) {
                log.error("Unexpected error - ", e)
            }
        }

        if (isClosed) {
            log.info("Quit IPS loop on close after {} rounds", tick)
        } else {
            log.error("Quit IPS loop abnormally after {} rounds", tick)
        }
    }

    private fun waitAndCheck(tick: Int) {
        try {
            TimeUnit.SECONDS.sleep(1)
        } catch (e: InterruptedException) {
            log.info("IPS loop interrupted after {} rounds", tick)
            Thread.currentThread().interrupt()
            return
        }

        // Wait for 5 seconds
        if (tick % 5 != 0) {
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
            proxyEntry = null
            if (!runningWithoutProxy) {
                log.info("IPS is idle, run it without proxy")
                reconnect(useProxy = false)
            }
        } else {
            if (!proxyAlive || !isConnected) {
                reconnect(useProxy = true)
            }
        }

        idleCount = if (isIdle) idleCount++ else 0
        val duration = min(20 + idleCount / 5, 120)
        if (tick % duration == 0) {
            generateReport(isIdle, proxyAlive, lastProxy)
        }
    }

    private fun checkAndUpdateProxyStatus(): Boolean {
        val lastProxy = proxyEntry
        val proxyAlive = when {
            lastProxy == null -> false
            reconnectPending.get() -> false
            lastProxy.willExpireAfter(Duration.ofMinutes(1)) -> false
            !lastProxy.test() -> false
            else -> true
        }

        if (!proxyAlive && lastProxy != null) {
            if (reconnectPending.get()) {
                log.info("Proxy <{}> is unavailable, mark it retired", lastProxy.display)
            } else {
                log.info("Proxy <{}> is force retired", lastProxy.display)
            }
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
                // do not waste the proxy resource, they are costly!
                isIdle = true
            }

            if (RuntimeUtils.hasLocalFileCommand(CMD_INTERNAL_PROXY_SERVER_FORCE_IDLE, Duration.ZERO)) {
                isIdle = true
            }
        }
        return isIdle
    }

    @Synchronized
    private fun reconnect(useProxy: Boolean) {
        disconnect()

        if (useProxy) {
            connect()
        } else {
            connectTo(null)
        }

        reconnectPending.set(false)
    }

    private fun connect() {
        val proxy = proxyPool.poll()
        if (proxy != null || !runningWithoutProxy) {
            connectTo(proxy)
            this.proxyEntry = proxy
        }
    }

    private fun connectTo(proxy: ProxyEntry?) {
        if (isClosed) {
            return
        }

        if (isConnected) {
            log.warn("IPS is already running")
            return
        }

        if (log.isTraceEnabled) {
            log.trace("Ready to start IPS at {} with {}",
                    INTERNAL_PROXY_SERVER_PORT,
                    if (proxy != null) " <${proxy.display}>" else "no proxy")
        }

        val server = initForwardProxyServer(proxy)
        val thread = Thread { server.start(INTERNAL_PROXY_SERVER_PORT) }
        thread.isDaemon = true
        thread.start()

        var i = 0
        while (!NetUtil.testNetwork("127.0.0.1", port)) {
            if (i++ > 3) {
                log.info("Waited {}s for IPS to start ...", i)
            }
            TimeUnit.SECONDS.sleep(1)
        }

        forwardServer = server
        forwardServerThread = thread
        runningWithoutProxy = proxy == null
        totalConnects.incrementAndGet()

        connected.set(true)
        lock.withLock {
            connectedCond.signalAll()
        }

        log.info("IPS is started at {} with {}",
                INTERNAL_PROXY_SERVER_PORT,
                if (proxy != null) "external proxy <${proxy.display}>" else "no proxy")
    }

    private fun disconnect(notifyAll: Boolean = false) {
        connected.set(false)
        // notify all to exit waiting
        if (notifyAll) {
            lock.withLock {
                connectedCond.signalAll()
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
        if (isDisabled || closed.getAndSet(true)) {
            return
        }

        log.info("Closing IPS ...")

        try {
            loopThread.interrupt()
            loopThread.join()
            disconnect(notifyAll = true)
        } catch (e: Throwable) {
            log.error("Failed to close IPS - {}", e)
        }
    }

    private fun initForwardProxyServer(proxy: ProxyEntry?): HttpProxyServer {
        val server = HttpProxyServer()
        server.serverConfig(httpProxyServerConfig)

        if (proxy != null) {
            val proxyConfig = ProxyConfig(ProxyType.HTTP, proxy.host, proxy.port)
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
        val PROXY_LOG = HttpProxyServer.LOG
    }
}

fun main() {
    val conf = ImmutableConfig()
    val proxyPool = ProxyPool(conf)
    val server = InternalProxyServer(proxyPool, conf)
    server.start()
}
