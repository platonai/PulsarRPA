package ai.platon.pulsar.persist.mongo

import ai.platon.pulsar.common.logPrintln
import ai.platon.pulsar.common.NetUtil
import shaded.org.bson.Document
import org.junit.jupiter.api.*
import shaded.com.mongodb.client.MongoClient
import shaded.com.mongodb.client.MongoClients
import java.time.LocalDate
import kotlin.test.assertFalse

open class MongoTestBase {
    companion object {
        lateinit var mongoClient: MongoClient
        val dayOfMonth = LocalDate.now().dayOfMonth
        val databaseName = "test-$dayOfMonth"
        val collectionName = "test-collection-$dayOfMonth"

        val testDocuments = listOf(
            Document("name", "John Doe 1").append("age", 31).append("city", "New York 1"),
            Document("name", "John Doe 2").append("age", 32).append("city", "New York 2"),
            Document("name", "John Doe 3").append("age", 33).append("city", "New York 3"),
        )

        @BeforeAll
        @JvmStatic
        fun setupClass() {
            Assumptions.assumeTrue { NetUtil.testNetwork("localhost", 27017) }
            mongoClient = MongoClients.create("mongodb://localhost:27017")
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            Assumptions.assumeTrue { NetUtil.testNetwork("localhost", 27017) }
            val database = mongoClient.getDatabase(collectionName)
            database.drop()
            mongoClient.close()
        }
    }

    val database get() = mongoClient.getDatabase(databaseName)

    val collection get() = database.getCollection(collectionName)

    @BeforeEach
    fun checkMongoDBConnection() {
        // test if MongoDB is available, if not, skip the test
        try {
            val collections = mongoClient.getDatabase(databaseName).listCollectionNames()
            collections.forEach { logPrintln(it) }
        } catch (e: Exception) {
            Assumptions.assumeTrue(false, "MongoDB is not available: skip tests")
        }
    }

    @BeforeEach
    fun ensureCollectionDoesNotExist() {
        val collections = database.listCollectionNames()
        assertFalse { collections.contains(collectionName) }
    }

    @AfterEach
    fun dropCollection() {
        collection.drop()
    }
}

