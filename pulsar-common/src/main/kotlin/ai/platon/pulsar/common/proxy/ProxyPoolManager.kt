package ai.platon.pulsar.common.proxy

import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.AppPaths.AVAILABLE_PROVIDER_DIR
import ai.platon.pulsar.common.AppPaths.ENABLED_PROVIDER_DIR
import ai.platon.pulsar.common.FileCommand
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import org.apache.commons.io.FileUtils
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

open class ProxyPoolManager(
        val proxyPool: ProxyPool,
        private val conf: ImmutableConfig
): AutoCloseable {
    // Set the active proxy idle, for test purpose
    private val isForceIdle get() = FileCommand.check(AppConstants.CMD_PROXY_FORCE_IDLE, 15)

    var lastActiveTime = Instant.EPOCH
    var idleTimeout = conf.getDuration(CapabilityTypes.PROXY_IDLE_TIMEOUT, Duration.ofMinutes(10))
    val idleTime get() = Duration.between(lastActiveTime, Instant.now())
    open val isIdle get() = (numRunningTasks.get() == 0 && idleTime > idleTimeout) || isForceIdle

    val numRunningTasks = AtomicInteger()
    var statusString: String = ""
    var verbose = false

    val activeProxyEntries = ConcurrentSkipListMap<Path, ProxyEntry>()
    val workingProxyEntries = ConcurrentSkipListSet<ProxyEntry>()

    val isEnabled get() = isProxyEnabled(conf)
    val isDisabled get() = !isEnabled

    private val closed = AtomicBoolean()
    val isActive get() = !closed.get()

    /**
     * Run the task, if the proxy is disabled, call the innovation directly
     * */
    @Throws(NoProxyException::class)
    open suspend fun <R> runWith(proxyEntry: ProxyEntry?, task: suspend () -> R): R {
        return if (isDisabled) task() else runWith0(proxyEntry, task)
    }

    /**
     * Run the task with the proxy
     * */
    @Throws(NoProxyException::class)
    private suspend fun <R> runWith0(proxyEntry: ProxyEntry?, task: suspend () -> R): R {
        return try {
            lastActiveTime = Instant.now()
            proxyEntry?.also {
                it.lastActiveTime = lastActiveTime
                workingProxyEntries.add(it)
            }
            numRunningTasks.incrementAndGet()
            task()
        } finally {
            lastActiveTime = Instant.now()
            proxyEntry?.also {
                it.lastActiveTime = lastActiveTime
                workingProxyEntries.remove(it)
            }
            numRunningTasks.decrementAndGet()
        }
    }

    /**
     * Take off the proxy if it is active, and the monitor will choose the next proxy to connect
     * */
    open fun takeOff(excludedProxy: ProxyEntry, ban: Boolean) {}

    override fun toString(): String {
        return statusString
    }

    override fun close() {

    }

    companion object {
        private const val PROXY_PROVIDER_FILE_NAME = "proxy.providers.txt"
        private val DEFAULT_PROXY_PROVIDER_FILES = arrayOf(AppContext.USER_HOME, AppContext.TMP_DIR)
            .map { Paths.get(it) }
            .map { it.resolve(PROXY_PROVIDER_FILE_NAME) }

        private val PROXY_FILE_WATCH_INTERVAL = Duration.ofSeconds(30)
        private var providerDirLastWatchTime = Instant.EPOCH
        private var numEnabledProviderFiles = 0L

        /**
         * Proxy system can be enabled/disabled at runtime
         * Check if the proxy is enabled.
         *
         * The proxy system is enabled if:
         *
         * 1. PROXY_USE_PROXY is set to "yes" or "true"
         * 2. PROXY_USE_PROXY is not set
         *    1. PROXY_ENABLE_DEFAULT_PROVIDERS is "true"
         *    2. and there are proxy providers in AVAILABLE_PROVIDER_DIR
         * */
        fun isProxyEnabled(conf: ImmutableConfig): Boolean {
            val useProxy = conf[CapabilityTypes.PROXY_USE_PROXY]
            when (useProxy) {
                "yes", "true" -> return true
                "no", "false" -> return false
            }

            if (conf.getBoolean(CapabilityTypes.PROXY_ENABLE_DEFAULT_PROVIDERS, false)) {
                enableDefaultProviders()
            }

            // if no one set the proxy availability explicitly, but we have providers, use it
            return hasEnabledProvider()
        }

        @Synchronized
        @Throws(IOException::class)
        fun hasEnabledProvider(): Boolean {
            val now = Instant.now()

            if (Duration.between(providerDirLastWatchTime, now) > PROXY_FILE_WATCH_INTERVAL) {
                providerDirLastWatchTime = now
                numEnabledProviderFiles = try {
                    Files.list(AppPaths.ENABLED_PROVIDER_DIR).filter { Files.isRegularFile(it) }.count()
                } catch (e: Throwable) { 0 }
            }

            return numEnabledProviderFiles > 0
        }

        fun enableProxy(): Companion {
            System.setProperty(CapabilityTypes.PROXY_USE_PROXY, "yes")
            return this
        }

        fun disableProxy(): Companion {
            System.setProperty(CapabilityTypes.PROXY_USE_PROXY, "no")
            return this
        }

        @Synchronized
        @Throws(IOException::class)
        fun enableDefaultProviders(): Companion {
            DEFAULT_PROXY_PROVIDER_FILES.mapNotNull { it.takeIf { Files.exists(it) } }.forEach {
                FileUtils.copyFileToDirectory(it.toFile(), AVAILABLE_PROVIDER_DIR.toFile())
            }
            AVAILABLE_PROVIDER_DIR.mapNotNull { it.takeIf { Files.exists(it) } }.forEach { enableProvider(it) }

            return this
        }

        @Synchronized
        @Throws(IOException::class)
        fun enableProvider(providerPath: Path): Companion {
            val filename = providerPath.fileName
            val link = AppPaths.ENABLED_PROVIDER_DIR.resolve(filename)
            Files.deleteIfExists(link)
            Files.createSymbolicLink(link, providerPath)

            return this
        }

        @Synchronized
        @Throws(IOException::class)
        fun disableProviders(): Companion {
            Files.list(AppPaths.ENABLED_PROVIDER_DIR)
                    .filter { Files.isRegularFile(it) || Files.isSymbolicLink(it) }
                    .forEach { Files.delete(it) }
            return this
        }
    }
}
