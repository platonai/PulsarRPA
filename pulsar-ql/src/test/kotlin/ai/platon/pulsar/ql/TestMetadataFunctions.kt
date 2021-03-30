package ai.platon.pulsar.ql

import ai.platon.pulsar.context.PulsarContexts
import org.junit.Test
import kotlin.test.assertEquals

class TestMetadataFunctions : TestBase() {
    private val urlGroups = TestResource.urlGroups
    private val session = PulsarContexts.createSession()

    @Test
    fun testMetadata() {
        val url = urlGroups["jd"]!![2]

        val page = session.load(url)
        val doc = session.parse(page)
        assertEquals(page.url, doc.location)

        execute("SELECT * FROM META_PARSE('$url')")
    }
}
