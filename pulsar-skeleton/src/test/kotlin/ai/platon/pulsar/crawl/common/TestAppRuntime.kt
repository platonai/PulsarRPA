package ai.platon.pulsar.crawl.common

import ai.platon.pulsar.common.AppRuntime
import ai.platon.pulsar.common.Runtimes
import org.junit.Test
import oshi.hardware.CentralProcessor.TickType
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random


class TestAppRuntime {
    var sum = 0.0
    var prevTicks = LongArray(TickType.values().size)

    @Test
    fun testCPULoad() {
        Executors.newScheduledThreadPool(1).scheduleAtFixedRate({
            println()
            measureCPU()
        }, 2, 2, TimeUnit.SECONDS)

        val nThreads = AppRuntime.systemInfo.hardware.processor.logicalProcessorCount - 2
        val executor = Executors.newFixedThreadPool(nThreads)
        repeat(nThreads) {
            executor.submit { compute() }
        }
        executor.awaitTermination(30, TimeUnit.SECONDS)
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
        val processor = AppRuntime.systemInfo.hardware.processor

        val cpuLoad = processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100
        prevTicks = processor.systemCpuLoadTicks
        println(String.format("cpuLoad: %.2f%%", cpuLoad))

        val systemLoadAverage = processor.getSystemLoadAverage(3).joinToString()
        println("Sys: $systemLoadAverage")
    }
}
