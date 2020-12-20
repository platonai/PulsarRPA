package ai.platon.pulsar.common.proxy

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.Systems
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.readable
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Files
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * Manage all external proxies
 * Check all unavailable proxies, recover them if possible.
 * This might take a long time, so it should be run in a separate thread
 */
class LoadingProxyPool(
        val proxyLoader: ProxyLoader,
        conf: ImmutableConfig
): ProxyPool(conf) {

    private val log = LoggerFactory.getLogger(LoadingProxyPool::class.java)

    private val bannedIps get() = proxyLoader.bannedIps
    private val bannedSegments get() = proxyLoader.bannedSegments

    override fun take(): ProxyEntry? {
        lastActiveTime = Instant.now()

        var i = 0
        val maxRetry = 5
        var proxy: ProxyEntry? = null
        while (isActive && proxy == null && i++ < maxRetry && !Thread.currentThread().isInterrupted) {
            if (freeProxies.isEmpty()) {
                load()
            }

            proxy = poll0()
        }

        return proxy
    }

    /**
     * The proxy may be recovered later
     */
    override fun retire(proxyEntry: ProxyEntry) {
        proxyEntry.retire()

        if (proxyEntry.isBanned) {
            ban(proxyEntry)
        }
    }

    override fun report(proxyEntry: ProxyEntry) {
        log.info("Ban proxy <{}> after {} pages served in {} | total ban: {}, banned ips: {} | {}",
                proxyEntry.outIp, proxyEntry.numSuccessPages, proxyEntry.elapsedTime.readable(),
                numProxyBanned, bannedIps.size, proxyEntry)
        val s = bannedSegments.chunked(20).joinToString("\n") { it.joinToString() }
        log.info("Banned segments ({}): {}", bannedSegments.size, s)
    }

    private fun ban(proxyEntry: ProxyEntry) {
        // local proxy server
        if (proxyEntry.host == "127.0.0.1") {
            return
        }

        var banned = false

        if (proxyEntry.outIp !in bannedIps) {
            bannedIps.add(proxyEntry.outIp)
            banned = true
        }

        if (proxyEntry.outSegment !in bannedSegments) {
            bannedSegments.add(proxyEntry.outSegment)
            banned = true
        }

        if (banned) {
            ++numProxyBanned
            report(proxyEntry)
            // ban speed, proxy change speed
        }
    }

    private fun load() {
        proxyLoader.updateProxies(Duration.ZERO).asSequence()
                .filterNot { it in proxyEntries }
                .filterNot { it.outIp in bannedIps }
                .filterNot { it.outSegment in bannedSegments }
                .forEach { offer(it) }
    }

    // Block until timeout or an available proxy entry returns
    private fun poll0(): ProxyEntry? {
        // Retrieves and removes the head of the queue
        val proxy = try {
            freeProxies.poll(pollingTimeout.toMillis(), TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            null
        }?:return null

        val banState = handleBanState(proxy).takeIf { it.isBanned }?.also {
            numProxyBanned++
            log.info("Proxy is banned <{}> | bp: {}, bh: {}, bs: {} | {}",
                    it, numProxyBanned, bannedIps.size, bannedSegments.size, proxy.display)
        }

        return proxy.takeIf { banState == null }
    }

    private fun handleBanState(proxyEntry: ProxyEntry): ProxyEntry.BanState {
        val banStrategy = proxyLoader.banStrategy?:""
        return when {
            banStrategy == "" -> ProxyEntry.BanState.OK
            banStrategy == "none" -> ProxyEntry.BanState.OK
            banStrategy == "clear" -> ProxyEntry.BanState.OK.also { bannedSegments.clear(); bannedIps.clear(); }
            banStrategy.startsWith("segment") && proxyEntry.outSegment in bannedSegments -> ProxyEntry.BanState.SEGMENT
            banStrategy.startsWith("host") && proxyEntry.outIp in bannedIps -> ProxyEntry.BanState.HOST
            proxyEntry.isBanned -> ProxyEntry.BanState.OTHER
            else -> ProxyEntry.BanState.OK
        }
    }

    override fun dump() {
        synchronized(AppPaths.PROXY_ARCHIVE_DIR) {
            try {
                Files.writeString(AppPaths.PROXY_BANNED_HOSTS_FILE, bannedIps.joinToString("\n"))
                Files.writeString(AppPaths.PROXY_BANNED_SEGMENTS_FILE, bannedSegments.joinToString("\n"))
            } catch (e: IOException) {
                log.warn(e.toString())
            }
        }

        super.dump()
    }

    override fun toString(): String = String.format("total %d, free: %d, banH: %d banS: %d",
            proxyEntries.size, freeProxies.size, bannedIps.size, bannedSegments.size)
}
