package ai.platon.pulsar.common

import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.measure.ByteUnit
import ai.platon.pulsar.common.measure.ByteUnitConverter
import oshi.SystemInfo
import oshi.hardware.CentralProcessor
import java.time.Duration
import java.time.Instant

/**
 * Application specific system information
 * */
class AppSystemInfo {
    companion object {
        private var prevCPUTicks = LongArray(CentralProcessor.TickType.values().size)

        var CRITICAL_CPU_THRESHOLD = System.getProperty("critical.cpu.threshold") ?.toDoubleOrNull() ?: 0.85

        val startTime = Instant.now()
        val elapsedTime get() = Duration.between(startTime, Instant.now())

        val systemInfo = SystemInfo()

        // OSHI cached the value, so it's fast and safe to be called frequently
        val memoryInfo get() = systemInfo.hardware.memory

        /**
         * System cpu load in [0, 1]
         * */
        val systemCpuLoad get() = computeSystemCpuLoad()

        val isCriticalCPULoad get() = systemCpuLoad > CRITICAL_CPU_THRESHOLD

        /**
         * An array of the system load averages for 1, 5, and 15 minutes
         * with the size of the array specified by nelem; or negative values if not available.
         *
         * Load average, also called average system load, is an important metric that indicates
         * if there are multiple tasks in queue on the Linux server. The load average can be high or low,
         * depending on the number of cores your server has, how many CPUs are integrated into the system server,
         * and the load average number itself.
         *
         * A load average value is considered to be high when it’s greater than the number of CPUs the server has.
         * For example, if the number of CPUs in our server is only 4, but the load average we’re seeing is 5.4,
         * we’re experiencing a high load average.
         *
         * Load average is considered to be ideal when its value is lower than the number of CPUs in the Linux server.
         * For example, with only one CPU in the Linux server, it’s best if the load average is below 1.
         *
         * High load average tends to occurfor the three reasons mentioned below:
         * 1. A high number of threads executed in the server
         * 2. Lack of RAM forcing the server to use swap memory
         * 3. A high number of I/O traffic
         *
         * @see [Load average: What is it, and what's the best load average for your Linux servers?](https://www.site24x7.com/blog/load-average-what-is-it-and-whats-the-best-load-average-for-your-linux-servers)
         * */
        val systemLoadAverage get() = systemInfo.hardware.processor.getSystemLoadAverage(3)

        /**
         * Free memory in bytes.
         * Free memory is the amount of memory which is currently not used for anything.
         * This number should be small, because memory which is not used is simply wasted.
         * */
        val freeMemory get() = Runtime.getRuntime().freeMemory()
        val freeMemoryGiB get() = ByteUnit.BYTE.toGiB(freeMemory.toDouble())

        /**
         * Available memory in bytes.
         * Available memory is the amount of memory which is available for allocation to a new process or to existing
         * processes.
         * */
        val availableMemory get() = memoryInfo.available

        val usedMemory get() = memoryInfo.total - memoryInfo.available

        val totalMemory get() = Runtime.getRuntime().totalMemory()
        val totalMemoryGiB get() = ByteUnit.BYTE.toGiB(totalMemory.toDouble())
        val availableMemoryGiB get() = ByteUnit.BYTE.toGiB(availableMemory.toDouble())

        //        private val memoryToReserveLarge get() = conf.getDouble(
//            CapabilityTypes.BROWSER_MEMORY_TO_RESERVE_KEY,
//            AppConstants.DEFAULT_BROWSER_RESERVED_MEMORY
//        )
        val criticalMemoryMiB get() = System.getProperty("critical.memory.MiB")?.toDouble() ?: 0.0
        val actualCriticalMemory = when {
            criticalMemoryMiB > 0 -> ByteUnit.MIB.toBytes(criticalMemoryMiB)
            totalMemoryGiB >= 14 -> ByteUnit.GIB.toBytes(3.0) // 3 GiB
            totalMemoryGiB >= 30 -> AppConstants.DEFAULT_BROWSER_RESERVED_MEMORY
            else -> AppConstants.BROWSER_TAB_REQUIRED_MEMORY
        }

        val isCriticalMemory get() = availableMemory < actualCriticalMemory

        val freeDiskSpaces get() = Runtimes.unallocatedDiskSpaces()

        val isCriticalDiskSpace get() = checkIsOutOfDisk()

        val isCriticalResources get() = isCriticalMemory || isCriticalCPULoad || isCriticalDiskSpace

        private fun checkIsOutOfDisk(): Boolean {
            val freeSpace = freeDiskSpaces.maxOfOrNull { ByteUnitConverter.convert(it, "G") } ?: 0.0
            return freeSpace < 10.0
        }

        private fun computeSystemCpuLoad(): Double {
            val processor = systemInfo.hardware.processor

            synchronized(prevCPUTicks) {
                // Returns the "recent cpu usage" for the whole system by counting ticks
                val cpuLoad = processor.getSystemCpuLoadBetweenTicks(prevCPUTicks)
                // Get System-wide CPU Load tick counters. Returns an array with seven elements
                // representing milliseconds spent in User (0), Nice (1), System (2), Idle (3), IOwait (4),
                // Hardware interrupts (IRQ) (5), Software interrupts/DPC (SoftIRQ) (6), or Steal (7) states.
                prevCPUTicks = processor.systemCpuLoadTicks
                return cpuLoad
            }
        }
    }
}

@Deprecated("Inappropriate name", ReplaceWith("HardwareResource"))
typealias AppRuntime = AppSystemInfo
