package `fun`.platonic.pulsar.persist

import `fun`.platonic.pulsar.common.config.CapabilityTypes.GORA_MONGODB_SERVERS
import `fun`.platonic.pulsar.common.config.MutableConfig
import com.mongodb.BasicDBObject
import com.mongodb.MongoClient
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.test.assertNotNull

@Ignore("Should run manually, have embed mongo prerequisite")
class TestEmbedMongoDB {
    private val log = LoggerFactory.getLogger(TestEmbedMongoDB::class.java)

    val conf = MutableConfig()
    val mongoDriver = EmbedMongoDB("127.0.0.1:27017")

    @Before
    fun setup() {
        mongoDriver.start()
    }

    @After
    fun teardown() {
        mongoDriver.stop()
    }

    @Test
    fun testMongoClient() {
        println("Testing mongo client")

        val server = System.getProperty(GORA_MONGODB_SERVERS)
        val mongo = MongoClient(server)

        val db = mongo.getDB("test")
        val collection = "testCol"
        if (!db.collectionExists(collection)) {
            db.createCollection(collection, BasicDBObject())
        }
        val col = db.getCollection(collection)
        assertNotNull(col)
        col.save(BasicDBObject("testDoc", Date()))
    }
}
