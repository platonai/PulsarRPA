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

import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import com.mongodb.QueryBuilder
import org.apache.commons.logging.LogFactory
import org.apache.gora.filter.*
import org.apache.gora.persistency.impl.PersistentBase
import org.apache.gora.query.Query
import org.apache.gora.query.impl.QueryBase
import org.apache.gora.store.DataStore
import org.apache.gora.util.GoraException
import org.apache.gora.util.ReflectionUtils
import org.apache.hadoop.conf.Configuration
import org.jdom.Element
import org.jdom.input.SAXBuilder
import org.slf4j.LoggerFactory
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.ArrayList

/**
 * Accepted types of data to be mapped in mongo based on BSON types
 */
enum class DocumentFieldType {
    BINARY, // bytes
    BOOLEAN, // bytes
    INT32, // 4 bytes, signed integer
    INT64, // 8 bytes, signed integer
    DOUBLE, // 8 bytes, float
    STRING, // string
    DATE, // date
    LIST, // a list
    DOCUMENT, // a subdocument
    OBJECTID // ObjectId is a 12-byte BSON type
}

/**
 * @author Fabien Poulard fpoulard@dictanova.com
 */
class MongoMapping {
    private val validMongoDocumentField = Pattern.compile("[a-z0-9\\-]+", Pattern.CASE_INSENSITIVE)
    // FIXME check name is ok
    var collectionName: String = ""

    /**
     * Mapping between the class fields and the Mongo document fields
     */
    private val classToDocument = HashMap<String, String>()

    /**
     * Mapping between the Mongo document fields and the class fields
     */
    private val documentToClass = HashMap<String, String>()

    /**
     * Mongo document description
     */
    private val documentFields = HashMap<String, DocumentFieldType>()

    /**
     * Change the name of the collection.
     *
     * @param oldName
     * previous name
     * @param newName
     * new name to be used
     */
    fun renameCollection(oldName: String, newName: String) {
        // FIXME
        collectionName = newName
    }

    /**
     * Handle the registering of a new Mongo document field. This method must
     * check that, in case the field is several document levels deep, the
     * intermediate fields exists and are of a type compatible with this new type.
     * If the parent fields do not exist, they are created.
     *
     * @param name
     * name of the field, the various levels are separated from each
     * other by dots
     * @param type
     * type of the field
     */
    private fun newDocumentField(name: String, type: DocumentFieldType) {
        // First of all, split the field to identify the various levels
        val breadcrumb = name.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        // Process each intermediate field to check they are of Map type
        val partialFieldName = StringBuilder()
        for (i in 0 until breadcrumb.size - 1) {
            // Build intermediate field name
            val f = breadcrumb[i]
            if (!isValidFieldName(f))
                throw IllegalArgumentException("'$f' is an invalid field name for a Mongo document")
            partialFieldName.append(f)
            // Check field exists or not and is of valid type
            val intermediateFieldName = partialFieldName.toString()
            if (documentFields.containsKey(intermediateFieldName)) {
                if (!listOf(DocumentFieldType.DOCUMENT, DocumentFieldType.LIST).contains(documentFields[intermediateFieldName]))
                    throw IllegalStateException("The field '" + intermediateFieldName
                            + "' is already registered in "
                            + "a type not compatible with the new definition of " + "field '"
                            + name + "'.")
            } else {
                documentFields[intermediateFieldName] = DocumentFieldType.DOCUMENT
            }
            partialFieldName.append(".")
        }
        // Check the field does not already exist, insert the complete field
        if (documentFields.containsKey(name) && documentFields[name] != type)
            throw IllegalStateException("The field '$name' is already registered with a different type.")
        documentFields[name] = type
    }

    /**
     * Check that the field matches the valid field definition for a Mongo
     * document.
     *
     * @param f
     * name of the field to be checked
     * @return true if the parameter is a valid field name for a Mongo document
     */
    private fun isValidFieldName(f: String): Boolean {
        return validMongoDocumentField.matcher(f).matches()
    }

    /**
     * Register a new mapping between a field from the persisted class to a
     * MongoDocument field.
     *
     * @param classFieldName
     * name of the field in the persisted class
     * @param docFieldName
     * name of the field in the mondo document
     * @param fieldType
     * type of the field
     */
    fun addClassField(classFieldName: String, docFieldName: String, fieldType: String) {
        try {
            // Register a new field for the mongo document
            newDocumentField(docFieldName, DocumentFieldType.valueOf(fieldType.toUpperCase(Locale.getDefault())))
        } catch (e: IllegalArgumentException) {
            throw IllegalStateException("Declared '$fieldType' for class field '$classFieldName' is not supported by MongoMapping")
        }

        // Register the mapping
        if (classToDocument.containsKey(classFieldName)) {
            if (classToDocument[classFieldName] != docFieldName) {
                val docName = classToDocument[classFieldName]
                throw IllegalStateException("The class field '$classFieldName' is already registered in the mapping with the document field '$docName which differs from the new one '$docFieldName'.")
            }
        } else {
            classToDocument[classFieldName] = docFieldName
            documentToClass[docFieldName] = classFieldName
        }
    }

