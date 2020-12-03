package ai.platon.pulsar.common

import com.google.common.base.Predicates
import java.time.Duration
import java.util.concurrent.TimeUnit

enum class FlowState {
    CONTINUE, BREAK;

    val isContinue get() = this == CONTINUE
}

enum class Priority(val value: Int) {
    HIGHEST(0), HIGHER(100), NORMAL(1000), LOWER(1100), LOWEST(1200)
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
