package ai.platon.pulsar.common.concurrent

import ai.platon.pulsar.common.Systems
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes
import kotlinx.coroutines.scheduling.ExperimentalCoroutineDispatcher

// @see [kotlinx.coroutines.scheduling.ExperimentalCoroutineDispatcher]
private const val BLOCKING_SHIFT = 21 // 2M threads max
private const val MIN_SUPPORTED_POOL_SIZE = 1 // we support 1 for test purposes, but it is not usually used
private const val MAX_SUPPORTED_POOL_SIZE = (1 shl BLOCKING_SHIFT) - 2
private val CORE_POOL_SIZE = Systems.getProperty(
        "kotlinx.coroutines.scheduler.core.pool.size",
        AppConstants.NCPU.coerceAtLeast(2), // !!! at least two here
        minValue = MIN_SUPPORTED_POOL_SIZE
)
private val MAX_POOL_SIZE = Systems.getProperty(
        "kotlinx.coroutines.scheduler.max.pool.size",
        (AppConstants.NCPU * 128).coerceIn(CORE_POOL_SIZE, MAX_SUPPORTED_POOL_SIZE),
        maxValue = MAX_SUPPORTED_POOL_SIZE
)

/**
 * The name prefix of fetch workers
 * */
private val FETCHER_NAME_PREFIX = Systems.getProperty(CapabilityTypes.FETCH_WORKER_NAME_PREFIX, "F")

/**
 * Default instance of coroutine dispatcher for fetch jobs
 */
//object FetchScheduler : ExperimentalCoroutineDispatcher(CORE_POOL_SIZE, MAX_POOL_SIZE, FETCHER_NAME_PREFIX) {
//    // The fetch concurrency depends on the number of process of browsers which is the most critical resource
//    val IO = blocking(Systems.getProperty(CapabilityTypes.FETCH_CONCURRENCY, 64.coerceAtLeast(AppConstants.NCPU)))
//}
