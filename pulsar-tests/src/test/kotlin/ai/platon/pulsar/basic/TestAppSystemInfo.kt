package ai.platon.pulsar.basic

import ai.platon.pulsar.common.logPrintln
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
            logPrintln("Operation system: $versionInfo")
        }.onFailure { logPrintln(it.brief()) }

        runCatching {
            val processor = si.hardware.processor
            logPrintln("Processor: $processor")
        }.onFailure { logPrintln(it.brief()) }

        runCatching {
            val memory = si.hardware.memory
            logPrintln("Memory: $memory")
        }.onFailure { logPrintln(it.brief()) }
    }
}

