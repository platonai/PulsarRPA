/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.platon.pulsar.persist.gora.mongodb

import com.mongodb.*
import org.apache.avro.Schema
import org.apache.avro.generic.GenericArray
import org.apache.avro.util.Utf8
import org.apache.gora.mongodb.query.MongoDBResult
import org.apache.gora.mongodb.store.MongoStoreParameters
import org.apache.gora.mongodb.utils.BSONDecorator
import org.apache.gora.mongodb.utils.GoraDBEncoder
import org.apache.gora.persistency.Persistent
import org.apache.gora.persistency.impl.BeanFactoryImpl
import org.apache.gora.persistency.impl.DirtyListWrapper
import org.apache.gora.persistency.impl.DirtyMapWrapper
import org.apache.gora.persistency.impl.PersistentBase
import org.apache.gora.query.PartitionQuery
import org.apache.gora.query.Query
import org.apache.gora.query.Result
import org.apache.gora.query.impl.PartitionQueryImpl
import org.apache.gora.store.impl.DataStoreBase
import org.apache.gora.util.AvroUtils
import org.apache.gora.util.ClassLoadingUtils
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory

import java.io.IOException
import java.net.UnknownHostException
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.xml.bind.DatatypeConverter
import kotlin.collections.ArrayList

class MongoStore<K, T : PersistentBase>(
        val client: MongoClient
) : DataStoreBase<K, T>() {

    val LOG = LoggerFactory.getLogger(MongoStore::class.java)
    val DEFAULT_MAPPING_FILE = "/gora-mongodb-mapping.xml"
    private val mapsOfClients = ConcurrentHashMap<String, MongoClient>()
    private lateinit var mongoClientDB: DB
    private lateinit var mongoClientColl: DBCollection
    private lateinit var filterUtil: MongoFilterUtil<K, T>
    lateinit var mapping: MongoMapping

    /**
     * Initialize the data store by reading the credentials, setting the client's
     * properties up and reading the mapping file.
     */
    override fun initialize(keyClass: Class<K>, pPersistentClass: Class<T>, properties: Properties) {
        try {
            LOG.debug("Initializing MongoDB store")
            val parameters = MongoStoreParameters.load(properties, getConf())
            super.initialize(keyClass, pPersistentClass, properties)

            filterUtil = MongoFilterUtil(getConf())

            // Load the mapping
            val builder = MongoMappingBuilder(this)
            LOG.debug("Initializing Mongo store with mapping {}.", parameters.mappingFile)
            builder.fromFile(parameters.mappingFile)
            mapping = builder.build()

            // Prepare MongoDB connection
            val db = getDB(parameters)
            mongoClientDB = db
            mongoClientColl = db.getCollection(mapping.collectionName)

            LOG.info("Initialized Mongo store for database {} of {}.", parameters.dbname, parameters.servers)
        } catch (e: IOException) {
            LOG.error("Error while initializing MongoDB store: {}", e.message)
            throw RuntimeException(e)
        }
    }

    /**
     * Get reference to Mongo DB, using credentials if not null.
     */
    @Throws(UnknownHostException::class)
    private fun getDB(parameters: MongoStoreParameters): DB {
        // Get reference to Mongo DB
        mapsOfClients.computeIfAbsent(parameters.servers) { client }
        return client.getDB(parameters.dbname)
    }

    /**
     * Accessor to the name of the collection used.
     */
    override fun getSchemaName(): String? {
        return mapping.collectionName
    }

    public override fun getSchemaName(mappingSchemaName: String?, persistentClass: Class<*>): String {
        return super.getSchemaName(mappingSchemaName, persistentClass)
    }

    /**
     * Create a new collection in MongoDB if necessary.
     */
    override fun createSchema() {
        if (mongoClientDB == null)
            throw IllegalStateException(
                    "Impossible to create the schema as no database has been selected.")
        if (schemaExists()) {
            return
        }

        // If initialized create the collection
        mongoClientColl = mongoClientDB.createCollection(mapping.collectionName, BasicDBObject()) // send a DBObject to
        // force creation
        // otherwise creation is deferred
        mongoClientColl.setDBEncoderFactory(GoraDBEncoder.FACTORY)

        LOG.debug("Collection {} has been created for Mongo instance {}.", mapping.collectionName, mongoClientDB.mongo)
    }

    /**
     * Drop the collection.
     */
    override fun deleteSchema() {
        // If initialized, simply drop the collection
        mongoClientColl.drop()

        LOG.debug("Collection {} has been dropped for Mongo instance {}.", mongoClientColl.fullName, mongoClientDB.mongo)
    }

    /**
     * Check if the collection already exists or should be created.
     */
    override fun schemaExists(): Boolean {
        return mongoClientDB.collectionExists(mapping.collectionName)
    }

    /**
     * Ensure the data is synced to disk.
     */
    override fun flush() {
        for (client in mapsOfClients.values) {
            client.fsync(false)
            LOG.debug("Forced synced of database for Mongo instance {}.", client)
        }
    }

    /**
     * Release the resources linked to this collection
     */
    override fun close() {}

    /**
     * Retrieve an entry from the store with only selected fields.
     *
     * @param key
     * identifier of the document in the database
     * @param fields
     * list of fields to be loaded from the database
     */
    override fun get(key: K, fields: Array<String>): T? {
        val dbFields = getFieldsToQuery(fields)
        // Prepare the MongoDB query
        val q = BasicDBObject("_id", key)
        val proj = BasicDBObject()
        for (field in dbFields) {
            val docf = mapping.getDocumentField(field)
            if (docf != null) {
                proj[docf] = true
            }
        }
        // Execute the query
        val res = mongoClientColl.findOne(q, proj)
        // Build the corresponding persistent
        return newInstance(res, dbFields)
    }

    /**
     * Persist an object into the store.
     *
     * @param key
     * identifier of the object in the store
     * @param obj
     * the object to be inserted
     */
    override fun put(key: K, obj: T) {
        // Save the object in the database
        if (obj.isDirty) {
            performPut(key, obj)
        } else {
            LOG.info("Ignored putting object {} in the store as it is neither " + "new, neither dirty.", obj)
        }
    }

    /**
     * Update a object that already exists in the store. The object must exist
     * already or the update may fail.
     *
     * @param key
     * identifier of the object in the store
     * @param obj
     * the object to be inserted
     */
    private fun performPut(key: K, obj: T) {
        // Build the query to select the object to be updated
        val qSel = BasicDBObject("_id", key)

        // Build the update query
        val qUpdate = BasicDBObject()

        val qUpdateSet = newUpdateSetInstance(obj)
        if (qUpdateSet.size > 0) {
            qUpdate["\$set"] = qUpdateSet
        }

        val qUpdateUnset = newUpdateUnsetInstance(obj)
        if (qUpdateUnset.size > 0) {
            qUpdate["\$unset"] = qUpdateUnset
        }

        // Execute the update (if there is at least one $set ot $unset
        if (!qUpdate.isEmpty()) {
            mongoClientColl.update(qSel, qUpdate, true, false)
            obj.clearDirty()
        } else {
            LOG.debug("No update to perform, skip {}", key)
        }
    }

    override fun delete(key: K): Boolean {
        val removeKey = BasicDBObject("_id", key)
        val writeResult = mongoClientColl.remove(removeKey)
        return writeResult != null && writeResult.n > 0
    }

    override fun deleteByQuery(query: Query<K, T>): Long {
        // Build the actual MongoDB query
        val q = MongoDBQuery.toDBQuery(query)
        val writeResult = mongoClientColl.remove(q)
        return if (writeResult != null) {
            writeResult.n.toLong()
        } else 0
    }

    /**
     * Execute the query and return the result.
     */
    override fun execute(query: Query<K, T>): Result<K, T> {

        val fields = getFieldsToQuery(query.fields)
        // Build the actual MongoDB query
        val q = MongoDBQuery.toDBQuery(query)
        val p = MongoDBQuery.toProjection(fields, mapping)

        if (query.filter != null) {
            val succeeded = filterUtil.setFilter(q, query.filter, this)
            if (succeeded) {
                // don't need local filter
                query.isLocalFilterEnabled = false
            }
        }

        // Execute the query on the collection
        var cursor = mongoClientColl.find(q, p)
        if (query.limit > 0)
            cursor = cursor.limit(query.limit.toInt())
        cursor.batchSize(100)
        cursor.addOption(Bytes.QUERYOPTION_NOTIMEOUT)

        // Build the result
        val mongoResult = MongoDBResult(this, query)
        mongoResult.setCursor(cursor)

        return mongoResult
    }

    /**
     * Create a new [Query] to query the datastore.
     */
    override fun newQuery(): Query<K, T> {
        val query = MongoDBQuery(this)
        query.setFields(*getFieldsToQuery(null))
        return query
    }

    /**
     * Partitions the given query and returns a list of PartitionQuerys, which
     * will execute on local data.
     */
    @Throws(IOException::class)
    override fun getPartitions(query: Query<K, T>): List<PartitionQuery<K, T>> {
        // FIXME: for now, there is only one partition as we do not handle
        // MongoDB sharding configuration
        val partitions = ArrayList<PartitionQuery<K, T>>()
        val partitionQuery = PartitionQueryImpl(
                query)
        partitionQuery.conf = getConf()
        partitions.add(partitionQuery)
        return partitions
    }

    // //////////////////////////////////////////////////////// DESERIALIZATION

    /**
     * Build a new instance of the persisted class from the [DBObject]
     * retrieved from the database.
     *
     * @param obj
     * the [DBObject] that results from the query to the database
     * @param fields
     * the list of fields to be mapped to the persistence class instance
     * @return a persistence class instance which content was deserialized from
     * the [DBObject]
     */
    fun newInstance(obj: DBObject?, fields: Array<String>): T? {
        if (obj == null)
            return null
        val easybson = BSONDecorator(obj)
        // Create new empty persistent bean instance
        val persistent = newPersistent()
        val dbFields = getFieldsToQuery(fields)

        // Populate each field
        for (f in dbFields) {
            // Check the field exists in the mapping and in the db
            val docf = mapping.getDocumentField(f)
            if (docf == null || !easybson.containsField(docf))
                continue

            val storeType = mapping.getDocumentFieldType(docf)!!
            val field = fieldMap[f]!!
            val fieldSchema = field.schema()

            LOG.debug(
                    "Load from DBObject (MAIN), field:{}, schemaType:{}, docField:{}, storeType:{}",
                    field.name(), fieldSchema.type, docf, storeType)
            val result = fromDBObject(fieldSchema, storeType, field, docf,
                    easybson)
            persistent.put(field.pos(), result)
        }
        persistent.clearDirty()
        return persistent
    }

    private fun fromDBObject(fieldSchema: Schema,
                             storeType: DocumentFieldType, field: Schema.Field, docf: String,
                             easybson: BSONDecorator): Any? {
        var result: Any? = null
        when (fieldSchema.type) {
            Schema.Type.MAP -> result = fromMongoMap(docf, fieldSchema, easybson, field)
            Schema.Type.ARRAY -> result = fromMongoList(docf, fieldSchema, easybson, field)
            Schema.Type.RECORD -> {
                val rec = easybson.getDBObject(docf)
                if (rec == null) result = null else result = fromMongoRecord(fieldSchema, docf, rec)
            }
            Schema.Type.BOOLEAN -> result = easybson.getBoolean(docf)
            Schema.Type.DOUBLE -> result = easybson.getDouble(docf)
            Schema.Type.FLOAT -> result = easybson.getDouble(docf)!!.toFloat()
            Schema.Type.INT -> result = easybson.getInt(docf)
            Schema.Type.LONG -> result = easybson.getLong(docf)
            Schema.Type.STRING -> result = fromMongoString(storeType, docf, easybson)
            Schema.Type.ENUM -> result = AvroUtils.getEnumValue(fieldSchema, easybson.getUtf8String(docf)
                    .toString())
            Schema.Type.BYTES, Schema.Type.FIXED -> result = easybson.getBytes(docf)
            Schema.Type.NULL -> result = null
            Schema.Type.UNION -> result = fromMongoUnion(fieldSchema, storeType, field, docf, easybson)
            else -> LOG.warn("Unable to read {}", docf)
        }
        return result
    }

    private fun fromMongoUnion(fieldSchema: Schema,
                               storeType: DocumentFieldType, field: Schema.Field, docf: String,
                               easybson: BSONDecorator): Any? {
        val result: Any?// schema [type0, type1]
        val type0 = fieldSchema.types[0].type
        val type1 = fieldSchema.types[1].type

        // Check if types are different and there's a "null", like ["null","type"]
        // or ["type","null"]
        if (type0 != type1 && (type0 == Schema.Type.NULL || type1 == Schema.Type.NULL)) {
            val innerSchema = fieldSchema.types[1]
            LOG.debug("Load from DBObject (UNION), schemaType:{}, docField:{}, storeType:{}",
                    innerSchema.type, docf, storeType)
            // Deserialize as if schema was ["type"]
            result = fromDBObject(innerSchema, storeType, field, docf, easybson)
        } else {
            throw IllegalStateException("MongoStore doesn't support 3 types union field yet. Please update your mapping")
        }
        return result
    }

    private fun fromMongoRecord(fieldSchema: Schema, docf: String,
                                rec: DBObject?): Any {
        val result: Any
        val innerBson = BSONDecorator(rec)
        val clazz: Class<out Persistent>
        try {
            clazz = ClassLoadingUtils.loadClass(fieldSchema.fullName) as Class<out Persistent>
        } catch (e: ClassNotFoundException) {
            throw e
        }

        val record = BeanFactoryImpl(keyClass, clazz).newPersistent() as PersistentBase
        for (recField in fieldSchema.fields) {
            val innerSchema = recField.schema()
            val innerStoreType = mapping.getDocumentFieldType(innerSchema.name)!!
            val innerDocField = if (mapping.getDocumentField(recField.name()) != null)
                mapping.getDocumentField(recField.name())!!
            else
                recField.name()
            val fieldPath = "$docf.$innerDocField"
            LOG.debug("Load from DBObject (RECORD), field:{}, schemaType:{}, docField:{}, storeType:{}",
                    recField.name(), innerSchema.type, fieldPath, innerStoreType)
            record.put(recField.pos(), fromDBObject(innerSchema, innerStoreType, recField, innerDocField, innerBson))
        }
        result = record
        return result
    }

    private fun fromMongoList(docf: String, fieldSchema: Schema, easybson: BSONDecorator, f: Schema.Field): DirtyListWrapper<Any> {
        val list = easybson.getDBList(docf) ?: return DirtyListWrapper(arrayListOf())

        val rlist = ArrayList<Any>()
        for (item in list) {
            val storeType = mapping.getDocumentFieldType(docf)!!
            val o = fromDBObject(fieldSchema.elementType, storeType, f, "item", BSONDecorator(BasicDBObject("item", item)))!!
            rlist.add(o)
        }
        return DirtyListWrapper(rlist)
    }

    private fun fromMongoMap(docf: String, fieldSchema: Schema, easybson: BSONDecorator, f: Schema.Field): DirtyMapWrapper<Utf8, Any> {
        val map = easybson.getDBObject(docf)?:return DirtyMapWrapper(mapOf())

        val rmap = mutableMapOf<Utf8, Any>()
        for (e in map.entries) {
            val mapKey = e.key
            val decodedMapKey = decodeFieldKey(mapKey)!!

            val storeType = mapping.getDocumentFieldType(docf)!!
            val o = fromDBObject(fieldSchema.valueType, storeType, f, mapKey, BSONDecorator(map))!!
            rmap[Utf8(decodedMapKey)] = o
        }
        return DirtyMapWrapper(rmap)
    }

    private fun fromMongoString(storeType: DocumentFieldType, docf: String, easybson: BSONDecorator): Any {
        val result: Any
        if (storeType == DocumentFieldType.OBJECTID) {
            // Try auto-conversion of BSON data to ObjectId
            // It will work if data is stored as String or as ObjectId
            val bin = easybson.get(docf)
            if (bin is String) {
                val id = ObjectId(bin)
                result = Utf8(id.toString())
            } else {
                result = Utf8(bin.toString())
            }
        } else if (storeType == DocumentFieldType.DATE) {
            val bin = easybson.get(docf)
            if (bin is Date) {
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.getDefault())
                calendar.time = bin as Date
                result = Utf8(DatatypeConverter.printDateTime(calendar))
            } else {
                result = Utf8(bin.toString())
            }
        } else {
            result = easybson.getUtf8String(docf)
        }
        return result
    }

    // ////////////////////////////////////////////////////////// SERIALIZATION

    /**
     * Build a new instance of [DBObject] from the persistence class
     * instance in parameter. Limit the [DBObject] to the fields that are
     * dirty and not null, that is the fields that will need to be updated in the
     * store.
     *
     * @param persistent
     * a persistence class instance which content is to be serialized as
     * a [DBObject] for use as parameter of a $set operator
     * @return a [DBObject] which content corresponds to the fields that
     * have to be updated... and formatted to be passed in parameter of a
     * $set operator
     */
    private fun newUpdateSetInstance(persistent: T): BasicDBObject {
        val result = BasicDBObject()

        for (f in persistent.schema.fields) {
            if (persistent.isDirty(f.pos()) && persistent.get(f.pos()) != null) {
                val docf = mapping.getDocumentField(f.name())!!
                val value = persistent.get(f.pos())
                val storeType = mapping.getDocumentFieldType(docf)!!
                LOG.debug("Transform value to DBObject (MAIN), docField:{}, schemaType:{}, storeType:{}", docf, f.schema().type, storeType)
                val o = toDBObject(docf, f.schema(), f.schema().type, storeType, value)
                result[docf] = o
            }
        }

        return result
    }

    /**
     * Build a new instance of [DBObject] from the persistence class
     * instance in parameter. Limit the [DBObject] to the fields that are
     * dirty and null, that is the fields that will need to be updated in the
     * store by being removed.
     *
     * @param persistent
     * a persistence class instance which content is to be serialized as
     * a [DBObject] for use as parameter of a $set operator
     * @return a [DBObject] which content corresponds to the fields that
     * have to be updated... and formated to be passed in parameter of a
     * $unset operator
     */
    private fun newUpdateUnsetInstance(persistent: T): BasicDBObject {
        val result = BasicDBObject()
        for (f in persistent.schema.fields) {
            if (persistent.isDirty(f.pos()) && persistent.get(f.pos()) == null) {
                val docf = mapping.getDocumentField(f.name())
                val value = persistent.get(f.pos())
                val storeType = mapping.getDocumentFieldType(docf)
                LOG.debug(
                        "Transform value to DBObject (MAIN), docField:{}, schemaType:{}, storeType:{}",
                        *arrayOf<Any>(docf, f.schema().type, storeType))
                val o = toDBObject(docf, f.schema(), f.schema().type,
                        storeType, value)
                result[docf] = o
            }
        }
        return result
    }

    private fun toDBObject(
            docf: String, fieldSchema: Schema,
            fieldType: Schema.Type, storeType: DocumentFieldType?,
            value: Any?): Any? {
        var result: Any? = null
        when (fieldType) {
            Schema.Type.MAP -> {
                if (storeType != null && storeType != DocumentFieldType.DOCUMENT) {
                    val type = fieldSchema.type
                    throw IllegalStateException("Field $type: to store a Gora 'map', target Mongo mapping have to be of 'document' type")
                }
                val valueSchema = fieldSchema.valueType
                result = mapToMongo(docf, value as Map<CharSequence, *>?, valueSchema, valueSchema.type)
            }
            Schema.Type.ARRAY -> {
                if (storeType != null && storeType != DocumentFieldType.LIST) {
                    throw IllegalStateException(
                            ("Field "
                                    + fieldSchema.type
                                    + ": To store a Gora 'array', target Mongo mapping have to be of 'list' type"))
                }
                val elementSchema = fieldSchema.elementType
                result = listToMongo(docf, value as List<*>?, elementSchema,
                        elementSchema.type)
            }
            Schema.Type.BYTES -> if (value != null) {
                result = (value as ByteBuffer).array()
            }
            Schema.Type.INT, Schema.Type.LONG, Schema.Type.FLOAT, Schema.Type.DOUBLE, Schema.Type.BOOLEAN -> result = value
            Schema.Type.STRING -> result = stringToMongo(fieldSchema, storeType, value)
            Schema.Type.ENUM -> if (value != null)
                result = value.toString()
            Schema.Type.RECORD -> {
                if (value != null) result = recordToMongo(docf, fieldSchema, value)
            }
            Schema.Type.UNION -> result = unionToMongo(docf, fieldSchema, storeType, value)
            Schema.Type.FIXED -> result = value

            else -> LOG.error("Unknown field type: {}", fieldSchema.type)
        }

        return result
    }

    private fun unionToMongo(docf: String, fieldSchema: Schema,
                             storeType: DocumentFieldType?, value: Any?): Any? {
        val result: Any?// schema [type0, type1]
        val type0 = fieldSchema.types[0].type
        val type1 = fieldSchema.types[1].type

        // Check if types are different and there's a "null", like ["null","type"]
        // or ["type","null"]
        if ((type0 != type1 && (type0 == Schema.Type.NULL || type1 == Schema.Type.NULL))) {
            val innerSchema = fieldSchema.types[1]
            LOG.debug("Transform value to DBObject (UNION), schemaType:{}, type1:{}, storeType:{}",
                    innerSchema.type, type1, storeType)
            // Deserialize as if schema was ["type"]
            result = toDBObject(docf, innerSchema, type1, storeType, value)
        } else {
            throw IllegalStateException(
                    "MongoStore doesn't support 3 types union field yet. Please update your mapping")
        }
        return result
    }

    private fun recordToMongo(docf: String,
                              fieldSchema: Schema, value: Any): BasicDBObject {
        val record = BasicDBObject()
        for (member in fieldSchema.fields) {
            val innerValue = (value as PersistentBase).get(member.pos())
            val innerDoc = mapping.getDocumentField(member.name())
            val innerType = member.schema().type
            val innerStoreType = mapping.getDocumentFieldType(innerDoc)
            LOG.debug(
                    "Transform value to DBObject (RECORD), docField:{}, schemaType:{}, storeType:{}",
                    *arrayOf<Any>(member.name(), member.schema().type, innerStoreType))
            record[member.name()] = toDBObject(docf, member.schema(), innerType, innerStoreType,
                    innerValue)
        }
        return record
    }

    private fun stringToMongo(fieldSchema: Schema, storeType: DocumentFieldType?, value: Any?): Any? {
        var result: Any? = null
        if (storeType == DocumentFieldType.OBJECTID) {
            if (value != null) {
                val id: ObjectId
                try {
                    id = ObjectId(value.toString())
                } catch (e1: IllegalArgumentException) {
                    // Unable to parse anything from Utf8 value, throw error
                    throw IllegalStateException(("Field " + fieldSchema.type
                            + ": Invalid string: unable to convert to ObjectId"))
                }

                result = id
            }
        } else if (storeType == DocumentFieldType.DATE) {
            if (value != null) {
                // Try to parse date from Utf8 value
                var calendar: Calendar? = null
                try {
                    // Parse as date + time
                    calendar = DatatypeConverter.parseDateTime(value.toString())
                } catch (e1: IllegalArgumentException) {
                    try {
                        // Parse as date only
                        calendar = DatatypeConverter.parseDate(value.toString())
                    } catch (e2: IllegalArgumentException) {
                        // No-op
                    }

                }

                if (calendar == null) {
                    // Unable to parse anything from Utf8 value, throw error
                    throw IllegalStateException(("Field " + fieldSchema.type + ": Invalid date format '" + value + "'"))
                }
                result = calendar.time
            }
        } else {
            if (value != null) {
                result = value.toString()
            }
        }
        return result
    }

    /**
     * Convert a Java Map as used in Gora generated classes to a Map that can
     * safely be serialized into MongoDB.
     *
     * @param value
     * the Java Map that must be serialized into a MongoDB object
     * @param fieldType
     * type of the values within the map
     * @return a [BasicDBObject] version of the [Map] that can be
     * safely serialized into MongoDB.
     */
    private fun mapToMongo(docf: String,
                           value: Map<CharSequence, *>?, fieldSchema: Schema,
                           fieldType: Schema.Type): BasicDBObject {
        val map = BasicDBObject()
        // Handle null case
        if (value == null)
            return map

        // Handle regular cases
        for (e in value!!.entries) {
            val mapKey = e.key.toString()
            val encodedMapKey = encodeFieldKey(mapKey)
            val mapValue = e.value

            val storeType = mapping.getDocumentFieldType(docf)
            val result = toDBObject(docf, fieldSchema, fieldType, storeType,
                    mapValue)
            map[encodedMapKey] = result
        }

        return map
    }

    /**
     * Convert a Java [GenericArray] as used in Gora generated classes to a
     * List that can safely be serialized into MongoDB.
     *
     * @param array
     * the [GenericArray] to be serialized
     * @param fieldType
     * type of the elements within the array
     * @return a [BasicDBList] version of the [GenericArray] that can
     * be safely serialized into MongoDB.
     */
    private fun listToMongo(docf: String, array: Collection<*>?,
                            fieldSchema: Schema, fieldType: Schema.Type): BasicDBList {
        val list = BasicDBList()
        // Handle null case
        if (array == null)
            return list

        // Handle regular cases
        for (item in array!!) {
            val storeType = mapping.getDocumentFieldType(docf)
            val result = toDBObject(docf, fieldSchema, fieldType, storeType, item)
            list.add(result)
        }

        return list
    }

    // //////////////////////////////////////////////////////// CLEANUP

    /**
     * Ensure Key encoding -&gt; dots replaced with middle dots
     *
     * @param key
     * char with only dots.
     * @return encoded string with "\u00B7" chars..
     */
    fun encodeFieldKey(key: String?): String? {
        return if (key == null) {
            null
        } else key!!.replace(".", "\u00B7")
    }

    /**
     * Ensure Key decoding -&gt; middle dots replaced with dots
     *
     * @param key
     * encoded string with "\u00B7" chars.
     * @return Cleanup up char with only dots.
     */
    fun decodeFieldKey(key: String?): String? {
        return if (key == null) {
            null
        } else key!!.replace("\u00B7", ".")
    }
}
