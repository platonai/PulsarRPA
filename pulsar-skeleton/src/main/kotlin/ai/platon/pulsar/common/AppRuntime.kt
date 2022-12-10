package ai.platon.pulsar.common

import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.measure.ByteUnit
import oshi.SystemInfo
import java.time.Duration
import java.time.Instant

/**
 * Project specific application runtime information
 * */
class AppRuntime {
    companion object {

        val startTime = Instant.now()
        val elapsedTime get() = Duration.between(startTime, Instant.now())

        val systemInfo = SystemInfo()
        // OSHI cached the value, so it's fast and safe to be called frequently
        val memoryInfo get() = systemInfo.hardware.memory
        /**
         * Free memory in bytes
         * Free memory is the amount of memory which is currently not used for anything.
         * This number should be small, because memory which is not used is simply wasted.
         * */
        val freeMemory get() = Runtime.getRuntime().freeMemory()
        val freeMemoryGiB get() = ByteUnit.BYTE.toGiB(freeMemory.toDouble())
        /**
         * Available memory in bytes
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
        val memoryToReserve = when {
            totalMemoryGiB >= 14 -> ByteUnit.GIB.toBytes(3.0) // 3 GiB
            totalMemoryGiB >= 30 -> AppConstants.DEFAULT_BROWSER_RESERVED_MEMORY
            else -> AppConstants.BROWSER_TAB_REQUIRED_MEMORY
        }

        val freeSpace get() = Runtimes.unallocatedDiskSpaces()

        val isLowMemory get() = availableMemory < memoryToReserve
    }
}
