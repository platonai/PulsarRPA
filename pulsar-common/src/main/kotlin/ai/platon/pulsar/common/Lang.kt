package ai.platon.pulsar.common

import com.google.common.base.Predicates
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.reflect.full.memberProperties

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

object ObjectConverter {

    inline fun <reified T : Any> asMap(t: T) : Map<String, Any?> {
        val props = T::class.memberProperties.associateBy { it.name }
        return props.keys.associateWith { props[it]?.get(t) }
    }

    inline fun <reified T : Any> asQueryParameters(t: T, excludes: Iterable<String> = listOf()) : String {
        val props = T::class.memberProperties.associateBy { it.name }
        return props.entries.asSequence()
                .filter { it.key !in excludes }
                .map { it.key to props[it.key]?.get(t) }
                .filter { it.second != null }
                .joinToString("&") { (k, v) -> "$k=$v" }
    }
}
