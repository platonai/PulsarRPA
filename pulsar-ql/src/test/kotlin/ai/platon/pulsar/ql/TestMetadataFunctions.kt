package ai.platon.pulsar.ql

import org.junit.Test

class TestMetadataFunctions : TestBase() {
    private val urlGroups = TestResource.urlGroups

    @Test
    fun testGet() {
        val url = urlGroups["jd"]!![2]

        val page = session.load(url)
        val doc = session.parse(page)

        // execute("SELECT DOM_GET('$url')")
    }
}
