package ai.platon.pulsar.common

import kotlin.test.*

data class DataClass11(val s: String)
data class DataClass12(val s: String)

data class DataClass21(val s: String, val s2: String)
data class DataClass22(val s: String, val s2: String)

class TestLanguage {

    @Test
    fun testDataClassEquality() {
        assertEquals(DataClass11(""), DataClass11(""))
        assertNotEquals(DataClass11("1"), DataClass11("2"))

        assertEquals(DataClass21("", ""), DataClass21("", ""))
        assertEquals(DataClass21("1", "2"), DataClass21("1", "2"))
    }

}
