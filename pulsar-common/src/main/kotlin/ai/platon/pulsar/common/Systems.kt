package ai.platon.pulsar.common

import java.util.concurrent.TimeUnit

object Systems {

    private const val GC_DELAY = 50
    private const val MAX_GC = 8
    private var lastGC: Long = 0

    /**
     * Get the used memory in KB.
     * This method possibly calls System.gc().
     *
     * @return the used memory
     */
    fun getMemoryUsed(): Long {
        collectGarbage()
        val rt = Runtime.getRuntime()
        return rt.totalMemory() - rt.freeMemory()
    }

    /**
     * Get the free memory in bytes.
     * This method possibly calls System.gc().
     *
     * @return the free memory
     */
    fun getMemoryFree(): Long {
        collectGarbage()
        val rt = Runtime.getRuntime()
        return rt.freeMemory()
    }

    /**
     * Get the maximum memory in bytes.
     *
     * @return the maximum memory
     */
    fun getMemoryMax(): Long {
        return Runtime.getRuntime().maxMemory()
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

    fun getProperty(propertyName: String, defaultValue: String): String = System.getProperty(propertyName, defaultValue)

    fun getProperty(
            propertyName: String,
            defaultValue: Long,
            minValue: Long = 1,
            maxValue: Long = Long.MAX_VALUE
    ): Long {
        val value = System.getProperty(propertyName) ?: return defaultValue
        val parsed = value.toLongOrNull()?: error("System property '$propertyName' has unrecognized value '$value'")
        if (parsed !in minValue..maxValue) {
            error("System property '$propertyName' should be in range $minValue..$maxValue, but is '$parsed'")
        }
        return parsed
    }

    fun getProperty(
            propertyName: String,
            defaultValue: Int,
            minValue: Int = 1,
            maxValue: Int = Int.MAX_VALUE
    ): Int {
        val value = System.getProperty(propertyName) ?: return defaultValue
        val parsed = value.toIntOrNull()?: error("System property '$propertyName' has unrecognized value '$value'")
        if (parsed !in minValue..maxValue) {
            error("System property '$propertyName' should be in range $minValue..$maxValue, but is '$parsed'")
        }
        return parsed
    }
}
