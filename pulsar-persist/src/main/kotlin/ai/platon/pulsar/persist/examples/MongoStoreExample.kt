package ai.platon.pulsar.persist.examples

import ai.platon.pulsar.common.config.MutableConfig
import ai.platon.pulsar.gora.mongodb.store.MongoStore
import ai.platon.pulsar.persist.gora.generated.GWebPage

class MongoStoreExample {
    val conf = MutableConfig()

    fun run () {
        try {
            val store = MongoStore<String, GWebPage>()
            println(store.schemaName)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}

fun main() {
    MongoStoreExample().run()
}
