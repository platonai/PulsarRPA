package ai.platon.pulsar.common.proxy

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.config.ImmutableConfig
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Files
import java.time.Duration

/**
 * Load proxies from proxy vendors
 */
open class FileProxyLoader(conf: ImmutableConfig): ProxyLoader(conf) {
    private val log = LoggerFactory.getLogger(FileProxyLoader::class.java)

    @Synchronized
    @Throws(IOException::class, NoProxyException::class)
    override fun updateProxies(reloadInterval: Duration): List<ProxyEntry> {
        return kotlin.runCatching { updateProxies0(reloadInterval) }
                .onFailure { log.warn("Failed to update proxies, {}", it.message) }
                .getOrElse { listOf() }
    }

    @Throws(IOException::class, NoProxyException::class)
    private fun updateProxies0(reloadInterval: Duration): List<ProxyEntry> {
        return Files.list(AppPaths.ENABLED_PROXY_DIR).filter { Files.isRegularFile(it) }
                .iterator().asSequence()
                .map { Files.readAllLines(it) }
                .flatMap { it.mapNotNull { ProxyEntry.parse(it) } }
                .toList()
    }
}
