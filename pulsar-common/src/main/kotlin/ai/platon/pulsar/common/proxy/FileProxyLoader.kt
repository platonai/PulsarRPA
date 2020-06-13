package ai.platon.pulsar.common.proxy

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.config.ImmutableConfig
import java.io.IOException
import java.nio.file.Files
import java.time.Duration
import kotlin.streams.toList

/**
 * Load proxies from proxy vendors
 */
open class FileProxyLoader(conf: ImmutableConfig): ProxyLoader(conf) {

    @Synchronized
    override fun updateProxies(reloadInterval: Duration): List<ProxyEntry> {
        return kotlin.runCatching { updateProxies0(reloadInterval) }
                .onFailure { log.warn("Failed to update proxies, {}", it.message) }
                .getOrElse { listOf() }
    }

    @Throws(IOException::class)
    private fun updateProxies0(reloadInterval: Duration): List<ProxyEntry> {
        return Files.list(AppPaths.ENABLED_PROXY_DIR).filter { Files.isRegularFile(it) }.map { Files.readAllLines(it) }
                .toList().flatMap { it.mapNotNull { ProxyEntry.parse(it) } }
    }
}
