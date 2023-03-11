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

    @Synchronized
    fun promisedDriverCount(browserId: BrowserId, capacity: Int): Int {
        if (browserId in closedDriverPools || browserId in retiredDriverPools) {
            return 0
        }

        val pool = _workingDriverPools[browserId] ?: return capacity
        return pool.numAvailable
    }

    @Synchronized
    fun isFullCapacity(browserId: BrowserId): Boolean {
        if (browserId in closedDriverPools || browserId in retiredDriverPools) {
            return false
        }

        val pool = _workingDriverPools[browserId] ?: return false
        return pool.numWorking + pool.numWaiting >= pool.capacity
    }

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
        driverPool.retire()
        _retiredDriverPools[browserId] = driverPool
        _workingDriverPools.remove(browserId)
    }

    @Synchronized
    fun retire(browserId: BrowserId): LoadingWebDriverPool? {
        val retiredDriverPool = workingDriverPools[browserId]
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
