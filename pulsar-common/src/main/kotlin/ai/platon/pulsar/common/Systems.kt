package ai.platon.pulsar.common

import java.util.*
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
    val memoryUsed: Long get() = collectGarbage().let { Runtime.getRuntime() }.let { it.totalMemory() - it.freeMemory() }

    /**
     * Get the free memory in bytes.
     * This method possibly calls System.gc().
     *
     * @return the free memory
     */
    val memoryFree: Long get() = collectGarbage().let { Runtime.getRuntime().freeMemory() }

    /**
     * Get the maximum memory in bytes.
     *
     * @return the maximum memory
     */
    val memoryMax: Long get() = Runtime.getRuntime().maxMemory()

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

    fun setProperty(key: String, value: Any) {
        System.setProperty(key, value.toString())
    }

    fun setPropertyIfAbsent(key: String, value: String) {
        if (System.getProperty(key) == null) {
            System.setProperty(key, value)
        }
    }

    fun setPropertyIfAbsent(key: String, value: Any) {
        if (System.getProperty(key) == null) {
            System.setProperty(key, value.toString())
        }
    }

    fun getProperty(name: String, defaultValue: String): String = System.getProperty(name, defaultValue)

    fun getProperty(name: String, defaultValue: Long, minValue: Long = 1, maxValue: Long = Long.MAX_VALUE): Long {
        val value = System.getProperty(name) ?: return defaultValue
        val parsed = value.toLongOrNull()?: error("System property '$name' has unrecognized value '$value'")
        if (parsed !in minValue..maxValue) {
            error("System property '$name' should be in range $minValue..$maxValue, but is '$parsed'")
        }
        return parsed
    }

    fun getProperty(name: String, defaultValue: Int, minValue: Int = 1, maxValue: Int = Int.MAX_VALUE): Int {
        val value = System.getProperty(name) ?: return defaultValue
        val parsed = value.toIntOrNull()?: error("System property '$name' has unrecognized value '$value'")
        if (parsed !in minValue..maxValue) {
            error("System property '$name' should be in range $minValue..$maxValue, but is '$parsed'")
        }
        return parsed
    }

    fun getProperty(name: String, defaultValue: Boolean): Boolean {
        val value = System.getProperty(name)?.toLowerCase()?:return defaultValue
        return SParser(value).getBoolean(defaultValue)
    }

    fun loadAllProperties(resourceName: String, replaceIfExist: Boolean = false) {
        Properties().apply { load(ResourceLoader.getResourceAsStream(resourceName)) }.forEach { name, value ->
            takeIf { replaceIfExist }?.setProperty(name.toString(), value)?:setPropertyIfAbsent(name.toString(), value)
        }
    }
}
