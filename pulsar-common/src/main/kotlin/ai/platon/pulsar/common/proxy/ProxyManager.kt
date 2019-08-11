package ai.platon.pulsar.common.proxy

import ai.platon.pulsar.common.NetUtil
import ai.platon.pulsar.common.PulsarPaths
import ai.platon.pulsar.common.config.CapabilityTypes
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

const val HTTP_PROXY_POOL_UPDATE_PERIOD = "http.proxy.pool.update.period"
const val HTTP_PROXY_POOL_RECOVER_PERIOD = "http.proxy.pool.recover.period"

class ProxyManager(private val proxyPool: ProxyPool, private val conf: ImmutableConfig): AutoCloseable {
    private var updatePeriod = conf.getDuration(HTTP_PROXY_POOL_UPDATE_PERIOD, Duration.ofSeconds(20))
    private var recoverPeriod = conf.getDuration(HTTP_PROXY_POOL_RECOVER_PERIOD, Duration.ofSeconds(120))
    private var updateThread = Thread(this::update)
    private var recoverThread = Thread(this::recover)

    private val closed = AtomicBoolean()
    val isClosed get() = closed.get()

    fun start() {
        updateThread.isDaemon = true
        updateThread.start()

        recoverThread.isDaemon = true
        recoverThread.start()
    }

    override fun close() {
        if (closed.getAndSet(true)) {
            return
        }

        updateThread.interrupt()
        updateThread.join()

        recoverThread.interrupt()
        recoverThread.join()
    }

    private fun update() {
        var tick = 0
        var idle = 0
        while (!isClosed && !proxyPool.isClosed) {
            idle = if (proxyPool.isEmpty()) 0 else idle + 1

            val mod = min(30 + 2 * idle, 60 * 60)
            if (tick % mod == 0) {
                log.info("Proxy pool status - {}", proxyPool)
            }

            if (tick % 10 == 0) {
                tryUpdateProxyFromMaster()

                if (proxyPool.isEmpty()) {
                    proxyPool.providers.forEach {
                        tryUpdateProxy(URL(it))
                    }
                }
            }

            proxyPool.reloadIfModified()

            try {
                TimeUnit.SECONDS.sleep(1)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }

            ++tick
        }
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

    private fun tryUpdateProxyFromMaster() {
        val host = conf.get(CapabilityTypes.PULSAR_MASTER_HOST, "localhost")
        val port = conf.getInt(CapabilityTypes.PULSAR_MASTER_PORT, 8081)

        val thisHost = NetUtil.getHostname()
        if (host == thisHost) {
            return
        }

        val url = URL("http://$host:$port/proxy/download")
        if (!NetUtil.testHttpNetwork(url)) {
            return
        }

        tryUpdateProxy(url)
    }

    private fun tryUpdateProxy(url: URL) {
        try {
            val filename = "proxies." + PulsarPaths.fromUri(url.toString()) + ".txt"
            val target = Paths.get(proxyPool.availableDir.toString(), filename)
            val symbolLink = Paths.get(proxyPool.enabledDir.toString(), filename)

            Files.deleteIfExists(target)
            Files.deleteIfExists(symbolLink)

            FileUtils.copyURLToFile(url, target.toFile())
            Files.createSymbolicLink(symbolLink, target)

            val proxies = Files.readAllLines(target)
            log.info("Saved {} proxies to {} from {}", proxies.size, target, url)
        } catch (e: IOException) {
            log.error(e.toString())
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(ProxyManager::class.java)
        const val PROXY_SERVER_LOCAL_PORT = 8382
    }
}

fun main() {
    val log = LoggerFactory.getLogger(ProxyManager::class.java)

    val conf = ImmutableConfig()
    val proxyPool = ProxyPool(conf)
    val proxyServer = ProxyManager(proxyPool, conf)
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
