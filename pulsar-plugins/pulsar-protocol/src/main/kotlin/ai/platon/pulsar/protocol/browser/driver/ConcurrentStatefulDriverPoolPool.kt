package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.stringify
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.crawl.fetch.privacy.BrowserId
import com.google.common.annotations.Beta
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.ConcurrentSkipListSet

/**
 * A concurrent pool of loading driver pools whose states are tracked.
 * */
class ConcurrentStatefulDriverPoolPool {
    private val logger = getLogger(this)
    /**
     * Working driver pools
     * */
    private val _workingDriverPools = ConcurrentSkipListMap<BrowserId, LoadingWebDriverPool>()

    /**
     * Retired but not closed driver pools
     * */
    private val _retiredDriverPools = ConcurrentSkipListMap<BrowserId, LoadingWebDriverPool>()
    /**
     * Closed driver pool ids
     * */
    private val _closedDriverPools = ConcurrentSkipListSet<BrowserId>()

    val workingDriverPools: Map<BrowserId, LoadingWebDriverPool> get() = _workingDriverPools

    val retiredDriverPools: Map<BrowserId, LoadingWebDriverPool> get() = _retiredDriverPools

    val closedDriverPools: Set<BrowserId> get() = _closedDriverPools
    /**
     * Return the number of new drivers can offer by the pool at the calling time point.
     *
     * If the driver pool is retired or closed, no new driver can offer.
     * If the driver pool is not created yet, it can offer [capacity] drivers at top.
     *
     * @param browserId The id of the webdriver pool.
     * @param capacity The capacity of a driver pool.
     * @return The number of new drivers can offer by the pool at the calling time point.
     * */
    @Synchronized
    fun promisedDriverCount(browserId: BrowserId, capacity: Int): Int {
        if (browserId in closedDriverPools || browserId in retiredDriverPools) {
            return 0
        }

        // if the
        val pool = _workingDriverPools[browserId] ?: return capacity
        return pool.numAvailable
    }
    /**
     * Check if a webdriver pool is full capacity, so it can not provide a webdriver for new tasks.
     * Note that if a driver pool is retired or closed, it's not full capacity.
     *
     * @param browserId The id of the webdriver pool.
     * @return True if the webdriver pool is full capacity, false otherwise.
     * */
    @Synchronized
    fun isFullCapacity(browserId: BrowserId): Boolean {
        if (browserId in closedDriverPools || browserId in retiredDriverPools) {
            return false
        }

        val pool = _workingDriverPools[browserId] ?: return false
        return pool.numWorking + pool.numWaiting >= pool.capacity
    }

    @Synchronized
    fun isRetiredPool(browserId: BrowserId) = retiredDriverPools.contains(browserId)

    @Beta
    @Synchronized
    fun subscribeDriver(browserId: BrowserId): WebDriver? {
        if (browserId in closedDriverPools || browserId in retiredDriverPools) {
            return null
        }

        val pool = _workingDriverPools[browserId] ?: return null
        val driver = pool.poll()
        // _subscribedDrivers.add(driver)
        return driver
    }

    @Beta
    @Synchronized
    fun subscribeDriver(): WebDriver? {
        val driverPool = _workingDriverPools.values.firstOrNull { it.numAvailable > 0 } ?: return null
        val driver = driverPool.poll()
        // _subscribedDrivers.add(driver)
        return driver
    }

    @Synchronized
    fun computeIfAbsent(browserId: BrowserId, mappingFunction: (BrowserId) -> LoadingWebDriverPool): LoadingWebDriverPool {
        return _workingDriverPools.computeIfAbsent(browserId, mappingFunction)
    }

    @Synchronized
    fun retire(driverPool: LoadingWebDriverPool) {
        val browserId = driverPool.browserId
        _workingDriverPools.remove(browserId)

        if (browserId in _closedDriverPools) {
            logger.warn("Inconsistent driver pool state: retire pool who is already closed | {}", browserId)
        }

        driverPool.retire()
        _retiredDriverPools[browserId] = driverPool
    }

    @Synchronized
    fun retire(browserId: BrowserId): LoadingWebDriverPool? {
        val retiredDriverPool = _workingDriverPools.remove(browserId)
        if (retiredDriverPool != null) {
            retire(retiredDriverPool)
        }
        return retiredDriverPool
    }

    @Synchronized
    fun close(driverPool: LoadingWebDriverPool) {
        val browserId = driverPool.browserId

        _workingDriverPools.remove(browserId)
        _retiredDriverPools.remove(browserId)
        _closedDriverPools.add(browserId)

        kotlin.runCatching { driverPool.close() }.onFailure { getLogger(this).warn(it.stringify()) }
    }

    @Synchronized
    fun close() {
        val pools = workingDriverPools.values + retiredDriverPools.values
        pools.forEach { close(it) }
    }
}