    /**
     * Given a field from the persistence class, return the corresponding field in
     * the document.
     *
     * @param field
     * @return
     */
    fun getDocumentField(field: String): String {
        return classToDocument[field]!!
    }

    /**
     * Package private method to retrieve the type of a document field.
     */
    fun getDocumentFieldType(field: String): DocumentFieldType {
        return documentFields[field]!!
    }

    companion object {
        val LOG = LoggerFactory.getLogger(MongoMapping::class.java)
    }
}

/**
 * A builder for creating the mapper. This will allow building a thread safe
 * [org.apache.gora.mongodb.store.MongoMapping] using simple immutabilty.
 */
class MongoMappingBuilder<K, T : PersistentBase>(private val dataStore: MongoStore<K, T>) {
    private val LOG = LoggerFactory.getLogger(MongoMappingBuilder::class.java)!!
    private val mapping = MongoMapping()

    /**
     * Return the built mapping if it is in a legal state
     */
    fun build(): MongoMapping {
        return if (mapping.collectionName.isNotEmpty())
            mapping
        else  throw IllegalStateException("A collection is not specified")
    }

    /**
     * Load the [MongoMapping] from a file
     * passed in parameter.
     *
     * @param uri
     * path to the file holding the mapping
     * @throws java.io.IOException
     */
    @Throws(IOException::class)
    fun fromFile(uri: String) {
        try {
            val saxBuilder = SAXBuilder()
            var ism: InputStream? = javaClass.getResourceAsStream(uri)
            if (ism == null) {
                val msg = ("Unable to load the mapping from resource '$uri' as it does not appear to exist! Trying local file.")
                LOG.warn(msg)
                ism = FileInputStream(uri)
            }
            val doc = saxBuilder.build(ism)
            val root = doc.rootElement
            // No need to use documents descriptions for now...
            // Extract class descriptions
            val classElements = root.getChildren(TAG_CLASS) as List<Element>
            for (classElement in classElements) {
                val persistentClass = dataStore.persistentClass
                val keyClass = dataStore.keyClass
                if (haveKeyClass(keyClass, classElement) && havePersistentClass(persistentClass, classElement)) {
                    loadPersistentClass(classElement, persistentClass)
                    break // only need that
                }
            }
        } catch (ex: IOException) {
            LOG.error(ex.message)
            LOG.error(ex.stackTrace.toString())
            throw ex
        } catch (ex: Exception) {
            LOG.error(ex.message)
            LOG.error(ex.stackTrace.toString())
            throw IOException(ex)
        }
    }

    private fun havePersistentClass(persistentClass: Class<T>, classElement: Element): Boolean {
        return classElement.getAttributeValue(ATT_NAME) == persistentClass.name
    }

    private fun haveKeyClass(keyClass: Class<K>, classElement: Element): Boolean {
        return classElement.getAttributeValue(ATT_KEYCLASS) == keyClass.name
    }

    /**
     * Handle the XML parsing of the class definition.
     *
     * @param classElement
     * the XML node containing the class definition
     */
    protected fun loadPersistentClass(classElement: Element, pPersistentClass: Class<T>) {
        val docNameFromMapping = classElement.getAttributeValue(ATT_DOCUMENT)
        val collName = dataStore.getSchemaName(docNameFromMapping, pPersistentClass)

        mapping.collectionName = collName
        // docNameFromMapping could be null here
        if (collName != docNameFromMapping) {
            LOG.info("Keyclass and nameclass match but mismatching table names. " +
                    "Mappingfile: '$docNameFromMapping', Actual schema: '$collName', assuming they are the same.")
            if (docNameFromMapping != null) {
                mapping.renameCollection(docNameFromMapping, collName)
            }
        }

        // Process fields declaration
        val fields = classElement.getChildren(TAG_FIELD) as List<Element>
        for (field in fields) {
            mapping.addClassField(field.getAttributeValue(ATT_NAME),
                            field.getAttributeValue(ATT_FIELD),
                            field.getAttributeValue(ATT_TYPE))
        }
    }

