package ai.platon.pulsar.persist.mongo

import ai.platon.pulsar.common.printlnPro
import shaded.com.mongodb.client.model.Filters
import shaded.org.bson.Document
import kotlin.test.*

/**
 * The driver com.mongodb.MongoClient is a Legacy Driver.
 * Introduced In: MongoDB Java driver 3.x and earlier.
 *
 * The driver com.mongodb.client.MongoClient is a New Driver.
 * Introduced in: MongoDB Java Driver 3.7 and later.
 * */
class MongoClientNewTest : MongoTestBase() {

    @Test
    fun testGetCollectionName() {
        // Use the database (for example, get a collection)
        printlnPro("Connected to the database: " + database.name)
        assertNotNull(database)
        assertEquals(databaseName, database.name)
    }

    @Test
    fun `when querying a document using cursor then it works`() {
        val document: Document = Document("name", "John Doe")
            .append("age", 30)
            .append("city", "New York")

        collection.insertOne(document)

        val cursor = collection.find().iterator()

        cursor.use {
            while (it.hasNext()) {
                val json = it.next().toJson()
                printlnPro(json)
                assertTrue { json.contains("New York") }
            }
        }
    }

    @Test
    fun `when inserting a document into MongoDB then it should be inserted successfully`() {
        val document: Document = Document("name", "John Doe")
            .append("age", 30)
            .append("city", "New York")

        collection.insertOne(document)
        printlnPro("Document inserted")
    }

    @Test
    fun `when deleting a document then it should be deleted successfully`() {
        collection.deleteOne(Filters.eq("name", "John Doe"))
        printlnPro("Document deleted")
    }
}
