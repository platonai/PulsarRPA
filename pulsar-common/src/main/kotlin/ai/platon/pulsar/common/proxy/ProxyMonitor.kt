package ai.platon.pulsar.common.proxy

import ai.platon.pulsar.common.NetUtil
import ai.platon.pulsar.common.RuntimeUtils
import ai.platon.pulsar.common.config.AppConstants.CMD_PROXY_POOL_DUMP
import ai.platon.pulsar.common.config.CapabilityTypes.PROXY_PROXY_POOL_RECOVER_PERIOD
import ai.platon.pulsar.common.config.ImmutableConfig
import org.slf4j.LoggerFactory
import java.net.URL
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * TODO: merge all monitor threads
 * */
class ProxyMonitor(
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
        while (!isClosed && !Thread.currentThread().isInterrupted) {
            if (!proxyPool.isIdle) {
                when {
                    proxyPool.numFreeProxies == 0 -> proxyPool.updateProxies(asap = true)
                    proxyPool.numFreeProxies < 3 -> proxyPool.updateProxies()
                }
            }

            if (tick % 20 == 0) {
                if (RuntimeUtils.hasLocalFileCommand(CMD_PROXY_POOL_DUMP)) {
                    proxyPool.dump()
                }
            }

            ++tick

            try {
                TimeUnit.SECONDS.sleep(1)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
    }

    private fun testTestWebSites() {
        val removal = mutableSetOf<URL>()
        ProxyEntry.TEST_URLS.forEach {
            if (!NetUtil.testHttpNetwork(it)) {
                removal.add(it)
            }
        }
        ProxyEntry.TEST_URLS.removeAll(removal)
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
        private val log = LoggerFactory.getLogger(ProxyMonitor::class.java)
    }
}

fun main() {
    val log = LoggerFactory.getLogger(ProxyMonitor::class.java)

    val conf = ImmutableConfig()
    val proxyPool = ProxyPool(conf)
    val proxyManager = ProxyMonitor(proxyPool, conf)
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
