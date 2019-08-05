package ai.platon.pulsar.common.proxy

import ai.platon.pulsar.common.NetUtil
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

class ProxyManager(private val proxyPool: ProxyPool, private val conf: ImmutableConfig): AutoCloseable {
    private val HTTP_PROXY_POOL_UPDATE_PERIOD = "http.proxy.pool.update.period"
    private var updatePeriod = conf.getDuration(HTTP_PROXY_POOL_UPDATE_PERIOD, Duration.ofSeconds(20))
    private var updateThread = Thread(this::update)

    private val closed = AtomicBoolean()
    val isClosed get() = closed.get()

    fun start() {
        updateThread.start()
    }

    fun startAsDaemon() {
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
        try {
            var tick = 0
            while (!isClosed) {
                if (tick % 20 == 0) {
                    log.info("Updating proxy pool, status: {}", proxyPool)
                }

                if (tick % 20 == 0) {
                    tryUpdateProxyFromMaster()
                }

                val start = Instant.now()
                // proxyPool.recover(10)
                val elapsed = Duration.between(start, Instant.now())

                // Too often, enlarge review period
                if (elapsed > updatePeriod) {
                    log.info("It costs {} to check all retired proxy servers, enlarge the check interval", elapsed)
                    updatePeriod.plus(elapsed)
                }

                proxyPool.reloadIfModified()

                TimeUnit.MILLISECONDS.sleep(updatePeriod.toMillis())

                ++tick
            }
        } catch (e: InterruptedException) {
            log.error(e.toString())
        }
    }

    private fun tryUpdateProxyFromMaster() {
        val host = conf.get(CapabilityTypes.PULSAR_MASTER_HOST, "localhost")
        val port = conf.getInt(CapabilityTypes.PULSAR_MASTER_PORT, 8081)

        val thisHost = NetUtil.getHostname()
        if (host == thisHost) {
            return
        }

        val url = "http://$host:$port/proxy/download"
        if (!NetUtil.testNetwork(host, port)) {
            return
        }

        try {
            val filename = "synced.proxies.txt"
            val target = Paths.get(proxyPool.archiveDir.toString(), filename)
            val symbolLink = Paths.get(proxyPool.enabledDir.toString(), filename)

            Files.deleteIfExists(target)
            Files.deleteIfExists(symbolLink)

            FileUtils.copyURLToFile(URL(url), target.toFile())
            Files.createSymbolicLink(symbolLink, target)
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
