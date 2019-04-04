package ai.platon.pulsar.persist

import ai.platon.pulsar.common.config.CapabilityTypes.GORA_MONGODB_SERVERS
import ai.platon.pulsar.common.config.MutableConfig
import ai.platon.pulsar.common.config.PulsarConstants.DEFAULT_EMBED_MONGO_SERVER
import com.mongodb.BasicDBObject
import com.mongodb.MongoClient
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.function.BiPredicate

/**
 * Created by vincent on 19-1-19.
 * Copyright @ 2013-2019 Platon AI. All rights reserved
 */
class EmbedMongoStarter {

    private val log = LoggerFactory.getLogger(AutoDetectedStorageService::class.java)

    private var embedMongo: EmbedMongoDB? = null
    private lateinit var mongo: MongoClient

    @Synchronized
    fun start(server: String) {
        if (ai.platon.pulsar.common.RuntimeUtils.checkIfProcessRunning(".+extractmongod.+")) {
            log.info("Embed mongodb is already running")
            return
        }

        embedMongo = EmbedMongoDB(server)
        embedMongo?.start()

        // Test it's running
        try {
            mongo = MongoClient(server)
            val dbName = UUID.randomUUID().toString()
            val db = mongo.getDB(dbName)
            val collection = "testCol"
            if (!db.collectionExists(collection)) {
                db.createCollection(collection, BasicDBObject())
            }
            val col = db.getCollection(collection)
            col.save(BasicDBObject("testDoc", Date()))
            mongo.dropDatabase(dbName)

            log.info("Mongod is running")
        } catch (e: Throwable) {
            log.error("Failed to start embed mongo")
            throw e
        }
    }

    @Synchronized
    fun shutdown() {
        if (!ai.platon.pulsar.common.RuntimeUtils.checkIfProcessRunning(".+extractmongod.+")) {
            log.info("Embed mongodb is not running")
            return
        }

        try {
            // TODO: Embedded MongoDB fails to shutdown gracefully #5487
            // see https://github.com/spring-projects/spring-boot/issues/5487
            embedMongo?.stop()
        } catch (e: Throwable) {
            log.error(e.message)
        } finally {
            embedMongo = null
        }
    }

    private fun checkEmbedMongo(conf: ai.platon.pulsar.common.config.ImmutableConfig): Boolean {
        if (conf.getBoolean(ai.platon.pulsar.common.config.CapabilityTypes.STORAGE_EMBED_MONGO, false)) {
            return true
        }

        try {
            val base = Paths.get(ai.platon.pulsar.common.config.PulsarConstants.HOME_DIR, ".embedmongo")
            val paths = Files.find(base, 10, BiPredicate { path, _ -> path.endsWith("extractmongod") })
            return paths.count() > 0
        } catch (ignored: IOException) {}

        return false
    }
}

fun main(args: Array<String>) {
    val conf = MutableConfig()
    val server = conf.get(GORA_MONGODB_SERVERS, DEFAULT_EMBED_MONGO_SERVER)
    EmbedMongoStarter().start(server)

    val sc = Scanner(System.`in`)
    var i = sc.next()
}
