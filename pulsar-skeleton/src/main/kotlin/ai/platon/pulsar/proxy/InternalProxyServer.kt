package ai.platon.pulsar.proxy

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.NetUtil.PROXY_CONNECTION_TIMEOUT
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.PulsarConstants.*
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
import org.slf4j.LoggerFactory
import java.net.HttpURLConnection
import java.net.Proxy
import java.net.URL
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * TODO: never close the listening TCP port, just choose the external proxy
 * */
class InternalProxyServer(
        private val proxyPool: ProxyPool,
        private val conf: ImmutableConfig
): AutoCloseable {
    private val log = LoggerFactory.getLogger(InternalProxyServer::class.java)
    private val now = DateTimeUtil.now("yyyyMMdd")
    private val path = PulsarPaths.get("proxy", "logs", "proxy-$now.log")
    private val proxyLog = SimpleLogger(path, SimpleLogger.INFO)

    private var runningWithoutProxy = false
    private var forwardServer: HttpProxyServer? = null
    private var forwardServerThread: Thread? = null
    private val loopThread = Thread(this::startLoop)
    private val closed = AtomicBoolean()
    private val connected = AtomicBoolean()
    private val lock: Lock = ReentrantLock()
    private val connectedCond: Condition = lock.newCondition()
    private var readyTimeout = Duration.ofSeconds(45)
    private var idleTime = Duration.ZERO

    private val bossGroupThreads = conf.getInt(PROXY_INTERNAL_SERVER_BOSS_THREADS, 5)
    private val workerGroupThreads = conf.getInt(PROXY_INTERNAL_SERVER_WORKER_THREADS, 20)
    private val httpProxyServerConfig = HttpProxyServerConfig()

    val enabled = conf.getBoolean(PROXY_ENABLE_INTERNAL_SERVER, true)
    val disabled get() = !enabled
    var proxyEntry: ProxyEntry? = null
        private set
    private var heartBeat: HttpURLConnection? = null
    val port = INTERNAL_PROXY_SERVER_PORT
    val totalConnects = AtomicInteger()
    val numRunningThreads = AtomicInteger()
    val isConnected get() = connected.get()
    val isClosed get() = closed.get()

    var lastActiveTime = Instant.now()

    init {
        httpProxyServerConfig.bossGroupThreads = bossGroupThreads
        httpProxyServerConfig.workerGroupThreads = workerGroupThreads
        httpProxyServerConfig.isHandleSsl = false
    }

    fun start() {
        if (disabled) {
            log.warn("Internal proxy server is disabled")
            return
        }

        loopThread.isDaemon = true
        loopThread.start()
    }

    fun waitUntilRunning(): Boolean {
        if (disabled || isClosed) {
            return false
        }

        if (isConnected) {
            return true
        }

        log.info("Waiting for a proxy server to connect ...")

        lastActiveTime = Instant.now()
        idleTime = Duration.ZERO

        lock.withLock {
            val b = connectedCond.await(readyTimeout.seconds, TimeUnit.SECONDS)
            if (!b) {
                // timeout
            }
        }

        return !isClosed && isConnected
    }

    private fun startLoop() {
        var tick = 0
        while (!isClosed) {
            try {
                TimeUnit.SECONDS.sleep(1)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }

            // Wait for 5 seconds
            if (tick++ % 5 != 0) {
                continue
            }

            // TODO: use heart beat
            val proxyStillAlive = proxyEntry?.testNetwork()?:false
            if (!proxyStillAlive && proxyEntry != null) {
                retireCurrentProxy()
            }

            var shouldReconnect = false
            if (tick % 10 == 0) {
                if (RuntimeUtils.hasLocalFileCommand(CMD_INTERNAL_PROXY_SERVER_RECONNECT)) {
                    log.info("Execute local file command {}", CMD_INTERNAL_PROXY_SERVER_RECONNECT)
                    shouldReconnect = true
                }
            }

            val isIdle = isIdle()
            if (isIdle) {
                log.info("Run internal proxy server in idle mode without external proxy")
                disconnect()
                connectTo(null)
            } else {
                if (!proxyStillAlive || !isConnected) {
                    shouldReconnect = true
                }

                if (shouldReconnect) {
                    reconnect()
                }
            }
        }

        log.info("Quit internal proxy server")
    }

    private fun retireCurrentProxy() {
        val proxy = proxyEntry
        if (proxy != null) {
            log.info("Proxy <{}> is unavailable, mark it retired", proxy)
            proxyPool.retire(proxy)
            proxyEntry = null
        }
    }

    private fun reconnect() {
        log.info("Internal proxy server is connecting to a new external proxy ...")

        disconnect()
        connect()
    }

    @Synchronized
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
            log.warn("Internal proxy server is already running")
            return
        }

        val server = initForwardProxyServer(proxy)
        val thread = Thread { server.start(INTERNAL_PROXY_SERVER_PORT) }
        thread.isDaemon = true
        thread.start()

        forwardServer = server
        forwardServerThread = thread
        runningWithoutProxy = proxy == null
        totalConnects.incrementAndGet()

        connected.set(true)
        lock.withLock {
            connectedCond.signalAll()
        }

        log.info("Internal proxy server is started at {} with {}",
                INTERNAL_PROXY_SERVER_PORT,
                if (proxy != null) "external proxy <$proxy>" else "no proxy")
    }

    @Synchronized
    private fun disconnect(notifyAll: Boolean = false) {
        connected.set(false)
        // notify all to exit waiting
        if (notifyAll) {
            lock.withLock {
                connectedCond.signalAll()
            }
        }

        heartBeat?.disconnect()
        forwardServer?.use { it.close() }
        forwardServerThread?.interrupt()
        forwardServerThread?.join()
        forwardServer = null
        forwardServerThread = null
    }

    @Synchronized
    override fun close() {
        if (disabled || closed.getAndSet(true)) {
            return
        }

        log.info("Closing internal proxy server ...")

        disconnect(notifyAll = true)
        loopThread.join()

        proxyLog.use { it.close() }
    }

    private fun isIdle(): Boolean {
        var isIdle = false
        if (numRunningThreads.get() == 0) {
            idleTime = Duration.between(lastActiveTime, Instant.now())
            if (idleTime.toMinutes() > 20) {
                // do not waste the proxy resource, they are costly!
                isIdle = true
            }
        }

        if (RuntimeUtils.hasLocalFileCommand(CMD_INTERNAL_PROXY_SERVER_FORCE_IDLE)) {
            log.info("Execute local file command {}", CMD_INTERNAL_PROXY_SERVER_FORCE_IDLE)
            isIdle = true
        }

        return isIdle
    }

    fun establishHeartBeat(url: URL, proxy: Proxy) {
        try {
            heartBeat?.disconnect()

            val conn = url.openConnection(proxy) as HttpURLConnection
            conn.connectTimeout = PROXY_CONNECTION_TIMEOUT.toMillis().toInt()
            conn.connect()

            heartBeat = conn
        } catch (e: Exception) {
            log.warn("Failed to establish a heart beat to {} through proxy {}", url, proxy)
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
                        // return HttpUtil.checkHeader(httpRequest.headers(), HttpHeaderNames.CONTENT_TYPE, "^(?i)application/json.*$")
                        return log.isDebugEnabled
                    }

                    override fun handelRequest(httpRequest: FullHttpRequest, pipeline: HttpProxyInterceptPipeline) {
                        // log.debug("Ready to download {}", httpRequest.headers())
                    }
                })

                pipeline.addLast(object : FullResponseIntercept() {
                    override fun match(httpRequest: HttpRequest, httpResponse: HttpResponse, pipeline: HttpProxyInterceptPipeline): Boolean {
                        // return HttpUtil.checkUrl(pipeline.httpRequest, "^www.baidu.com$") && HttpUtil.isHtml(httpRequest, httpResponse)
                        return log.isDebugEnabled
                    }

                    override fun handelResponse(httpRequest: HttpRequest, httpResponse: FullHttpResponse, pipeline: HttpProxyInterceptPipeline) {
                        // log.debug("Got resource {}, {}", httpResponse.status(), httpResponse.headers())
                    }
                })
            }
        })

        server.httpProxyExceptionHandle(object: HttpProxyExceptionHandle() {
            override fun beforeCatch(clientChannel: Channel, cause: Throwable) {
                // log.warn("Internal proxy error - {}", StringUtil.stringifyException(cause))
            }

            override fun afterCatch(clientChannel: Channel, proxyChannel: Channel, cause: Throwable) {
                proxyLog.write(SimpleLogger.INFO, "proxy", StringUtil.stringifyException(cause))
                val message = cause.message?:return
                log.warn(message.split("\n").first())
            }
        })

        return server
    }
}

fun main() {
    val conf = ImmutableConfig()
    val proxyPool = ProxyPool(conf)
    val server = InternalProxyServer(proxyPool, conf)
    server.start()
}
