package ai.platon.pulsar.persist

import ai.platon.pulsar.common.config.MutableConfig
import ai.platon.pulsar.persist.gora.generated.GWebPage

import kotlin.test.*

class TestMongoStore {
    val conf = MutableConfig()

    @Ignore
    @Test
    fun testRealSchema() {
//        val store = MongoStore<String, GWebPage>()
//        println(store.schemaName)
//
//        val provider = AutoDetectStorageProvider(conf)
//        println(provider.storeClassName)
        /**
         * Enable only if there MongoDB is running
         * */
        // assertEquals(AppConstants.MONGO_STORE_CLASS, provider.storeClassName)
    }
}
