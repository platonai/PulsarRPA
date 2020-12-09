package ai.platon.pulsar.common

import com.google.common.base.Predicates
import java.time.Duration
import java.util.concurrent.TimeUnit

enum class FlowState {
    CONTINUE, BREAK;

    val isContinue get() = this == CONTINUE
}

/**
 * Smaller value, higher priority, keep consistent with PriorityQueue
 *
 * Notice: can not use Int.MIN_VALUE as the highest priority value nor Int.MAX_VALUE as the lowest, choose another one
 * */
enum class Priority(val value: Int) {
    HIGHEST(Int.MIN_VALUE / 10), HIGHER(-1000), NORMAL(0), LOWER(1000), LOWEST(Int.MAX_VALUE / 10)
}

/** Unsafe lazy, usually be used in single thread */
fun <T> usfLazy(initializer: () -> T): Lazy<T> = lazy(LazyThreadSafetyMode.NONE, initializer)

fun sleepSeconds(seconds: Long) {
    runCatching { TimeUnit.SECONDS.sleep(seconds) }.onFailure { Thread.currentThread().interrupt() }
}

fun sleep(duration: Duration) {
    runCatching { Thread.sleep(duration.toMillis()) }.onFailure { Thread.currentThread().interrupt() }
}

/** Always false and have no static check warning */
fun alwaysFalse(): Boolean {
    return Predicates.alwaysFalse<Boolean>().apply(false)
}

/** Always true and have no static check warning */
fun alwaysTrue(): Boolean {
    return Predicates.alwaysTrue<Boolean>().apply(true)
}
