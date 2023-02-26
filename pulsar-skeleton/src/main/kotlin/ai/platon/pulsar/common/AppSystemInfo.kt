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

            // Returns the "recent cpu usage" for the whole system by counting ticks
            val cpuLoad = processor.getSystemCpuLoadBetweenTicks(prevCPUTicks)
            // Get System-wide CPU Load tick counters. Returns an array with seven elements
            // representing milliseconds spent in User (0), Nice (1), System (2), Idle (3), IOwait (4),
            // Hardware interrupts (IRQ) (5), Software interrupts/DPC (SoftIRQ) (6), or Steal (7) states.
            prevCPUTicks = processor.systemCpuLoadTicks
//            println(String.format("cpuLoad: %.2f%%", cpuLoad))

//            val systemLoadAverage = processor.getSystemLoadAverage(3).joinToString()
//            println("Sys: $systemLoadAverage")

            return cpuLoad
        }
    }
}

@Deprecated("Inappropriate name", ReplaceWith("HardwareResource"))
typealias AppRuntime = AppSystemInfo
