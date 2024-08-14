package ai.platon.pulsar.persist

import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.MutableConfig
import ai.platon.pulsar.persist.gora.generated.GWebPage
import com.mongodb.MongoClient
import org.apache.commons.lang3.RandomStringUtils
import org.apache.gora.mongodb.store.MongoStore
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import kotlin.test.*

class TestMongoStore {
    companion object {
        private val crawlId = RandomStringUtils.randomAlphanumeric(18)
        private val conf = MutableConfig()
        
        private lateinit var mongoClient: MongoClient

        @BeforeAll
        @JvmStatic
        fun setupClass() {
            conf.set(CapabilityTypes.STORAGE_DATA_STORE_CLASS, AppConstants.MONGO_STORE_CLASS)
            conf.set(CapabilityTypes.STORAGE_CRAWL_ID, crawlId)
            // mongoClient = MongoClients.create("mongodb://localhost:27017");
            mongoClient = MongoClient("localhost", 27017)
        }
    }

    @BeforeEach
    fun checkMongoDBConnection() {
        // test if MongoDB is available, if not, skip the test
        try {
            mongoClient.getDatabase("test").listCollectionNames().first()
        } catch (e: Exception) {
            e.printStackTrace()
            assumeTrue(false, "MongoDB is not available: skip tests")
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
}
