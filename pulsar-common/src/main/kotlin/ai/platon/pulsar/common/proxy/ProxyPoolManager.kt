package ai.platon.pulsar.common.proxy

import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.AppPaths.AVAILABLE_PROVIDER_DIR
import ai.platon.pulsar.common.FileCommand
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import org.apache.commons.io.FileUtils
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

    var lastActiveTime = Instant.now()
    var idleTimeout = conf.getDuration(CapabilityTypes.PROXY_IDLE_TIMEOUT, Duration.ofMinutes(10))
    val idleTime get() = Duration.between(lastActiveTime, Instant.now())
    open val isIdle get() = (numRunningTasks.get() == 0 && idleTime > idleTimeout) || isForceIdle

    val numRunningTasks = AtomicInteger()
    var statusString: String = ""
    var verbose = false

    val activeProxyEntries = ConcurrentSkipListMap<Path, ProxyEntry>()
    val workingProxyEntries = ConcurrentSkipListSet<ProxyEntry>()
    open val localPort = -1
    open val currentInterceptProxyEntry: ProxyEntry? = null
    val isEnabled get() = isProxyEnabled(conf)
    val isDisabled get() = !isEnabled

    private val closed = AtomicBoolean()
    val isActive get() = !closed.get()

    /**
     * Run the task, it it's disabled, call the innovation directly
     * */
    @Throws(NoProxyException::class)
    open suspend fun <R> runWith(proxyEntry: ProxyEntry?, task: suspend () -> R): R {
        return if (isDisabled) task() else runWith0(proxyEntry, task)
    }

    /**
     * Run the task in the proxy monitor
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
     * Take off the proxy if it is active proxy, and the monitor will choose the next proxy to connect
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
                .map { Paths.get(it).resolve(PROXY_PROVIDER_FILE_NAME) }

        private val PROXY_FILE_WATCH_INTERVAL = Duration.ofSeconds(30)
        private var providerDirLastWatchTime = Instant.EPOCH
        private var numEnabledProviderFiles = 0L

        /**
         * Proxy system can be enabled/disabled at runtime
         * */
        fun isProxyEnabled(conf: ImmutableConfig): Boolean {
            // explicit set system environment property
            val useProxy = conf.get(CapabilityTypes.PROXY_USE_PROXY)
            if (useProxy != null) {
                when (useProxy) {
                    "yes", "true" -> return true
                    "no", "false" -> return false
                }
            }

            if (conf.getBoolean(CapabilityTypes.PROXY_ENABLE_DEFAULT_PROVIDERS, false)) {
                enableDefaultProviders()
            }

            // if no one set the proxy availability explicitly, but we have providers, use it
            return hasEnabledProvider()
        }

        fun hasEnabledProvider(): Boolean {
            val now = Instant.now()
            synchronized(ProxyPoolManager::class.java) {
                if (Duration.between(providerDirLastWatchTime, now) > PROXY_FILE_WATCH_INTERVAL) {
                    providerDirLastWatchTime = now
                    numEnabledProviderFiles = try {
                        Files.list(AppPaths.ENABLED_PROVIDER_DIR).filter { Files.isRegularFile(it) }.count()
                    } catch (e: Throwable) { 0 }
                }
            }

            return numEnabledProviderFiles > 0
        }

        fun enableDefaultProviders() {
            DEFAULT_PROXY_PROVIDER_FILES.mapNotNull { it.takeIf { Files.exists(it) } }.forEach {
                FileUtils.copyFileToDirectory(it.toFile(), AVAILABLE_PROVIDER_DIR.toFile())
            }
            AVAILABLE_PROVIDER_DIR.mapNotNull { it.takeIf { Files.exists(it) } }.forEach { enableProvider(it) }
        }

        fun enableProvider(providerPath: Path) {
            val filename = providerPath.fileName
            val target = AppPaths.ENABLED_PROVIDER_DIR.resolve(filename)
            Files.deleteIfExists(target)
            Files.createSymbolicLink(target, providerPath)
        }

        fun disableProviders() {
            Files.list(AppPaths.ENABLED_PROVIDER_DIR)
                    .filter { Files.isRegularFile(it) || Files.isSymbolicLink(it) }
                    .forEach { Files.delete(it) }
        }
    }
}