    companion object {
        // Document description
        internal const val TAG_DOCUMENT = "document"
        internal const val ATT_COLLECTION = "collection"
        internal const val STAG_DOCUMENT_FIELD = "field"
        internal const val ATT_NAME = "name"
        internal const val ATT_TYPE = "type"
        internal const val STAG_SUBDOCUMENT = "subdocument"
        // Class description
        internal const val TAG_CLASS = "class"
        internal const val ATT_KEYCLASS = "keyClass"
        internal const val ATT_DOCUMENT = "document"
        internal const val TAG_FIELD = "field"
        internal const val ATT_FIELD = "docfield"
    }
}

/**
 * Describe factory which create remote filter for MongoDB.
 *
 * @author Damien Raude-Morvan draudemorvan@dictanova.com
 */
interface FilterFactory<K, T : PersistentBase> {
    var filterUtil: MongoFilterUtil<K, T>
    val supportedFilters: List<String>

    fun createFilter(filter: Filter<K, T>, store: MongoStore<K, T>): DBObject?
}

/**
 * Base implementation of a
 * [FilterFactory] which just manage back
 * reference to [MongoFilterUtil].
 *
 * @author Damien Raude-Morvan draudemorvan@dictanova.com
 */
abstract class BaseFactory<K, T : PersistentBase>(
        override var filterUtil: MongoFilterUtil<K, T>
): FilterFactory<K, T>

open class DefaultFactory<K, T : PersistentBase>(
        filterUtil: MongoFilterUtil<K, T>
) : BaseFactory<K, T>(filterUtil) {
    private val log = LogFactory.getLog(DefaultFactory::class.java)

    override val supportedFilters: List<String> get() {
        return listOf(SingleFieldValueFilter::class, MapFieldValueFilter::class, FilterList::class).map { it.java.canonicalName }
    }

    override fun createFilter(filter: Filter<K, T>, store: MongoStore<K, T>): DBObject? {
        return when (filter) {
            is FilterList<*, *> -> {
                transformListFilter(filter as FilterList<K, T>, store)
            }
            is SingleFieldValueFilter<*, *> -> {
                transformFieldFilter(filter as SingleFieldValueFilter<K, T>, store)
            }
            is MapFieldValueFilter<*, *> -> {
                transformMapFilter(filter as MapFieldValueFilter<K, T>, store)
            }
            else -> {
                log.warn("MongoDB remote filter not yet implemented for " + filter.javaClass.canonicalName)
                null
            }
        }
    }

    private fun transformListFilter(filterList: FilterList<K, T>, store: MongoStore<K, T>): DBObject? {
        val query = BasicDBObject()
        filterList.filters.forEach {
            if (!filterUtil.setFilter(query, it, store)) {
                return null
            }
        }
        return query
    }

    private fun transformFieldFilter(
            fieldFilter: SingleFieldValueFilter<K, T>,
            store: MongoStore<K, T>): DBObject {
        val mapping = store.mapping
        val dbFieldName = mapping.getDocumentField(fieldFilter.fieldName)

        val filterOp = fieldFilter.filterOp
        val operands = fieldFilter.operands

        var builder = QueryBuilder.start(dbFieldName)
        builder = appendToBuilder(builder, filterOp, operands)
        if (!fieldFilter.isFilterIfMissing) {
            // If false, the find query will pass if the column is not found.
            val notExist = QueryBuilder.start(dbFieldName).exists(false).get()
            builder = QueryBuilder.start().or(notExist, builder.get())
        }
        return builder.get()
    }

    private fun transformMapFilter(
            mapFilter: MapFieldValueFilter<K, T>, store: MongoStore<K, T>): DBObject {
        val mapping = store.mapping
        val dbFieldName = (mapping.getDocumentField(mapFilter.fieldName)
                + "." + store.encodeFieldKey(mapFilter.mapKey.toString()))

        val filterOp = mapFilter.filterOp
        val operands = mapFilter.operands

        var builder = QueryBuilder.start(dbFieldName)
        builder = appendToBuilder(builder, filterOp, operands)
        if (!mapFilter.isFilterIfMissing) {
            // If false, the find query will pass if the column is not found.
            val notExist = QueryBuilder.start(dbFieldName).exists(false).get()
            builder = QueryBuilder.start().or(notExist, builder.get())
        }
        return builder.get()
    }

    private fun appendToBuilder(builder: QueryBuilder, filterOp: FilterOp, rawOperands: List<Any>): QueryBuilder {
        val operands = convertOperandsToString(rawOperands)
        when (filterOp) {
            FilterOp.EQUALS -> if (operands.size == 1) {
                builder.`is`(operands.iterator().next())
            } else {
                builder.`in`(operands)
            }
            FilterOp.NOT_EQUALS -> if (operands.size == 1) {
                builder.notEquals(operands.iterator().next())
            } else {
                builder.notIn(operands)
            }
            FilterOp.LESS -> builder.lessThan(operands)
            FilterOp.LESS_OR_EQUAL -> builder.lessThanEquals(operands)
            FilterOp.GREATER -> builder.greaterThan(operands)
            FilterOp.GREATER_OR_EQUAL -> builder.greaterThanEquals(operands)
            else -> throw IllegalArgumentException("$filterOp no MongoDB equivalent yet")
        }
        return builder
    }

    /**
     * Transform all Utf8 into String before preparing MongoDB query.
     *
     * Otherwise, you'll get <tt>RuntimeException: json can't serialize type : Utf8</tt>
     *
     * @see [GORA-388](https://issues.apache.org/jira/browse/GORA-388)
     */
    private fun convertOperandsToString(rawOperands: List<Any>): List<String> {
        return rawOperands.mapTo(ArrayList(rawOperands.size)) { it.toString() }
    }
}

