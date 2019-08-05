package ai.platon.pulsar.proxy

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.PulsarConstants.INTERNAL_PROXY_SERVER_PORT
import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.common.proxy.ProxyPool
import com.github.monkeywie.proxyee.exception.HttpProxyExceptionHandle
import com.github.monkeywie.proxyee.intercept.HttpProxyInterceptInitializer
import com.github.monkeywie.proxyee.intercept.HttpProxyInterceptPipeline
import com.github.monkeywie.proxyee.intercept.common.CertDownIntercept
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
        val proxyPool: ProxyPool,
        val conf: ImmutableConfig
): AutoCloseable {
    private val log = LoggerFactory.getLogger(InternalProxyServer::class.java)

    private var runningNoProxy = false
    private var forwardServer: HttpProxyServer? = null
    private var forwardServerThread: Thread? = null
    private val loopThread = Thread(this::startLoop)
    private val httpProxyServerConfig = HttpProxyServerConfig()
    private val closed = AtomicBoolean()

    var proxyEntry: ProxyEntry? = null
        private set
    var readyTimeout = Duration.ofSeconds(20)
        private set
    val lock: Lock = ReentrantLock()
    val connectionCond: Condition = lock.newCondition()
    val disconnectCond: Condition = lock.newCondition()
    val isConnected = AtomicBoolean()
    val numConnect = AtomicInteger()
    val isClosed get() = closed.get()

    init {
        httpProxyServerConfig.bossGroupThreads = 5
        httpProxyServerConfig.workerGroupThreads = 15
        httpProxyServerConfig.isHandleSsl = true
    }

    fun start() {
        loopThread.start()
    }

    fun startAsDaemon() {
        loopThread.isDaemon = true
        loopThread.start()
    }

    fun waitUntilReady(): Boolean {
        if (isClosed) {
            return false
        }

        if (isConnected.get()) {
            return true
        }

        lock.withLock {
            log.info("Waiting for proxy server to connect ...")
            val b = connectionCond.await(readyTimeout.seconds, TimeUnit.SECONDS)
            isConnected.set(b)
        }

        return isConnected.get()
    }

    private fun startLoop() {
        log.info("Starting internal proxy server ...")

        while (!isClosed) {
            val canConnect = proxyEntry?.testNetwork()?:false

            if (!canConnect) {
                synchronized(closed) {
                    if (!isClosed) {
                        // close old no working running forward proxy server and quit the thread
                        disconnect()
                        connect(proxyEntry)
                    }
                }
            }

            try {
                TimeUnit.SECONDS.sleep(10)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }

        log.info("Quit internal proxy server")
    }

    override fun close() {
        synchronized(closed) {
            if (closed.getAndSet(true)) {
                return
            }

            disconnect()
            loopThread.join()
        }
    }

    private fun connect(oldProxy: ProxyEntry?) {
        if (oldProxy != null) {
            log.info("Proxy {} is unavailable, mark it retired", oldProxy)
            proxyPool.retire(oldProxy)
        }

        // Poll a new available proxy and every pulsar request is forwarded to that proxy
        log.trace("Try to pool a proxy ...")
        val proxy = proxyPool.poll()
        this.proxyEntry = proxy

        val server = createForwardServer()

        if (proxy != null) {
            val proxyConfig = ProxyConfig(ProxyType.HTTP, proxy.host, proxy.port)
            server.proxyConfig(proxyConfig)
            runningNoProxy = false
            log.info("External proxy is {}", proxyConfig)
        } else {
            runningNoProxy = true
        }

        if (!runningNoProxy) {
            forwardServer = server
            forwardServerThread = Thread { server.start(INTERNAL_PROXY_SERVER_PORT) }
            forwardServerThread?.isDaemon = true
            forwardServerThread?.start()
            numConnect.incrementAndGet()

            log.info("Internal proxy server is started at {} with {}",
                    INTERNAL_PROXY_SERVER_PORT,
                    if (proxy != null) "external proxy $proxy" else "no proxy")
        }

        isConnected.set(true)
        lock.withLock {
            connectionCond.signalAll()
        }
    }

    private fun disconnect() {
        isConnected.set(false)
        lock.withLock {
            disconnectCond.signalAll()
        }

        if (!isClosed) {
            forwardServer?.use { it.close() }
            forwardServerThread?.join()
            forwardServer = null
            forwardServerThread = null
        }
    }

    private fun createForwardServer(): HttpProxyServer {
        return HttpProxyServer()
                .serverConfig(httpProxyServerConfig)
                .proxyInterceptInitializer(object : HttpProxyInterceptInitializer() {
                    override fun init(pipeline: HttpProxyInterceptPipeline) {
                        pipeline.addLast(CertDownIntercept())
                        pipeline.addLast(object : FullRequestIntercept() {
                            override fun match(httpRequest: HttpRequest, pipeline: HttpProxyInterceptPipeline): Boolean {
                                // return HttpUtil.checkHeader(httpRequest.headers(), HttpHeaderNames.CONTENT_TYPE, "^(?i)application/json.*$")
                                return false
                            }

                            override fun handelRequest(httpRequest: FullHttpRequest, pipeline: HttpProxyInterceptPipeline) {
                                //
                            }
                        })
                        pipeline.addLast(object : FullResponseIntercept() {
                            override fun match(httpRequest: HttpRequest, httpResponse: HttpResponse, pipeline: HttpProxyInterceptPipeline): Boolean {
                                // return HttpUtil.checkUrl(pipeline.httpRequest, "^www.baidu.com$") && HttpUtil.isHtml(httpRequest, httpResponse)
                                return true
                            }

                            override fun handelResponse(httpRequest: HttpRequest, httpResponse: FullHttpResponse, pipeline: HttpProxyInterceptPipeline) {
                                log.debug("Got resource {}, {}", httpResponse.status(), httpResponse.headers())
                            }
                        })
                    }
                })
                .httpProxyExceptionHandle(object: HttpProxyExceptionHandle() {
                    @Throws(Exception::class)
                    override fun beforeCatch(clientChannel: Channel, cause: Throwable) {
                        cause.printStackTrace()
                    }

                    @Throws(Exception::class)
                    override fun afterCatch(clientChannel: Channel, proxyChannel: Channel, cause: Throwable) {
                        cause.printStackTrace()
                    }
                })
    }
}

fun main() {
    val conf = ImmutableConfig()
    val proxyPool = ProxyPool(conf)
    val server = InternalProxyServer(proxyPool, conf)
    server.start()
}
