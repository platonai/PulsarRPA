package ai.platon.pulsar.ql

import ai.platon.pulsar.context.PulsarContexts
import org.junit.Test
import kotlin.test.assertEquals

class TestMetadataFunctions : TestBase() {
    private val urlGroups = TestResource.urlGroups

    @Test
    fun testMetadata() {
        val url = urlGroups["mia"]!![2]

        val pulsar = PulsarContexts.createSession()
        val page = pulsar.load(url)
        val doc = pulsar.parse(page)
        assertEquals(page.url, doc.location)

        execute("SELECT * FROM META_PARSE('$url')")
    }
}
