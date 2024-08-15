package ai.platon.pulsar.persist.mongo

import shaded.com.mongodb.client.model.Filters
import shaded.org.bson.Document
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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
        println("Connected to the database: " + database.name)
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
                println(json)
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
        println("Document inserted")
    }
    
    @Test
    fun `when deleting a document then it should be deleted successfully`() {
        collection.deleteOne(Filters.eq("name", "John Doe"))
        println("Document deleted")
    }
}
