package ai.platon.pulsar.proxy

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.config.AppConstants.*
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.common.proxy.ProxyException
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

class ProxyManager(
        private val proxyPool: ProxyPool,
        private val metricsSystem: MetricsSystem,
        private val conf: ImmutableConfig
): AutoCloseable {

    enum class Availability {
        OK, TEST_IP, NO_PROXY, WILL_EXPIRE, GONE;

        val isAvailable get() = this == OK || this == TEST_IP
        val isNotAvailable get() = !isAvailable
    }

    private val log = LoggerFactory.getLogger(ProxyManager::class.java)

    private val numBossGroupThreads = conf.getInt(PROXY_INTERNAL_SERVER_BOSS_THREADS, 1)
    private val numWorkerGroupThreads = conf.getInt(PROXY_INTERNAL_SERVER_WORKER_THREADS, 2)
    private val httpProxyServerConfig = HttpProxyServerConfig()
    private var forwardServer: HttpProxyServer? = null
    private var forwardServerThread: Thread? = null
    private val threadJoinTimeout = Duration.ofSeconds(30)

    private val watcherThread = Thread(this::startWatcher)
    private val watcherStarted = AtomicBoolean()

    private val closed = AtomicBoolean()
    private val online = AtomicBoolean()
    private val connectionLock: Lock = ReentrantLock()
    private val connected: Condition = connectionLock.newCondition()
    private val disconnected: Condition = connectionLock.newCondition()
    private val pollingInterval = Duration.ofMillis(100)
    private val conditionTimeout = Duration.ofSeconds(30)

    private var idleTimeout = conf.getDuration(PROXY_INTERNAL_SERVER_IDLE_TIMEOUT, Duration.ofMinutes(5))
    private var idleCount = 0

    private var numTotalConnects = 0
    private val numRunningTasks = AtomicInteger()
    private var lastActiveTime = Instant.now()
    private var idleTime = Duration.ZERO
    private val isOnline get() = online.get()
    private val isClosed get() = closed.get()

    val isEnabled get() = ProxyPool.isProxyEnabled() && conf.getBoolean(PROXY_ENABLE_FORWARD_SERVER, true)
    val isDisabled get() = !isEnabled
    var port = -1
        private set
    var report: String = ""
    var verbose = false
    var autoRefresh = true
    val isWatcherStarted get() = watcherStarted.get()

    var lastProxyEntry: ProxyEntry? = null
        private set
    var currentProxyEntry: ProxyEntry? = null
        private set

    init {
        httpProxyServerConfig.bossGroupThreads = numBossGroupThreads
        httpProxyServerConfig.workerGroupThreads = numWorkerGroupThreads
        httpProxyServerConfig.isHandleSsl = false
    }

    @Synchronized
    fun start() {
        if (isDisabled) {
            log.warn("Proxy manager is disabled")
            return
        }

        if (watcherStarted.compareAndSet(false, true)) {
            watcherThread.isDaemon = true
            watcherThread.name = "pm"
            watcherThread.start()
        }
    }

    /**
     * Run the task despite the proxy manager is disabled, it it's disabled, call the innovation directly
     * */
    fun <R> runAnyway(task: () -> R): R {
        return if (isDisabled) {
            task()
        } else {
            run(task)
        }
    }

    /**
     * Run the task in the proxy manager
     * */
    fun <R> run(task: () -> R): R {
        if (isClosed || isDisabled) {
            throw ProxyException("Proxy manager is " + if (isClosed) "closed" else "disabled")
        }

        idleTime = Duration.ZERO

        if (!ensureOnline()) {
            throw ProxyException("Failed to wait for proxy manager, proxy is unavailable")
        }

        return try {
            numRunningTasks.incrementAndGet()
            task()
        } catch (e: Exception) {
            throw e
        } finally {
            lastActiveTime = Instant.now()
            numRunningTasks.decrementAndGet()
        }
    }

    fun ensureOnline(): Boolean {
        if (isDisabled || isClosed) {
            return false
        }

        connectionLock.withLock {
            if (isOnline) {
                return true
            }

            log.info("No online proxy, waiting ...")

            try {
                var signaled = false
                var round = 0
                val maxRound = conditionTimeout.toMillis() / pollingInterval.toMillis()
                while (!isClosed && !isOnline && !signaled && round++ < maxRound) {
                    signaled = connected.await(pollingInterval.toMillis(), TimeUnit.MILLISECONDS)
                }
            } catch (e: InterruptedException) {
                log.warn("Interrupted to wait for a proxy")
                Thread.currentThread().interrupt()
            }
        }

        return !isClosed && isOnline
    }

    fun changeProxyIfOnline(excludedProxy: ProxyEntry) {
        if (isDisabled || isClosed) {
            return
        }

        if (!ensureOnline()) {
            return
        }

        if (excludedProxy == this.currentProxyEntry) {
            tryConnectToNext()
        }
    }

    private fun startWatcher() {
        var tick = 0
        while (!isClosed && tick++ < Int.MAX_VALUE && !Thread.currentThread().isInterrupted) {
            try {
                watch(tick)

                try {
                    TimeUnit.SECONDS.sleep(1)
                } catch (e: InterruptedException) {
                    log.info("Proxy watcher loop is interrupted after {} rounds", tick)
                    Thread.currentThread().interrupt()
                }
            } catch (e: Throwable) {
                log.error("Unexpected proxy manager error", e)
            }
        }

        if (isClosed) {
            log.info("Quit proxy manager loop on close after {} rounds", tick)
        } else {
            log.error("Quit proxy manager loop abnormally after {} rounds", tick)
        }
    }

    private fun watch(tick: Int) {
        // Wait for 5 seconds
        if (tick % 10 != 0) {
            return
        }

        if (RuntimeUtils.hasLocalFileCommand(CMD_INTERNAL_PROXY_SERVER_DISCONNECT, Duration.ofSeconds(15))) {
            log.info("Find fcmd $CMD_INTERNAL_PROXY_SERVER_DISCONNECT, disconnect proxy")
            disconnect()
            return
        }

        val lastProxy = currentProxyEntry
        val availability = checkAvailability()

        // always false, feature disabled
        val isIdle = isIdle()
        if (isIdle) {
            if (availability.isAvailable && lastProxy != null) {
                proxyPool.retire(lastProxy)
                // all free proxies are very likely be expired
                log.info("Proxy manager is idle, clear proxy pool")
                proxyPool.clear()
            }

            log.info("Proxy manager is idle, disconnect online proxy")
            disconnect()
        } else {
            if (availability.isNotAvailable || !isOnline) {
                tryConnectToNext()
            }
        }

        idleCount = if (isIdle) idleCount++ else 0
        val duration = min(20 + idleCount / 5, 120)
        if (tick % duration == 0) {
            generateReport(isIdle, availability, lastProxy)
            if (verbose) {
                log.info(report)
            }
        }
    }

    private fun startTester() {
        while (!isClosed && !Thread.currentThread().isInterrupted) {
            val proxy = currentProxyEntry
            if (proxy != null) {
                proxy.test()
            }
        }
    }

    private fun checkAvailability(): Availability {
        if (!autoRefresh) {
            return Availability.OK
        }

        val lastProxy = currentProxyEntry
        val availability = when {
            lastProxy == null -> Availability.NO_PROXY
            lastProxy.isTestIp -> Availability.TEST_IP
            lastProxy.willExpireAfter(Duration.ofMinutes(1)) -> Availability.WILL_EXPIRE
            lastProxy.isGone -> Availability.GONE
            !lastProxy.test() -> Availability.GONE
            else -> Availability.OK
        }

        if (lastProxy != null && availability.isNotAvailable) {
            log.info("Proxy is retired ({}) | <{}>", availability, lastProxy)
            proxyPool.retire(lastProxy)
            currentProxyEntry = null
        }

        return availability
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

        disconnect()

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

        // wait until disconnected
        connectionLock.withLock {
            while (!isClosed && isOnline) {
                disconnected.await(pollingInterval.toMillis(), TimeUnit.MILLISECONDS)
            }
        }

        val nextPort = SocketUtils.findAvailableTcpPort(INTERNAL_PROXY_SERVER_PORT_BASE)
        if (log.isTraceEnabled) {
            val proxyDescription = if (proxy != null) " <${proxy.display}>" else "no proxy"
            log.trace("Ready to start forward proxy server at {} with {}", nextPort, proxyDescription)
        }

        try {
            connectionLock.withLock {
                if (isClosed) {
                    return
                }

                val server = initForwardProxyServer(proxy)
                val thread = Thread { server.start(nextPort) }
                thread.isDaemon = true
                thread.start()

                waitUntilOnline(nextPort)

                forwardServer = server
                forwardServerThread = thread
                port = nextPort
                lastProxyEntry = currentProxyEntry
                currentProxyEntry = proxy
                ++numTotalConnects

                online.set(true)
                connected.signalAll()
            }

            if (log.isInfoEnabled) {
                val proxyDescription = if (proxy != null) "external proxy <${proxy.display}>" else "no proxy"
                log.info("Forward proxy server is started at {} with {}", nextPort, proxyDescription)
            }
        } catch (e: Exception) {
            log.error("Failed to start forward proxy server", e)
        }
    }

    @Synchronized
    private fun disconnect() {
        connectionLock.withLock {
            online.set(false)

            val server = forwardServer
            val port = server?.proxyConfig?.port
            if (server != null) {
                log.info("Disconnecting proxy with {} ...", server.proxyConfig?.hostPort)
            }
            forwardServer?.use { it.close() }
            forwardServerThread?.interrupt()
            forwardServerThread?.join(threadJoinTimeout.toMillis())
            forwardServer = null
            forwardServerThread = null

            port?.let { waitUntilOffline(it) }
            lastProxyEntry?.let { metricsSystem.reportRetiredProxies(it.toString()) }

            disconnected.signalAll()
        }
    }

    @Synchronized
    override fun close() {
        if (isDisabled) {
            return
        }

        if (closed.compareAndSet(false, true)) {
            log.info("Closing proxy manager ... | {}", report)

            try {
                disconnect()
                watcherThread.interrupt()
                watcherThread.join(threadJoinTimeout.toMillis())
            } catch (e: Throwable) {
                log.error("Unexpected exception, failed to close proxy manager", e)
            }
        }
    }

    private fun waitUntilOnline(port: Int) {
        var i = 0
        while (!isClosed && !NetUtil.testNetwork("127.0.0.1", port) && !Thread.currentThread().isInterrupted) {
            if (i++ > 5) {
                log.warn("Waited {}s for proxy to be online ...", i)
            }
            if (i > 20) {
                disconnect()
                throw TimeoutException("Timeout to wait for proxy to connect")
            }

            try {
                TimeUnit.SECONDS.sleep(1)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
    }

    private fun waitUntilOffline(port: Int) {
        var i = 0
        while (!isClosed && NetUtil.testNetwork("127.0.0.1", port) && !Thread.currentThread().isInterrupted) {
            if (i++ > 5) {
                log.warn("Waited {}s for proxy to be offline ...", i)
            }

            try {
                TimeUnit.MILLISECONDS.sleep(100)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
    }

    private fun generateReport(isIdle: Boolean, available: Availability, lastProxy: ProxyEntry? = null) {
        report = String.format("%s%s running tasks, %s | %s",
                if (isIdle) "[Idle] " else "",
                numRunningTasks,
                formatProxy(available, lastProxy),
                proxyPool)
    }

    private fun formatProxy(available: Availability, lastProxy: ProxyEntry?): String {
        if (lastProxy != null) {
            return "proxy: ${lastProxy.display}" + if (available.isAvailable) "" else "(retired)"
        }

        return "proxy: <none>"
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