/**
 * Manage creation of filtering [org.apache.gora.query.Query] using
 * configured factories.
 *
 * You can use <tt>{@value #MONGO_FILTER_FACTORIES_PARAMETER}</tt> parameter to
 * change factories implementations used.
 *
 * @author Damien Raude-Morvan draudemorvan@dictanova.com
 * @see .setFilter
 */
class MongoFilterUtil<K, T : PersistentBase>(conf: Configuration) {

    private val factories = LinkedHashMap<String, FilterFactory<K, T>>()

    init {
        val factoryClassNames = conf.getStrings(MONGO_FILTER_FACTORIES_PARAMETER, MONGO_FILTERS_DEFAULT_FACTORY)

        for (factoryClass in factoryClassNames) {
            try {
                val factory = ReflectionUtils.newInstance(factoryClass) as FilterFactory<K, T>
                factory.supportedFilters.forEach {
                    factories[it] = factory
                }
                factory.filterUtil = this
            } catch (e: Exception) {
                throw GoraException(e)
            }
        }
    }

    private fun getFactory(filter: Filter<K, T>): FilterFactory<K, T>? {
        return factories[filter.javaClass.canonicalName]
    }

    /**
     * Set a filter on the <tt>query</tt>. It translates a Gora filter to a
     * MongoDB filter.
     *
     * @param query
     * The Mongo Query
     * @param filter
     * The Gora filter.
     * @param store
     * The IntrusiveMongoStore.
     * @return if remote filter is successfully applied.
     */
    fun setFilter(query: DBObject, filter: Filter<K, T>, store: MongoStore<K, T>): Boolean {
        val factory = getFactory(filter)
        if (factory == null) {
            LOG.warn("MongoDB remote filter factory not yet implemented for " + filter.javaClass.canonicalName)
            return false
        } else {
            val mongoFilter = factory.createFilter(filter, store)
            if (mongoFilter == null) {
                LOG.warn("MongoDB remote filter not yet implemented for " + filter.javaClass.canonicalName)
                return false
            } else {
                query.putAll(mongoFilter)
                return true
            }
        }
    }

    companion object {
        val MONGO_FILTERS_DEFAULT_FACTORY = "ai.platon.pulsar.persist.gora.mongodb.DefaultFactory"
        val MONGO_FILTER_FACTORIES_PARAMETER = "gora.mongodb.filter.factories"
        private val LOG = LogFactory.getLog(MongoFilterUtil::class.java)
    }
}

/**
 * MongoDB specific implementation of the [Query] interface.
 *
 * @author Fabien Poulard fpoulard@dictanova.com
 */
class MongoDBQuery<K, T : PersistentBase>(dataStore: DataStore<K, T>) : QueryBase<K, T>(dataStore) {

    companion object {
        /**
         * Compute the query itself. Only make use of the keys for querying.
         *
         * @return a [DBObject] corresponding to the query
         */
        fun toDBQuery(query: Query<*, *>): DBObject {
            val q = BasicDBObject()
            if (query.startKey != null && query.endKey != null
                    && query.startKey == query.endKey) {
                q["_id"] = query.startKey
            } else {
                if (query.startKey != null)
                    q["_id"] = BasicDBObject("\$gte", query.startKey)
                if (query.endKey != null)
                    q["_id"] = BasicDBObject("\$lte", query.endKey)
            }

            return q
        }

        /**
         * Compute the projection of the query, that is the fields that will be
         * retrieved from the database.
         *
         * @return a [DBObject] corresponding to the list of field to be
         * retrieved with the associated boolean
         */
        fun toProjection(fields: Array<String>, mapping: MongoMapping): DBObject {
            val proj = BasicDBObject()

            for (k in fields) {
                val dbFieldName = mapping.getDocumentField(k)
                if (dbFieldName.isNotEmpty()) {
                    proj[dbFieldName] = true
                }
            }

            return proj
        }
    }
}
