package ai.platon.pulsar.persist

import ai.platon.pulsar.common.config.VolatileConfig
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class TestPageModel {

    private val baseUrl = "https://www.amazon.com/dp/B082P8J28M"
    private val conf = VolatileConfig()
    private val groupId = 43853791

    @Test
    fun testEmplace() {
        val page = WebPage.newWebPage(baseUrl, conf)
        val pageModel = page.ensurePageModel()
        pageModel.emplace(groupId, "", mapOf("a" to "1", "b" to "2"))
        assertEquals("1", pageModel.findGroup(groupId)?.get("a"))

        pageModel.emplace(groupId, "", mapOf("c" to "3", "d" to "4"))
        assertEquals("4", pageModel.findGroup(groupId)?.get("d"))
    }

    @Test
    fun testAccess() {
        val page = WebPage.newWebPage(baseUrl, conf)
        val pageModel = page.ensurePageModel()

        assertNotEquals("1", pageModel.findValue(1, "a"))

        pageModel.set(1, "a", "1")
        assertEquals("1", pageModel.findValue(1, "a"))

        pageModel.remove(1, "a")
        assertNotEquals("1", pageModel.findValue(1, "a"))
    }
}
