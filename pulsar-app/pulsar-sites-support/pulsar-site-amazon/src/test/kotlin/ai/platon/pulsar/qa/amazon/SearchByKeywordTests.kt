package ai.platon.pulsar.qa.amazon

import ai.platon.pulsar.qa.CheckEntry
import ai.platon.pulsar.qa.QABase
import ai.platon.pulsar.qa.assertAnyRecordsNotBlank
import kotlin.test.Test

class SearchByKeywordTests : QABase() {
    private val resourcePrefix = "config/sites/amazon/crawl/parse/sql"
    private val sqlResource = "$resourcePrefix/keyword-asin-extract.sql"

    @Test
    fun `When extract a page which is gone then warning message exists`() {
        var url = "https://www.amazon.com/s?rh=n:19539094011&rd=1&fs=true&page=14"
        assertAnyRecordsNotBlank(CheckEntry(url, sqlResource, listOf("abstract")))

        url = "https://www.amazon.com/s?k=Bike+Rim+Brake+Sets&rh=n:6389286011&page=31"
        assertAnyRecordsNotBlank(CheckEntry(url, sqlResource, listOf("abstract")))
    }
}
