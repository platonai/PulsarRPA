package ai.platon.pulsar.common

import ai.platon.pulsar.common.measure.ByteUnit

import kotlin.test.*

class TestMeasures {

    @Test
    fun testByteUnit() {
        assertEquals(500 * 1024 * 1024L, ByteUnit.MIB.toBytes(500.0).toLong())
        assertEquals(2 * 1024 * 1024 * 1024L, ByteUnit.GIB.toBytes(2.0).toLong())
    }
}
