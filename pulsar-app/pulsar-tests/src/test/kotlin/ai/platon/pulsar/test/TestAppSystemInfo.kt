package ai.platon.pulsar.test

import ai.platon.pulsar.common.brief
import kotlin.test.*
import oshi.SystemInfo

/**
    ISSUE: OSHI failed on Windows in module pulsar-tests and productions,
    VERSION: pulsar-1.10.12
    CAUSE: jna relevant packages are overridden by kotlin-compiler-${version}.jar:com.sun.jna.Memory,
    which has no close() method.
 * */
class TestAppSystemInfo {
    
    @Test
    fun testOSHIAvailable() {
        val si = SystemInfo()
        
        runCatching {
            val versionInfo = si.operatingSystem.versionInfo
            println("Operation system: $versionInfo")
        }.onFailure { println(it.brief()) }
        
        runCatching {
            val processor = si.hardware.processor
            println("Processor: $processor")
        }.onFailure { println(it.brief()) }
        
        runCatching {
            val memory = si.hardware.memory
            println("Memory: $memory")
        }.onFailure { println(it.brief()) }
    }
}
