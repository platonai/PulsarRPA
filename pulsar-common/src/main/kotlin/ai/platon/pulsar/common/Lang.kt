package ai.platon.pulsar.common

import com.google.common.base.Predicates
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.reflect.full.memberProperties

enum class FlowState {
    CONTINUE, BREAK;

    val isContinue get() = this == CONTINUE
}

// it seems that java reflection does not support typealias
// typealias CheckState = OpenResourceStatus
/**
 * Both 0 and 200 are good states
 * */
data class CheckState(
    val code: Int = 0,
    val message: String = "",
    val scope: String = ""
) {
    val isOK get() = code == 0 || code == 200
    val isNotOK get() = code != 0
}

/**
 * Smaller value, higher priority, keep consistent with PriorityQueue
 *
 * Notice: can not use Int.MIN_VALUE as the highest priority value nor Int.MAX_VALUE as the lowest, choose another one
 * */
enum class Priority5(val value: Int) {
    HIGHEST(Int.MIN_VALUE / 10),
    HIGHER(-1000),
    NORMAL(0),
    LOWER(1000),
    LOWEST(Int.MAX_VALUE / 10)
}

enum class Priority13(val value: Int) {
    HIGHEST(Int.MIN_VALUE / 10),
    HIGHER5(-5000),
    HIGHER4(-4000),
    HIGHER3(-3000),
    HIGHER2(-2000),
    HIGHER(-1000),
    NORMAL(0),
    LOWER(1000),
    LOWER2(2000),
    LOWER3(3000),
    LOWER4(4000),
    LOWER5(5000),
    LOWEST(Int.MAX_VALUE / 10);

    companion object {
        fun valueOfOrNull(name: String): Priority13? {
            return values().firstOrNull { it.name == name }
        }

        @Throws(IllegalArgumentException::class)
        fun valueOf(value: Int): Priority13 {
            return values().firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Illegal priority value $value, " +
                        "must be one of ${values().map { it.value }}")
        }

        fun valueOfOrNull(value: Int): Priority13? {
            return values().firstOrNull { it.value == value }
        }
    }
}

enum class Priority21(val value: Int) {
    HIGHEST(Int.MIN_VALUE / 10),
    HIGHER9(-9000),
    HIGHER8(-8000),
    HIGHER7(-7000),
    HIGHER6(-6000),
    HIGHER5(-5000),
    HIGHER4(-4000),
    HIGHER3(-3000),
    HIGHER2(-2000),
    HIGHER(-1000),
    NORMAL(0),
    LOWER(1000),
    LOWER2(2000),
    LOWER3(3000),
    LOWER4(4000),
    LOWER5(5000),
    LOWER6(6000),
    LOWER7(7000),
    LOWER8(8000),
    LOWER9(9000),
    LOWEST(Int.MAX_VALUE / 10)
}

interface StartStopRunnable {
    fun start()
    fun stop()

    fun restart() {
        stop()
        start()
    }
}

class StartStopRunner(val runnable: StartStopRunnable) {
    fun start() = runnable.start()
    fun stop() = runnable.stop()
}

/** Unsafe lazy, usually be used in single thread */
fun <T> usfLazy(initializer: () -> T): Lazy<T> = lazy(LazyThreadSafetyMode.NONE, initializer)

fun sleepSeconds(seconds: Long) {
    try {
        TimeUnit.SECONDS.sleep(seconds)
    } catch (e: InterruptedException) {
        Thread.currentThread().interrupt()
    }
}

fun sleepMillis(millis: Long) {
    try {
        TimeUnit.MILLISECONDS.sleep(millis)
    } catch (e: InterruptedException) {
        Thread.currentThread().interrupt()
    }
}

fun sleep(duration: Duration) {
    try {
        Thread.sleep(duration.toMillis())
    } catch (e: InterruptedException) {
        Thread.currentThread().interrupt()
    }
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
