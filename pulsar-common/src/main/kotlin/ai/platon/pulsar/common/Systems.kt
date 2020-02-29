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
}
