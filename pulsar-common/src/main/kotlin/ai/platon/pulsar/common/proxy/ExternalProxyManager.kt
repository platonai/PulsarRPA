package ai.platon.pulsar.common.proxy

import ai.platon.pulsar.common.NetUtil
import ai.platon.pulsar.common.PulsarPaths
import ai.platon.pulsar.common.config.ImmutableConfig
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

const val HTTP_PROXY_POOL_RECOVER_PERIOD = "http.proxy.pool.recover.period"

class ExternalProxyManager(private val proxyPool: ProxyPool, private val conf: ImmutableConfig): AutoCloseable {
    private var recoverPeriod = conf.getDuration(HTTP_PROXY_POOL_RECOVER_PERIOD, Duration.ofSeconds(120))
    private var updateThread = Thread(this::update)
    // no recover thread, the proxy provider should do the job
    private var recoverThread = Thread(this::recover)

    private val closed = AtomicBoolean()
    val isClosed get() = closed.get()

    fun start() {
        updateThread.isDaemon = true
        updateThread.start()
    }

    override fun close() {
        if (closed.getAndSet(true)) {
            return
        }

        updateThread.join()
    }

    private fun update() {
        var tick = 0
        var idle = 0
        while (!isClosed) {
            // report proxy pool status periodically
            idle = if (proxyPool.numWorkingProxies == 0) idle + 1 else 0
            val mod = min(30 + 2 * idle, 60)
            if (tick % mod == 0) {
                log.info("Proxy pool status - {}", proxyPool)
            }

            try {
                TimeUnit.SECONDS.sleep(1)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }

            ++tick
        }
    }

    private fun testTestWebSites() {
        val removal = mutableSetOf<URL>()
        ProxyEntry.TEST_WEB_SITES.forEach {
            if (!NetUtil.testHttpNetwork(it)) {
                removal.add(it)
            }
        }
        ProxyEntry.TEST_WEB_SITES.removeAll(removal)
        log.warn("Unreachable test sites: {}", removal.joinToString(", ") { it.toString() })
    }

    private fun recover() {
        var tick = 0

        while (!isClosed && !proxyPool.isClosed) {
            if (tick++ % recoverPeriod.seconds.toInt() != 0) {
                continue
            }

            val start = Instant.now()
            val recovered = proxyPool.recover(5)
            val elapsed = Duration.between(start, Instant.now())

            if (recovered > 0) {
                log.info("Recovered {} from proxy pool", recovered)
            }

            // Too often, enlarge review period
            if (elapsed > recoverPeriod) {
                recoverPeriod = recoverPeriod.plus(elapsed)
                log.info("It takes {} to check retired proxy, increase interval to {}", elapsed, recoverPeriod)
            }

            try {
                TimeUnit.SECONDS.sleep(1)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(ExternalProxyManager::class.java)
    }
}

fun main() {
    val log = LoggerFactory.getLogger(ExternalProxyManager::class.java)

    val conf = ImmutableConfig()
    val proxyPool = ProxyPool(conf)
    val proxyServer = ExternalProxyManager(proxyPool, conf)
    proxyServer.start()

    while (true) {
        val proxy = proxyPool.poll()

        if (proxy != null) {
            if (proxy.testNetwork()) {
                log.info("Proxy is available | {} ", proxy)

                System.setProperty("java.net.useSystemProxies", "true")
                System.setProperty("http.proxyHost", proxy.host)
                System.setProperty("http.proxyPort", proxy.port.toString())

            } else {
                log.info("Proxy is unavailable | {}", proxy)
            }

            // Do something

            proxyPool.offer(proxy)
        }

        TimeUnit.SECONDS.sleep(5)
    }
}
