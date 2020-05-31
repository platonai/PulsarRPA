package ai.platon.pulsar.protocol.browser.driver


import ai.platon.pulsar.common.MetricsManagement
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.CapabilityTypes.BROWSER_DRIVER_HEADLESS
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.common.prependReadableClassName
import com.codahale.metrics.Gauge
import org.slf4j.LoggerFactory
import oshi.SystemInfo
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Created by vincent on 18-1-1.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
class LoadingWebDriverPool(
        private val driverFactory: WebDriverFactory,
        immutableConfig: ImmutableConfig
): Parameterized, AutoCloseable {

    private val log = LoggerFactory.getLogger(LoadingWebDriverPool::class.java)
    val concurrency = immutableConfig.getInt(CapabilityTypes.FETCH_CONCURRENCY, AppConstants.FETCH_THREADS)
    val capacity = immutableConfig.getInt(CapabilityTypes.BROWSER_POOL_CAPACITY, concurrency)
    val onlineDrivers = ConcurrentSkipListSet<ManagedWebDriver>()
    val freeDrivers = ArrayBlockingQueue<ManagedWebDriver>(500)

    private val lock = ReentrantLock()
    private val notBusy = lock.newCondition()

    private val isHeadless = immutableConfig.getBoolean(BROWSER_DRIVER_HEADLESS, true)
    private val closed = AtomicBoolean()
    private var closer: Thread? = null
    private val systemInfo = SystemInfo()
    // OSHI cached the value, so it's fast and safe to be called frequently
    private val availableMemory get() = systemInfo.hardware.memory.available
    private val instanceRequiredMemory = 200 * 1024 * 1024 // 200 MiB

    private val metricRegistry = MetricsManagement.defaultMetricRegistry
    val counterRetired = metricRegistry.counter(prependReadableClassName(this, "retired"))
    val counterQuit = metricRegistry.counter(prependReadableClassName(this, "quit"))

    val isActive get() = !closed.get()
    val numWaiting = AtomicInteger()
    val numWorking = AtomicInteger()
    val numFree get() = freeDrivers.size
    val numActive get() = numWorking.get() + numFree
    val numOnline get() = onlineDrivers.size

    init {
        metricRegistry.register(prependReadableClassName(this,"waitingDrivers"), object: Gauge<Int> {
            override fun getValue(): Int = numWaiting.get()
        })

        metricRegistry.register(prependReadableClassName(this,"workingDrivers"), object: Gauge<Int> {
            override fun getValue(): Int = numWorking.get()
        })
    }

    @Throws(InterruptedException::class)
    fun take(conf: ImmutableConfig): ManagedWebDriver = take(0, conf)

    @Throws(InterruptedException::class)
    fun take(priority: Int, conf: ImmutableConfig): ManagedWebDriver {
        return take0(priority, conf).also { numWorking.incrementAndGet() }
    }

    fun put(driver: ManagedWebDriver) {
        if (numWorking.decrementAndGet() == 0) {
            lock.withLock { notBusy.signalAll() }
        }

        if (driver.isRetired) retire(driver) else offer(driver)
    }

    fun forEach(action: (ManagedWebDriver) -> Unit) = onlineDrivers.forEach(action)

    fun firstOrNull(predicate: (ManagedWebDriver) -> Boolean) = onlineDrivers.firstOrNull(predicate)

    fun closeAll(incognito: Boolean = true, processExit: Boolean = false, timeToWait: Duration = Duration.ofSeconds(90)) {
        if (!processExit) {
            waitUntilIdleOrTimeout(timeToWait)
        }

        closeAllDrivers(processExit)
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            if (closer == null) {
                closer = Thread {
                    try {
                        closeAll(incognito = true, processExit = true)
                    } catch (e: InterruptedException) {
                        log.warn("Thread interrupted when closing | {}", this)
                    }
                }
                closer?.start()
            }
        }
    }

    override fun toString(): String = "$numFree/$numWorking/$numActive/$numOnline (free/working/active/online)"

    private fun offer(driver: ManagedWebDriver) = freeDrivers.offer(driver.apply { free() })

    private fun retire(driver: ManagedWebDriver, external: Boolean = true) {
        if (external && !isActive) {
            return
        }

        counterRetired.inc()
        freeDrivers.remove(driver.apply { retire() })
        driver.runCatching { quit().also { counterQuit.inc() } }
                .onFailure { log.warn("Unexpected exception quit $driver", it) }
    }

    @Throws(InterruptedException::class)
    private fun take0(priority: Int, conf: ImmutableConfig): ManagedWebDriver {
        val concurrencyOverride = conf.getInt(CapabilityTypes.FETCH_CONCURRENCY, this.concurrency)
        val capacityOverride = conf.getInt(CapabilityTypes.BROWSER_POOL_CAPACITY, concurrencyOverride)

        driverFactory.takeIf { isActive && onlineDrivers.size < capacityOverride && availableMemory > instanceRequiredMemory }
                ?.create(priority, conf)
                ?.also {
                    freeDrivers.add(it)
                    onlineDrivers.add(it)
                    logDriverOnline(it)
                }

        numWaiting.incrementAndGet()
        return freeDrivers.take().also { numWaiting.decrementAndGet() }
    }

    private fun closeAllDrivers(processExit: Boolean = false) {
        if (!isHeadless) {
            // return
        }

        freeDrivers.clear()

        // create a non-synchronized list for quitting all drivers in parallel
        val nonSynchronized = onlineDrivers.toList().also { onlineDrivers.clear() }
        nonSynchronized.parallelStream().forEach {
            it.quit().also { counterQuit.inc() }
            log.info("Quit driver {}", it)
        }
    }

    @Synchronized
    private fun waitUntilIdleOrTimeout(timeout: Duration) {
        lock.withLock {
            val ttl = timeout.seconds
            var i = 0
            try {
                while (isActive && numWorking.get() > 0 && ++i < ttl) {
                    notBusy.await(1, TimeUnit.SECONDS)
                    if (i % 20 == 0) {
                        log.info("Round $i waiting for idle | $this")
                    }
                }
            } catch (ignored: InterruptedException) {}
        }
    }

    private fun logDriverOnline(driver: ManagedWebDriver) {
        if (log.isTraceEnabled) {
            val driverControl = driverFactory.driverControl
            log.trace("The {}th web driver is online, browser: {} imagesEnabled: {} pageLoadStrategy: {} capacity: {}",
                    numOnline,
                    driver.name,
                    driverControl.imagesEnabled,
                    driverControl.pageLoadStrategy, capacity)
        }
    }
}
