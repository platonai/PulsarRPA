package ai.platon.pulsar.proxy

import ai.platon.pulsar.common.DateTimeUtil
import ai.platon.pulsar.common.PulsarPaths
import ai.platon.pulsar.common.SimpleLogger
import ai.platon.pulsar.common.StringUtil
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.PulsarConstants.INTERNAL_PROXY_SERVER_PORT
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
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class InternalProxyServer(
        private val proxyPool: ProxyPool,
        private val conf: ImmutableConfig
): AutoCloseable {
    private val log = LoggerFactory.getLogger(InternalProxyServer::class.java)

    private var runningNoProxy = false
    private var forwardServer: HttpProxyServer? = null
    private var forwardServerThread: Thread? = null
    private val loopThread = Thread(this::startLoop)
    private val httpProxyServerConfig = HttpProxyServerConfig()
    private val closed = AtomicBoolean()
    private val lock: Lock = ReentrantLock()
    private val connectionCond: Condition = lock.newCondition()
    private val disconnectCond: Condition = lock.newCondition()
    private var readyTimeout = Duration.ofSeconds(20)

    private val proxyLog: SimpleLogger

    val bossGroupThreads = conf.getInt(PROXY_INTERNAL_SERVER_BOSS_THREADS, 5)
    val workerGroupThreads = conf.getInt(PROXY_INTERNAL_SERVER_WORKER_THREADS, 25)

    val enabled = conf.getBoolean(PROXY_ENABLE_INTERNAL_SERVER, true)
    val disabled get() = !enabled
    var proxyEntry: ProxyEntry? = null
        private set
    val ipPort = "127.0.0.1:$INTERNAL_PROXY_SERVER_PORT"
    val numConnect = AtomicInteger()
    val isConnected = AtomicBoolean()
    val isClosed get() = closed.get()

    init {
        httpProxyServerConfig.bossGroupThreads = bossGroupThreads
        httpProxyServerConfig.workerGroupThreads = workerGroupThreads
        httpProxyServerConfig.isHandleSsl = false

        val now = DateTimeUtil.now("yyyyMMdd")
        val path = PulsarPaths.get("proxy", "logs", "proxy-$now.log")
        proxyLog = SimpleLogger(path, SimpleLogger.INFO)
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
        if (!enabled) {
            return false
        }

        if (isClosed) {
            return false
        }

        if (isConnected.get()) {
            return true
        }

        log.info("Waiting for proxy server to connect ...")

        lock.withLock {
            val b = connectionCond.await(readyTimeout.seconds, TimeUnit.SECONDS)
            isConnected.set(b)
        }

        return !isClosed && isConnected.get()
    }

    private fun startLoop() {
        log.info("Starting internal proxy server ...")

        var tick = 0
        while (!isClosed) {
            if (tick++ % 20 != 0) {
                continue
            }

            val canConnect = proxyEntry?.testNetwork()?:false
            if (!canConnect) {
                val proxy = poll(proxyEntry)
                if (proxy != null || !runningNoProxy) {
                    synchronized(closed) {
                        if (!isClosed) {
                            // close old no working running forward proxy server and quit the thread
                            disconnect()
                            connect(proxy)
                        }
                    }
                }
                // no proxy and already running with no proxy. nothing to do, just wait for the next round
            }

            try {
                TimeUnit.SECONDS.sleep(1)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }

        log.info("Quit internal proxy server")
    }

    override fun close() {
        if (disabled) {
            return
        }

        if (closed.getAndSet(true)) {
            return
        }

        log.info("Closing internal proxy server ...")

        synchronized(closed) {
            isConnected.set(false)
            // notify all to exit waiting
            lock.withLock {
                connectionCond.signalAll()
            }

            disconnect()
            loopThread.join()
        }

        proxyLog.use { it.close() }
    }

    private fun poll(oldProxy: ProxyEntry?): ProxyEntry? {
        if (oldProxy != null) {
            log.info("Proxy <{}> is unavailable, mark it retired", oldProxy)
            proxyPool.retire(oldProxy)
        }

        // Poll a new available proxy and every pulsar request is forwarded to that proxy
        log.trace("Try to pool a proxy ...")
        val proxy = proxyPool.poll()
        this.proxyEntry = proxy

        return proxy
    }

    private fun connect(proxy: ProxyEntry?) {
        if (isConnected.get()) {
            log.warn("Internal proxy server is already running")
            return
        }

        val server = initForwardProxyServer(proxy)
        val thread = Thread { server.start(INTERNAL_PROXY_SERVER_PORT) }
        thread.isDaemon = true
        thread.start()

        forwardServer = server
        forwardServerThread = thread
        runningNoProxy = proxy == null
        numConnect.incrementAndGet()
        isConnected.set(true)
        lock.withLock {
            connectionCond.signalAll()
        }

        log.info("Internal proxy server is started at {} with {}",
                INTERNAL_PROXY_SERVER_PORT,
                if (proxy != null) "external proxy <$proxy>" else "no proxy")
    }

    private fun disconnect() {
        if (forwardServer == null || forwardServerThread == null) {
            return
        }

        isConnected.set(false)
        lock.withLock {
            disconnectCond.signalAll()
        }

        forwardServer?.use { it.close() }
        forwardServerThread?.interrupt()
        forwardServerThread?.join()
        forwardServer = null
        forwardServerThread = null
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
                        log.debug("Ready to download {}", httpRequest.headers())
                    }
                })

                pipeline.addLast(object : FullResponseIntercept() {
                    override fun match(httpRequest: HttpRequest, httpResponse: HttpResponse, pipeline: HttpProxyInterceptPipeline): Boolean {
                        // return HttpUtil.checkUrl(pipeline.httpRequest, "^www.baidu.com$") && HttpUtil.isHtml(httpRequest, httpResponse)
                        return log.isDebugEnabled
                    }

                    override fun handelResponse(httpRequest: HttpRequest, httpResponse: FullHttpResponse, pipeline: HttpProxyInterceptPipeline) {
                        log.debug("Got resource {}, {}", httpResponse.status(), httpResponse.headers())
                    }
                })
            }
        })

        server.httpProxyExceptionHandle(object: HttpProxyExceptionHandle() {
            override fun beforeCatch(clientChannel: Channel, cause: Throwable) {
                // log.warn("Internal proxy error - {}", StringUtil.stringifyException(cause))
            }

            override fun afterCatch(clientChannel: Channel, proxyChannel: Channel, cause: Throwable) {
                val message = cause.message
                if (message == null) {
                    log.warn(StringUtil.stringifyException(cause))
                    return
                }

                if (message.endsWith("disconnected") || message.endsWith("timeout")) {
                    proxyLog.write(SimpleLogger.INFO, "proxy", message)
                } else {
                    log.warn(message)
                }
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
