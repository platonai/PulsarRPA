package ai.platon.pulsar.persist

import ai.platon.pulsar.common.config.VolatileConfig
import org.apache.gora.mongodb.store.MongoStore
import org.apache.gora.persistency.impl.DirtyListWrapper
import org.junit.Test
import kotlin.test.*

class TestPageModel {

    private val baseUrl = "https://www.amazon.com/dp/B082P8J28M"
    private val conf = VolatileConfig()
    private val webDb = WebDb(conf)
    private val groupId = 43853791

    @Test
    fun testEmplace() {
        val page = WebPage.newWebPage(baseUrl, conf)
        val pageModel = page.ensurePageModel()

        pageModel.emplace(groupId, "", mapOf("a" to "1", "b" to "2"))
        assertTrue { pageModel.unbox().isDirty }
        assertTrue { pageModel.unbox().fieldGroups[0].isDirty }

        assertEquals("1", pageModel.findGroup(groupId)?.get("a"))

        pageModel.emplace(groupId, "", mapOf("c" to "3", "d" to "4"))
        assertTrue { pageModel.unbox().isDirty }
        assertTrue { pageModel.unbox().fieldGroups[0].isDirty }

        assertEquals("4", pageModel.findGroup(groupId)?.get("d"))

        if (webDb.dataStore is MongoStore) {
            webDb.put(page)
            assertFalse { pageModel.unbox().isDirty }
            val fieldGroups = pageModel.unbox().fieldGroups as DirtyListWrapper

            // still dirty
            // assertFalse { fieldGroups.isDirty }
            // still dirty
            // assertFalse { pageModel.unbox().fieldGroups[0].isDirty }

            val page2 = webDb.get(baseUrl)
            val pageModel2 = page2.pageModel
            assertNotNull(pageModel2)
            assertFalse { pageModel2.unbox().isDirty }
            assertFalse { pageModel2.unbox().fieldGroups[0].isDirty }
            assertEquals("4", pageModel2.findGroup(groupId)?.get("d"))
        }
    }

    @Test
    fun testAccess() {
        val page = WebPage.newWebPage(baseUrl, conf)
        val pageModel = page.ensurePageModel()

        assertTrue { !pageModel.unbox().isDirty }
        assertNotEquals("1", pageModel.findValue(1, "a"))

        pageModel.put(1, "a", "1")
        assertTrue { pageModel.unbox().isDirty }
        assertTrue { pageModel.unbox().fieldGroups[0].isDirty }
        assertEquals("1", pageModel.findValue(1, "a"))

        pageModel.remove(1, "a")
        assertTrue { pageModel.unbox().isDirty }
        assertTrue { pageModel.unbox().fieldGroups[0].isDirty }
        assertNotEquals("1", pageModel.findValue(1, "a"))
    }
}
