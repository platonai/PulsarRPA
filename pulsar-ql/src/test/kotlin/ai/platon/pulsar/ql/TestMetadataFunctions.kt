package ai.platon.pulsar.ql

import ai.platon.pulsar.context.PulsarContexts
import org.junit.Test
import kotlin.test.assertEquals

class TestMetadataFunctions : TestBase() {
    private val urlGroups = TestResource.urlGroups
    private val session = PulsarContexts.createSession()

    @Test
    fun testGet() {
        val url = urlGroups["jd"]!![2]

        val page = session.load(url)
        val doc = session.parse(page)

        // execute("SELECT DOM_GET('$url')")
    }
}
