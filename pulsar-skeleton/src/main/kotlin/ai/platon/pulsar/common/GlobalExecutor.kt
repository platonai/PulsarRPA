package ai.platon.pulsar.common

import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Params
import org.slf4j.LoggerFactory
import java.util.concurrent.ExecutorService
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

class GlobalExecutor(conf: ImmutableConfig) : AutoCloseable {
    val log = LoggerFactory.getLogger(GlobalExecutor::class.java)

    private var executor: ExecutorService? = null
    private val closed = AtomicBoolean(false)
    private var autoConcurrencyFactor = conf.getFloat(CapabilityTypes.GLOBAL_EXECUTOR_AUTO_CONCURRENCY_FACTOR, 1f)
    private val threadFactory = ForkJoinWorkerThreadFactory { pool ->
        ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool).also { it.name = String.format("w%02d", it.poolIndex) }
    }
    private val concurrencyHint = conf.getInt(CapabilityTypes.GLOBAL_EXECUTOR_CONCURRENCY_HINT, -1)
    val concurrency = if (concurrencyHint > 0) concurrencyHint else max((AppConstants.NCPU * autoConcurrencyFactor).toInt(), 4)

    init {
        Params.of(
                "availableProcessors", AppConstants.NCPU,
                "autoConcurrencyFactor", autoConcurrencyFactor,
                "concurrencyHint", concurrencyHint,
                "concurrency", concurrency
        ).withLogger(log).info(true)
    }

    fun getExecutor(): ExecutorService {
        synchronized(GlobalExecutor::class.java) {
            if (executor == null) {
                executor = newWorkStealingPool(concurrency)
            }
            return executor!!
        }
    }

    fun <T> submit(task: () -> T): Future<T> {
        return getExecutor().submit(task)
    }

    /**
     * copy from [java.util.concurrent.Executors]
     * */
    private fun newWorkStealingPool(parallelism: Int): ExecutorService {
        return ForkJoinPool(parallelism, threadFactory, null as Thread.UncaughtExceptionHandler?, true)
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            executor?.shutdown()
        }
    }
}
