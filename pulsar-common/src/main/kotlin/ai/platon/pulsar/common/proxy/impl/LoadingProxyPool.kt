package ai.platon.pulsar.common.proxy.impl

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.common.proxy.ProxyException
import ai.platon.pulsar.common.proxy.ProxyLoader
import ai.platon.pulsar.common.proxy.ProxyPool
import ai.platon.pulsar.common.readable
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * Manage all external proxies.
 * Check all unavailable proxies, recover them if possible.
 * This might take a long time, so it should be run in a separate thread.
 */
class LoadingProxyPool(
    private val proxyLoader: ProxyLoader,
    conf: ImmutableConfig
) : ProxyPool(conf) {

    private val logger = LoggerFactory.getLogger(LoadingProxyPool::class.java)

    /**
     * Try to take a proxy from the pool, if the pool is empty, load proxies using a [ProxyLoader].
     *
     * */
    @Throws(ProxyException::class, InterruptedException::class)
    override fun take(): ProxyEntry? {
        lastActiveTime = Instant.now()

        var i = 0
        val maxRetry = 10
        var proxy: ProxyEntry? = null
        while (isActive && proxy == null && i++ < maxRetry && !Thread.currentThread().isInterrupted) {
            if (freeProxies.isEmpty()) {
                load()
            }

            // Block until timeout, thread interrupted or an available proxy entry returns
            // TODO: no need to block, just try to get a item from the queue
            proxy = poll0()
        }

        return proxy
    }

    /**
     * The proxy may be recovered later
     */
    override fun retire(proxyEntry: ProxyEntry) {
        proxyEntry.retire()
    }

    override fun report(proxyEntry: ProxyEntry) {
        logger.info(
            "Ban proxy <{}> after {} pages served in {} | {}",
            proxyEntry.outIp, proxyEntry.numSuccessPages, proxyEntry.elapsedTime.readable(), proxyEntry
        )
    }

    @Throws(ProxyException::class)
    private fun load() {
        // synchronize proxyLoader to fix issue 41: https://github.com/platonai/PulsarRPA/issues/41
        val loadedProxies = synchronized(proxyLoader) {
            proxyLoader.updateProxies(Duration.ZERO)
        }

        loadedProxies.asSequence()
            .filterNot { it in proxyEntries }
            .forEach { offer(it) }
    }

    /**
     * Block until timeout, thread interrupted or an available proxy entry returns
     * */
    @Throws(InterruptedException::class)
    private fun poll0(): ProxyEntry? {
        // Retrieves and removes the head of the queue
        val proxy = freeProxies.poll(pollingTimeout.toMillis(), TimeUnit.MILLISECONDS) ?: return null

        return proxy
    }

    override fun toString(): String = String.format("total %d, free: %d", proxyEntries.size, freeProxies.size)
}
