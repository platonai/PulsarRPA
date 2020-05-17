package ai.platon.pulsar.crawl

import ai.platon.pulsar.PulsarContext
import ai.platon.pulsar.PulsarSession
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.prependReadableClassName
import ai.platon.pulsar.common.proxy.ProxyVendorUntrustedException
import ai.platon.pulsar.persist.WebPage
import com.codahale.metrics.Gauge
import com.codahale.metrics.SharedMetricRegistries
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import oshi.SystemInfo
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs

open class StreamingCrawler(
        private val urls: Sequence<String>,
        private val options: LoadOptions = LoadOptions.create(),
        val pageCollector: MutableList<WebPage>? = null,
        session: PulsarSession = PulsarContext.createSession(),
        autoClose: Boolean = true,
        val conf: ImmutableConfig = session.sessionConfig
): Crawler(session, autoClose) {
    companion object {
        private val metricRegistry = SharedMetricRegistries.getOrCreate("pulsar")
        private val numRunningTasks = AtomicInteger()

        init {
            metricRegistry.register(prependReadableClassName(this,"runningTasks"), object: Gauge<Int> {
                override fun getValue(): Int = numRunningTasks.get()
            })
        }
    }

    private var concurrency = conf.getInt(CapabilityTypes.FETCH_CONCURRENCY, AppConstants.FETCH_THREADS)
    private val privacyManager = session.context.getBean(PrivacyManager::class)
    private val isAppActive get() = isAlive
    private val systemInfo = SystemInfo()
    // OSHI cached the value, so it's fast and safe to be called frequently
    private val availableMemory get() = systemInfo.hardware.memory.available
    private val requiredMemory = 500 * 1024 * 1024L // 500 MiB
    private val numTasks = AtomicInteger()

    open suspend fun run() {
        supervisorScope {
            urls.forEachIndexed { j, url ->
                numTasks.incrementAndGet()
                // log.info("$j.\t$url")

                var k = 0
                while (isAppActive && privacyManager.activeContext.isLeaked) {
                    if (k++ % 10 == 0) {
                        log.info("Privacy is leaked, wait for privacy context reset")
                    }
                    Thread.sleep(1000)
                }

                // update fetch concurrency on command
                if (numRunningTasks.get() == concurrency) {
                    val path = AppPaths.TMP_CONF_DIR.resolve("fetch-concurrency-override")
                    if (Files.exists(path)) {
                        val concurrencyOverride = Files.readAllLines(path).firstOrNull()?.toIntOrNull()?:concurrency
                        if (concurrencyOverride != concurrency) {
                            session.sessionConfig.setInt(CapabilityTypes.FETCH_CONCURRENCY, concurrencyOverride)
                        }
                    }
                }

                while (isAppActive && numRunningTasks.get() >= concurrency) {
                    Thread.sleep(200)
                }

                val memoryRemaining = availableMemory - requiredMemory
                while (isAppActive && memoryRemaining < 0) {
                    if (j % 10 == 0) {
                        log.info("$j.\tnumRunning: {}, availableMemory: {}, requiredMemory: {}, shortage: {}",
                                numRunningTasks,
                                Strings.readableBytes(availableMemory),
                                Strings.readableBytes(requiredMemory),
                                Strings.readableBytes(abs(memoryRemaining))
                        )
                    }
                    Thread.sleep(1000)
                }

                if (!isAppActive) {
                    return@supervisorScope
                }

                var page: WebPage? = null
                var exception: Throwable? = null
                numRunningTasks.incrementAndGet()
                val context = Dispatchers.Default + CoroutineName("w")
                launch(context) {
                    page = session.runCatching { loadDeferred(url, options) }
                            .onFailure { exception = it; log.warn("Load failed - $it") }
                            .getOrNull()
                            ?.also { pageCollector?.add(it) }
                    numRunningTasks.decrementAndGet()
                }

                if (exception is ProxyVendorUntrustedException) {
                    log.error(exception?.message?:"Unexpected error")
                    return@supervisorScope
                }
            }
        }

        log.info("All done. Total $numTasks tasks")
        if (pageCollector != null) {
            log.info("Collected ${pageCollector.size} pages")
        }
    }
}
