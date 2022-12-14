package ai.platon.pulsar.persist

import ai.platon.pulsar.common.config.VolatileConfig
import org.junit.Test
import kotlin.test.assertEquals

class TestPageModel {

    private val url = "https://www.amazon.com/dp/B082P8J28M"
    private val conf = VolatileConfig()
    private val groupId = 43853791

    @Test
    fun testEmplace() {
        val page = WebPage.newWebPage(url, conf)
        val pageModel = page.ensurePageModel()
        pageModel.emplace(groupId, "", mapOf("a" to "1", "b" to "2"))
        assertEquals("1", pageModel.findById(groupId)?.get("a"))

        pageModel.emplace(groupId, "", mapOf("c" to "3", "d" to "4"))
        assertEquals("4", pageModel.findById(groupId)?.get("d"))
    }
}
