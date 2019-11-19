package ai.platon.pulsar.ql

import ai.platon.pulsar.PulsarContext
import org.junit.Test
import kotlin.test.assertEquals

class TestMetadataFunctions : TestBase() {

    @Test
    fun testMetadata() {
        val url = urlGroups["mia"]!![2]

        val pulsar = PulsarContext.createSession()
        val page = pulsar.load(url)
        val doc = pulsar.parse(page)
        assertEquals(page.url, doc.location)

        execute("SELECT * FROM META_PARSE('$url')")
    }
}
