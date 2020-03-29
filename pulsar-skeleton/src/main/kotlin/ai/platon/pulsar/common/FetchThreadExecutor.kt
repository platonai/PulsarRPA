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

/**
 * TODO: use [MultithreadEventExecutorGroup] instead
 * */
class FetchThreadExecutor(conf: ImmutableConfig) : AutoCloseable {
    private val log = LoggerFactory.getLogger(FetchThreadExecutor::class.java)

    private var executor: ExecutorService? = null
    private val closed = AtomicBoolean(false)
    private val threadFactory = ForkJoinWorkerThreadFactory { pool ->
        ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool).also { it.name = String.format("w%02d", it.poolIndex) }
    }
    val concurrency = conf.getInt(CapabilityTypes.FETCH_CONCURRENCY, AppConstants.FETCH_THREADS)

    init {
        Params.of(
                "availableProcessors", AppConstants.NCPU,
                "concurrency", concurrency
        ).withLogger(log).info(true)
    }

    fun <T> submit(task: () -> T): Future<T> {
        return getOrCreate().submit(task)
    }

    private fun getOrCreate(): ExecutorService {
        synchronized(FetchThreadExecutor::class.java) {
            if (executor == null) {
                executor = newWorkStealingPool(concurrency)
            }
            return executor!!
        }
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
