package ai.platon.pulsar.crawl

import ai.platon.pulsar.PulsarContext
import ai.platon.pulsar.PulsarSession
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.prependReadableClassName
import ai.platon.pulsar.common.proxy.ProxyVendorUntrustedException
import ai.platon.pulsar.persist.WebPage
import com.codahale.metrics.Gauge
import com.codahale.metrics.SharedMetricRegistries
import kotlinx.coroutines.*
import org.h2tools.dev.util.ConcurrentLinkedList
import oshi.SystemInfo
import java.nio.file.Files
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs

open class StreamingCrawler(
        private val urls: Sequence<String>,
        private val options: LoadOptions = LoadOptions.create(),
        val pageCollector: ConcurrentLinkedList<WebPage>? = null,
        session: PulsarSession = PulsarContext.createSession(),
        autoClose: Boolean = true
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

    private val conf = session.sessionConfig
    private var concurrency = conf.getInt(CapabilityTypes.FETCH_CONCURRENCY, AppConstants.FETCH_THREADS)
    private val privacyManager = session.context.getBean(PrivacyManager::class)
    private var lastActiveTime = Instant.now()
    private val idleTime get() = Duration.between(lastActiveTime, Instant.now())
    private val isIdle get() = idleTime > Duration.ofMinutes(5)
    private val isAppActive get() = isAlive && !isIdle
    private val systemInfo = SystemInfo()
    // OSHI cached the value, so it's fast and safe to be called frequently
    private val availableMemory get() = systemInfo.hardware.memory.available
    private val requiredMemory = 500 * 1024 * 1024L // 500 MiB
    private val memoryRemaining get() = availableMemory - requiredMemory
    private val numTasks = AtomicInteger()
    private val taskTimeout = Duration.ofMinutes(5)

    var onLoadComplete: (WebPage) -> Unit = {}

    open suspend fun run() {
        supervisorScope {
            urls.forEachIndexed { j, url ->
                lastActiveTime = Instant.now()
                numTasks.incrementAndGet()

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
                    Thread.sleep(1000)
                }

                while (isAppActive && memoryRemaining < 0) {
                    if (j % 20 == 0) {
                        handleMemoryShortage(j)
                    }
                    Thread.sleep(1000)
                }

                if (!isAppActive) {
                    return@supervisorScope
                }

                var page: WebPage?
                var exception: Throwable? = null
                numRunningTasks.incrementAndGet()
                val context = Dispatchers.Default + CoroutineName("w")
                launch(context) {
                    withTimeout(taskTimeout.toMillis()) {
                        page = session.runCatching { loadDeferred(url, options) }
                                .onFailure { exception = it.also { log.warn("Load failed - ", it.message) } }
                                .getOrNull()
                                ?.also { pageCollector?.add(it) }
                        page?.let(onLoadComplete)
                        numRunningTasks.decrementAndGet()
                        lastActiveTime = Instant.now()
                    }
                }

                when (exception) {
                    null -> {}
                    is ProxyVendorUntrustedException -> {
                        log.error(exception?.message?:"Unexpected error").let { return@supervisorScope }
                    }
                    is TimeoutCancellationException -> {}
                    else -> log.error("Unexpected exception", exception)
                }
            }
        }

        log.info("Total $numTasks tasks are loaded")
    }

    private fun handleMemoryShortage(j: Int) {
        log.info("$j.\tnumRunning: {}, availableMemory: {}, requiredMemory: {}, shortage: {}",
                numRunningTasks,
                Strings.readableBytes(availableMemory),
                Strings.readableBytes(requiredMemory),
                Strings.readableBytes(abs(memoryRemaining))
        )
        session.context.clearCache()
        System.gc()
    }
}
