package ai.platon.pulsar.common

import java.util.concurrent.TimeUnit

private val GC_DELAY = 50
private val MAX_GC = 8
private var lastGC: Long = 0

/**
 * Get the used memory in KB.
 * This method possibly calls System.gc().
 *
 * @return the used memory
 */
fun getMemoryUsed(): Int {
    collectGarbage()
    val rt = Runtime.getRuntime()
    val mem = rt.totalMemory() - rt.freeMemory()
    return (mem shr 10).toInt()
}

/**
 * Get the free memory in KB.
 * This method possibly calls System.gc().
 *
 * @return the free memory
 */
fun getMemoryFree(): Int {
    collectGarbage()
    val rt = Runtime.getRuntime()
    val mem = rt.freeMemory()
    return (mem shr 10).toInt()
}

/**
 * Get the maximum memory in KB.
 *
 * @return the maximum memory
 */
fun getMemoryMax(): Long {
    val max = Runtime.getRuntime().maxMemory()
    return max / 1024
}

@Synchronized
private fun collectGarbage() {
    val runtime = Runtime.getRuntime()
    var total = runtime.totalMemory()
    val time = System.nanoTime()
    if (lastGC + TimeUnit.MILLISECONDS.toNanos(GC_DELAY.toLong()) < time) {
        for (i in 0 until MAX_GC) {
            runtime.gc()
            val now = runtime.totalMemory()
            if (now == total) {
                lastGC = System.nanoTime()
                break
            }
            total = now
        }
    }
}

fun setPropertyIfAbsent(key: String, value: String) {
    if (System.getProperty(key) == null) {
        System.setProperty(key, value)
    }
}
