package ai.platon.pulsar.common

import ai.platon.pulsar.common.measure.ByteUnit
import org.junit.Test
import kotlin.test.assertEquals

class TestMeasures {

    @Test
    fun testByteUnit() {
        assertEquals(500 * 1024 * 1024, ByteUnit.MIB.toBytes(500.0).toLong())
    }
}
