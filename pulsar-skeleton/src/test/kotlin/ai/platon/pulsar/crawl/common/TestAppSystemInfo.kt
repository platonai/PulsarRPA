package ai.platon.pulsar.crawl.common

import ai.platon.pulsar.common.AppSystemInfo
import org.junit.Test
import oshi.SystemInfo
import oshi.hardware.CentralProcessor.TickType
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class TestAppSystemInfo {
    var sum = 0.0
    var prevTicks = LongArray(TickType.values().size)

    @Test
    fun testOSHIAvailable() {
        val si = SystemInfo()

        runCatching {
            val versionInfo = si.operatingSystem.versionInfo
            println("Operation system: $versionInfo")
        }.onFailure { println(it.message) }

        runCatching {
            val processor = si.hardware.processor
            println("Processor: $processor")
        }.onFailure { println(it.message) }
        
        runCatching {
            val memory = si.hardware.memory
            println("Memory: $memory")
        }.onFailure { println(it.message) }
    }
    
    @Test
    fun testOSVersionInfo() {
        val systemInfo = AppSystemInfo.systemInfo ?: return

        val versionInfo = systemInfo.operatingSystem.versionInfo
        println(versionInfo)
    }
    
    @Test
    fun testSystemCpuLoad() {
        val systemCpuLoad = AppSystemInfo.systemCpuLoad
        assert(systemCpuLoad > 0)
    }
    
    @Test
    fun testCPULoad() {
        val systemInfo = AppSystemInfo.systemInfo ?: return
        
        Executors.newScheduledThreadPool(1).scheduleAtFixedRate({
            println()
            measureCPU()
        }, 2, 2, TimeUnit.SECONDS)

        val nThreads = systemInfo.hardware.processor.logicalProcessorCount - 2
        val executor = Executors.newFixedThreadPool(nThreads)
        repeat(nThreads) {
            executor.submit { compute() }
        }
        executor.awaitTermination(10, TimeUnit.SECONDS)
        println(sum)
    }

    fun compute() {
        var result = Random.nextDouble(1.0)

        val endTime = Instant.now().plusSeconds(10)
        while (Instant.now().isBefore(endTime)) {
            result = 0.5 * (sin(result) + cos(result))
            sum += result
        }
    }

    fun measureCPU() {
        val systemInfo = AppSystemInfo.systemInfo ?: return
        
        val processor = systemInfo.hardware.processor

        val cpuLoad = processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100
        prevTicks = processor.systemCpuLoadTicks
        println(String.format("cpuLoad: %.2f%%", cpuLoad))

        val systemLoadAverage = processor.getSystemLoadAverage(3).joinToString()
        println("Sys: $systemLoadAverage")
    }
}
