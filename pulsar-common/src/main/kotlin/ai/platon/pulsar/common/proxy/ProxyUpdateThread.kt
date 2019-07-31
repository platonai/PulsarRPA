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

class ProxyUpdateThread(private val proxyPool: ProxyPool, private val conf: ImmutableConfig) : Thread(), AutoCloseable {
    private var updatePeriod = conf.getDuration("http.proxy.pool.update.period", Duration.ofSeconds(20))
    private val isClosed = AtomicBoolean()

    override fun close() {
        isClosed.set(true)
    }

    init {
        this.isDaemon = true
    }

    override fun run() {
        try {
            var tick = 0
            while (!isClosed.get()) {
                if (tick % 20 == 0) {
                    log.debug("Updating proxy pool...")
                }

                if (tick % 20 == 0) {
                    tryUpdateProxyFromMaster()
                }

                val start = Instant.now()
                proxyPool.recover(100)
                val elapsed = Duration.between(start, Instant.now())

                // too often, enlarge review period
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
        private val log = LoggerFactory.getLogger(ProxyUpdateThread::class.java)
    }
}

fun main() {
    val log = LoggerFactory.getLogger(ProxyUpdateThread::class.java)

    val conf = ImmutableConfig()

    val proxyPool = ProxyPool(conf)

    val updateThread = ProxyUpdateThread(proxyPool, conf)
    updateThread.start()

    while (true) {
        val proxy = proxyPool.poll()

        if (proxy != null) {
            if (proxy.testNetwork()) {
                log.info("Proxy is available | {} ", proxy)
            } else {
                log.info("Proxy is unavailable | {}", proxy)
            }

            proxyPool.offer(proxy)
        }

        TimeUnit.SECONDS.sleep(5)
    }
}
