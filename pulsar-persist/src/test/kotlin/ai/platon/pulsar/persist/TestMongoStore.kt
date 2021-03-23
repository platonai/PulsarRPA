package ai.platon.pulsar.persist

import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.MutableConfig
import ai.platon.pulsar.persist.gora.generated.GWebPage
import org.apache.gora.mongodb.store.MongoStore
import org.junit.Test
import kotlin.test.assertEquals

class TestMongoStore {
    val conf = MutableConfig()

    @Test
    fun testRealSchema() {
        val store = MongoStore<String, GWebPage>()
        println(store.schemaName)

        val provider = AutoDetectStorageProvider(conf)
        assertEquals(AppConstants.MONGO_STORE_CLASS, provider.storeClassName)
    }
}
