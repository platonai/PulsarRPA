package ai.platon.pulsar.common.proxy

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.FileCommand
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

open class ProxyManager(
        private val conf: ImmutableConfig
): AutoCloseable {

    val numRunningTasks = AtomicInteger()
    var lastActiveTime = Instant.now()
    var idleTime = Duration.ZERO
    val closed = AtomicBoolean()

    var report: String = ""
    var verbose = false

    open val localPort = -1
    open val currentProxyEntry: ProxyEntry? = null
    open val isEnabled = false
    val isDisabled get() = !isEnabled
    val isClosed get() = closed.get()

    open fun start() {
        if (isDisabled) {
            log.warn("Proxy manager is disabled")
            return
        }
    }

    /**
     * Run the task despite the proxy manager is disabled, it it's disabled, call the innovation directly
     * */
    open fun <R> runAnyway(task: () -> R): R {
        return if (isDisabled) {
            task()
        } else {
            run(task)
        }
    }

    /**
     * Run the task in the proxy manager
     * */
    open fun <R> run(task: () -> R): R {
        if (isClosed || isDisabled) {
            throw ProxyException("Proxy manager is " + if (isClosed) "closed" else "disabled")
        }

        idleTime = Duration.ZERO

        if (!waitUntilOnline()) {
            throw ProxyException("Failed to wait for an online proxy")
        }

        return try {
            numRunningTasks.incrementAndGet()
            task()
        } catch (e: Exception) {
            throw e
        } finally {
            lastActiveTime = Instant.now()
            numRunningTasks.decrementAndGet()
        }
    }

    open fun waitUntilOnline(): Boolean {
        return false
    }

    open fun changeProxyIfOnline(excludedProxy: ProxyEntry, ban: Boolean) {
    }

    override fun close() {
    }

    companion object {
        private val log = LoggerFactory.getLogger(ProxyManager::class.java)

        private val INIT_PROXY_PROVIDER_FILES = arrayOf(AppConstants.TMP_DIR, AppConstants.HOME_DIR)
                .map { Paths.get(it, "proxy.providers.txt") }

        private val PROXY_FILE_WATCH_INTERVAL = Duration.ofSeconds(30)
        private var providerDirLastWatchTime = Instant.EPOCH
        private var numEnabledProviderFiles = 0L

        init {
            INIT_PROXY_PROVIDER_FILES.mapNotNull { it.takeIf { Files.exists(it) } }.forEach {
                FileUtils.copyFileToDirectory(it.toFile(), AppPaths.AVAILABLE_PROVIDER_DIR.toFile())
            }
        }

        fun hasEnabledProvider(): Boolean {
            val now = Instant.now()
            synchronized(ProxyManager::class.java) {
                if (Duration.between(providerDirLastWatchTime, now) > PROXY_FILE_WATCH_INTERVAL) {
                    providerDirLastWatchTime = now
                    numEnabledProviderFiles = try {
                        Files.list(AppPaths.ENABLED_PROVIDER_DIR).filter { Files.isRegularFile(it) }.count()
                    } catch (e: Throwable) { 0 }
                }
            }

            return numEnabledProviderFiles > 0
        }

        /**
         * Proxy system can be enabled/disabled at runtime
         * */
        fun isProxyEnabled(): Boolean {
            if (FileCommand.check(AppConstants.CMD_ENABLE_PROXY)) {
                return true
            }

            // explicit set system environment property
            val useProxy = System.getProperty(CapabilityTypes.PROXY_USE_PROXY)
            if (useProxy != null) {
                when (useProxy) {
                    "yes" -> return true
                    "no" -> return false
                }
            }

            // if no one set the proxy availability explicitly, but we have providers, use it
            if (hasEnabledProvider()) {
                return true
            }

            return false
        }
    }
}
