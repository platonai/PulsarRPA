package ai.platon.pulsar.common

import ai.platon.pulsar.PulsarEnv
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Params
import org.slf4j.LoggerFactory
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

/**
 * TODO: check necessary
 * */
class GlobalExecutor(immutableConfig: ImmutableConfig) : AutoCloseable {
    val log = LoggerFactory.getLogger(GlobalExecutor::class.java)

    private var executor: ExecutorService? = null
    private val closed = AtomicBoolean(false)
    private var autoConcurrencyFactor: Float = 0.toFloat()
    var concurrency: Int = 0
        private set

    init {
        val concurrencyHint = immutableConfig.getInt(CapabilityTypes.GLOBAL_EXECUTOR_CONCURRENCY_HINT, -1)
        concurrency = concurrencyHint
        if (concurrency <= 0) {
            autoConcurrencyFactor = immutableConfig.getFloat(CapabilityTypes.GLOBAL_EXECUTOR_AUTO_CONCURRENCY_FACTOR, 1f)
            concurrency = max((PulsarEnv.NCPU * autoConcurrencyFactor).toInt(), 4)
        }

        Params.of(
                "availableProcessors", PulsarEnv.NCPU,
                "autoConcurrencyFactor", autoConcurrencyFactor,
                "concurrencyHint", concurrencyHint,
                "concurrency", concurrency
        ).withLogger(log).info(true)
    }

    /**
     * TODO: Allocate executors for sessions separately
     */
    fun getExecutor(): ExecutorService {
        synchronized(GlobalExecutor::class.java) {
            if (executor == null) {
                executor = Executors.newWorkStealingPool(concurrency)
            }
            return executor!!
        }
    }

    fun <T> submit(task: () -> T): Future<T> {
        return getExecutor().submit(task)
    }

    override fun close() {
        if (closed.getAndSet(true)) {
            return
        }

        if (executor != null && !executor!!.isShutdown) {
            executor!!.shutdownNow()
        }
    }
}
