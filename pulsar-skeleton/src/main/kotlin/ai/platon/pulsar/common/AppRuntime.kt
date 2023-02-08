package ai.platon.pulsar.common

import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.measure.ByteUnit
import ai.platon.pulsar.common.measure.ByteUnitConverter
import oshi.SystemInfo
import oshi.hardware.CentralProcessor
import java.time.Duration
import java.time.Instant

/**
 * Project specific application runtime information
 * */
class AppRuntime {
    companion object {
        private var prevCPUTicks = LongArray(CentralProcessor.TickType.values().size)

        val startTime = Instant.now()
        val elapsedTime get() = Duration.between(startTime, Instant.now())

        val systemInfo = SystemInfo()
        // OSHI cached the value, so it's fast and safe to be called frequently
        val memoryInfo get() = systemInfo.hardware.memory

        /**
         * System cpu load in [0, 1]
         * */
        val systemCpuLoad get() = computeSystemCpuLoad()

        val isHighCPULoad get() = systemCpuLoad > 0.95
        /**
         * Free memory in bytes
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

        val totalMemory get() = Runtime.getRuntime().totalMemory()
        val totalMemoryGiB get() = ByteUnit.BYTE.toGiB(totalMemory.toDouble())
        val availableMemoryGiB get() = ByteUnit.BYTE.toGiB(availableMemory.toDouble())
        //        private val memoryToReserveLarge get() = conf.getDouble(
//            CapabilityTypes.BROWSER_MEMORY_TO_RESERVE_KEY,
//            AppConstants.DEFAULT_BROWSER_RESERVED_MEMORY
//        )
        // TODO: configurable
        val memoryToReserve = when {
            totalMemoryGiB >= 14 -> ByteUnit.GIB.toBytes(3.0) // 3 GiB
            totalMemoryGiB >= 30 -> AppConstants.DEFAULT_BROWSER_RESERVED_MEMORY
            else -> AppConstants.BROWSER_TAB_REQUIRED_MEMORY
        }

        val isLowMemory get() = availableMemory < memoryToReserve

        val freeDiskSpaces get() = Runtimes.unallocatedDiskSpaces()

        val isLowDiskSpace get() = checkIsOutOfDisk()

        val isInsufficientHardwareResources get() = isLowMemory || isHighCPULoad || isLowDiskSpace

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
