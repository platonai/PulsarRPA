package ai.platon.pulsar.common

import org.apache.commons.lang3.RandomStringUtils
import java.util.*
import kotlin.test.Test
import kotlin.test.assertTrue

class TestCollections {

    @Test
    fun `When group by than the order of keys are reserved`() {
        val sortedEntries = IntRange(1, 100).map {
            Random(10).nextInt() to RandomStringUtils.randomAlphanumeric(10)
        }.shuffled().sortedBy { it.first }
        val groupedEntries = sortedEntries.groupBy { it.first }
        groupedEntries.entries.zipWithNext().forEach {
            assertTrue { it.first.key <= it.second.key }
        }
    }
}
