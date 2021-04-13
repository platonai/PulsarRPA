package ai.platon.pulsar.qa.amazon

import ai.platon.pulsar.qa.CheckEntry
import ai.platon.pulsar.qa.QABase
import ai.platon.pulsar.qa.assertAllRecordsNotBlank
import kotlin.test.Test

class TestCategory : QABase() {
    private val resourcePrefix = "config/sites/amazon/crawl/parse/sql"
    private val defaultSqlResource = "$resourcePrefix/category-navigation.sql"
    private val defaultUrl = "https://www.amazon.com/b?node=19562656011"

    @Test
    fun `When extract asin then success`() {
        val fields = listOf("results", "pagination")
        assertAllRecordsNotBlank(CheckEntry(defaultUrl, defaultSqlResource, fields))
    }
}
