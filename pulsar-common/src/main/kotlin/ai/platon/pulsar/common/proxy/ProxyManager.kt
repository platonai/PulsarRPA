package ai.platon.pulsar.common.proxy

import ai.platon.pulsar.common.NetUtil
import ai.platon.pulsar.common.RuntimeUtils
import ai.platon.pulsar.common.config.CapabilityTypes.PROXY_PROXY_POOL_RECOVER_PERIOD
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.PulsarConstants.CMD_PROXY_POOL_DUMP
import org.slf4j.LoggerFactory
import java.net.URL
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

class ProxyManager(
        val proxyPool: ProxyPool,
        private val conf: ImmutableConfig
): AutoCloseable {
    private var recoverPeriod = conf.getDuration(PROXY_PROXY_POOL_RECOVER_PERIOD, Duration.ofSeconds(120))
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

            if (tick % 20 == 0) {
                if (RuntimeUtils.hasLocalFileCommand(CMD_PROXY_POOL_DUMP)) {
                    proxyPool.dump()
                }
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
        private val log = LoggerFactory.getLogger(ProxyManager::class.java)
    }
}

fun main() {
    val log = LoggerFactory.getLogger(ProxyManager::class.java)

    val conf = ImmutableConfig()
    val proxyPool = ProxyPool(conf)
    val proxyManager = ProxyManager(proxyPool, conf)
    proxyManager.start()

    while (true) {
        val proxy = proxyPool.poll()

        if (proxy != null) {
            if (proxy.test()) {
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
