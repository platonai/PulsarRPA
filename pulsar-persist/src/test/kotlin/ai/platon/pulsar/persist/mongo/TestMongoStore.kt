package ai.platon.pulsar.persist.mongo

import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.MutableConfig
import ai.platon.pulsar.persist.AutoDetectStorageProvider
import ai.platon.pulsar.persist.gora.generated.GWebPage
import org.apache.commons.lang3.RandomStringUtils
import org.apache.gora.mongodb.store.MongoStore
import kotlin.test.*

class TestMongoStore : MongoTestBase() {
    companion object {
        private val crawlId = "test_" + RandomStringUtils.randomAlphanumeric(5)
        private val conf = MutableConfig().also {
            it[CapabilityTypes.STORAGE_DATA_STORE_CLASS] = AppConstants.MONGO_STORE_CLASS
            it[CapabilityTypes.STORAGE_CRAWL_ID] = crawlId
        }
    }
    
    @Test
    fun testRealSchema() {
        val store = MongoStore<String, GWebPage>()
        assertNull(store.schemaName)
        
        val provider = AutoDetectStorageProvider(conf)
        val store2 = provider.createPageStore()
        assertEquals(AppConstants.MONGO_STORE_CLASS, provider.storeClassName)
        assertTrue("Actual schema name: ${store2.schemaName}") { crawlId in store2.schemaName }
    }
    
    @Test
    fun testOperation() {
        val store = AutoDetectStorageProvider(conf).createPageStore()
        
        val key = System.currentTimeMillis().toString()
        store.put(key, GWebPage())
        // why ?
        // assertTrue { store.exists(key) }
        store.delete(key)
        assertFalse { store.exists(key) }
    }
}
