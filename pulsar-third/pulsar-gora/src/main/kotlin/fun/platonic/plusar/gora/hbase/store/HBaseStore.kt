package `fun`.platonic.plusar.gora.hbase.store

import org.apache.avro.Schema
import org.apache.avro.util.Utf8
import org.apache.gora.persistency.impl.DirtyListWrapper
import org.apache.gora.persistency.impl.DirtyMapWrapper
import org.apache.gora.persistency.impl.PersistentBase
import org.apache.gora.query.PartitionQuery
import org.apache.gora.query.Query
import org.apache.gora.query.impl.PartitionQueryImpl
import org.apache.gora.store.DataStoreFactory
import org.apache.gora.store.impl.DataStoreBase
import org.apache.gora.util.ByteUtils.toBytes
import org.apache.hadoop.conf.Configurable
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.*
import org.apache.hadoop.hbase.client.*
import org.apache.hadoop.hbase.io.compress.Compression
import org.apache.hadoop.hbase.regionserver.BloomType
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase.util.Pair
import org.jdom.Element
import org.jdom.input.SAXBuilder
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import java.io.FileNotFoundException
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.net.URLClassLoader
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.LinkedBlockingQueue
import javax.naming.ConfigurationException
import kotlin.collections.HashMap

/**
 * Thread safe implementation to connect to a HBase table.
 *
 */
class HBaseTableConnection(
        val configuration: Configuration,
        tableName: String,
        private val autoFlush: Boolean
) {
    private val connection: Connection = ConnectionFactory.createConnection(configuration)
    private val regionLocator: RegionLocator
    // BufferedMutator used for doing async flush i.e. autoflush = false
    private val buffers: ThreadLocal<ConcurrentLinkedQueue<Mutation>> = ThreadLocal()
    private val tables: ThreadLocal<Table> = ThreadLocal()

    private val tPool = LinkedBlockingQueue<Table>()
    private val bPool = LinkedBlockingQueue<ConcurrentLinkedQueue<Mutation>>()
    val name: TableName = TableName.valueOf(tableName)

    private val table: Table
        get() {
            var table: Table? = tables.get()
            if (table == null) {
                table = connection.getTable(name)
                tPool.add(table!!)
                tables.set(table)
            }
            return table
        }

    private val buffer: ConcurrentLinkedQueue<Mutation>
        get() {
            var buffer: ConcurrentLinkedQueue<Mutation>? = buffers.get()
            if (buffer == null) {
                buffer = ConcurrentLinkedQueue()
                bPool.add(buffer)
                buffers.set(buffer)
            }
            return buffer
        }

    /**
     * getStartEndKeys provided by [HRegionLocation].
     * @see RegionLocator.getStartEndKeys
     */
    val startEndKeys: Pair<Array<ByteArray>, Array<ByteArray>> get() = regionLocator.startEndKeys

    init {
        this.regionLocator = this.connection.getRegionLocator(this.name)
    }

    fun flushCommits() {
        val bufMutator = connection.getBufferedMutator(this.name)
        for (buffer in bPool) {
            while (!buffer.isEmpty()) {
                val m = buffer.poll()
                bufMutator.mutate(m)
            }
        }
        bufMutator.flush()
        bufMutator.close()
    }

    fun close() {
        // Flush and close all instances.
        // (As an extra safeguard one might employ a shared variable i.e. 'closed'
        //  in order to prevent further table creation but for now we assume that
        //  once close() is called, clients are no longer using it).
        flushCommits()

        for (table in tPool) {
            table.close()
        }
    }

    /**
     * getRegionLocation provided by [HRegionLocation]
     * @see RegionLocator.getRegionLocation
     */
    fun getRegionLocation(bs: ByteArray): HRegionLocation {
        return regionLocator.getRegionLocation(bs)
    }

    fun exists(get: Get): Boolean {
        return table.exists(get)
    }

    fun existsAll(list: List<Get>): BooleanArray {
        return table.existsAll(list)
    }

    operator fun get(get: Get): Result {
        return table.get(get)
    }

    operator fun get(gets: List<Get>): Array<Result> {
        return table.get(gets)
    }

    fun getScanner(scan: Scan): ResultScanner {
        return table.getScanner(scan)
    }

    fun put(put: Put) {
        buffer.add(put)
    }

    fun put(puts: List<Put>) {
        buffer.addAll(puts)
    }

    fun delete(delete: Delete) {
        buffer.add(delete)
    }

    fun delete(deletes: List<Delete>) {
        buffer.addAll(deletes)
    }
}

class HBaseColumn(val family: ByteArray, val qualifier: ByteArray? = null) {

    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + Arrays.hashCode(family)
        result = prime * result + Arrays.hashCode(qualifier)
        return result
    }

    override fun equals(obj: Any?): Boolean {
        if (obj == null) return false
        if (this === obj) return true
        if (javaClass != obj.javaClass) return false
        val other = obj as HBaseColumn
        return Arrays.equals(family, other.family) && Arrays.equals(qualifier, other.qualifier)
    }

    override fun toString(): String {
        return ("HBaseColumn [family=" + Arrays.toString(family) + ", qualifier="
                + Arrays.toString(qualifier) + "]")
    }
}

class HBaseMapping(val table: HTableDescriptor, private val columnMap: Map<String, HBaseColumn>) {
    val tableName: String get() = table.nameAsString

    fun getColumn(fieldName: String): HBaseColumn? {
        return columnMap[fieldName]
    }

    /**
     * A builder for creating the mapper. This will allow building a thread safe
     * [HBaseMapping] using simple immutability.
     *
     */
    class HBaseMappingBuilder {
        private val tableToFamilies = HashMap<String, MutableMap<String, HColumnDescriptor>>()
        private val columnMap = HashMap<String, HBaseColumn>()

        var tableName: TableName = TableName.valueOf("")

        fun addFamilyProps(tableName: String, familyName: String,
                           compression: String?, blockCache: String?, blockSize: String?,
                           bloomFilter: String?, maxVersions: String?, timeToLive: String?,
                           inMemory: String?) {

            // We keep track of all tables, because even though we
            // only build a mapping for one table. We do this because of the way
            // the mapping file is set up. 
            // (First family properties are defined, whereafter columns are defined).
            //
            // HBaseMapping in fact does not need to support multiple tables,
            // because a Store itself only supports a single table. (Every store 
            // instance simply creates one mapping instance for itself).
            //
            // TODO A nice solution would be to redefine the mapping file structure.
            // For example nest columns in families. Of course this would break compatibility.


            val families = getOrCreateFamilies(tableName)


            val columnDescriptor = getOrCreateFamily(familyName, families)

            if (compression != null)
                columnDescriptor.compressionType = Compression.Algorithm.valueOf(compression)
            if (blockCache != null)
                columnDescriptor.isBlockCacheEnabled = java.lang.Boolean.parseBoolean(blockCache)
            if (blockSize != null)
                columnDescriptor.blocksize = Integer.parseInt(blockSize)
            if (bloomFilter != null)
                columnDescriptor.bloomFilterType = BloomType.valueOf(bloomFilter)
            if (maxVersions != null)
                columnDescriptor.maxVersions = Integer.parseInt(maxVersions)
            if (timeToLive != null)
                columnDescriptor.timeToLive = Integer.parseInt(timeToLive)
            if (inMemory != null)
                columnDescriptor.isInMemory = java.lang.Boolean.parseBoolean(inMemory)
        }

        fun addColumnFamily(tableName: String, familyName: String) {
            val families = getOrCreateFamilies(tableName)
            getOrCreateFamily(familyName, families)
        }

        fun addField(fieldName: String, family: String, qualifier: String?) {
            val familyBytes = Bytes.toBytes(family)
            val qualifierBytes = if (qualifier == null)
                null
            else
                Bytes.toBytes(qualifier)

            val column = HBaseColumn(familyBytes, qualifierBytes)
            columnMap[fieldName] = column
        }


        private fun getOrCreateFamily(familyName: String,
                                      families: MutableMap<String, HColumnDescriptor>): HColumnDescriptor {
            var columnDescriptor: HColumnDescriptor? = families[familyName]
            if (columnDescriptor == null) {
                columnDescriptor = HColumnDescriptor(familyName)
                families[familyName] = columnDescriptor
            }
            return columnDescriptor
        }

        private fun getOrCreateFamilies(tableName: String): MutableMap<String, HColumnDescriptor> {
            return tableToFamilies.computeIfAbsent(tableName) { HashMap() }
        }

        fun renameTable(oldName: String, newName: String) {
            val families = tableToFamilies.remove(oldName) ?: throw IllegalArgumentException("$oldName does not exist")
            tableToFamilies[newName] = families
        }

        /**
         * @return A newly constructed mapping.
         */
        fun build(): HBaseMapping {
            val families = tableToFamilies[tableName.nameAsString]
                    ?: throw IllegalStateException("no families for table " + tableName)

            val tableDescriptors = HTableDescriptor(tableName)
            for (desc in families.values) {
                tableDescriptors.addFamily(desc)
            }
            return HBaseMapping(tableDescriptors, columnMap)
        }
    }
}


/**
 * DataStore for HBase. Thread safe.
 *
 */
/**
 * Default constructor
 */
class HBaseStore<K, T : PersistentBase> : DataStoreBase<K, T>(), Configurable {

    @Volatile
    private lateinit var admin: Admin

    @Volatile
    private lateinit var table: HBaseTableConnection

    @Volatile
    lateinit var mapping: HBaseMapping

    private lateinit var filterUtil: HBaseFilterUtil<K, T>

    private var scannerCaching = SCANNER_CACHING_PROPERTIES_DEFAULT

    /**
     * Initialize the data store by reading the credentials, setting the client's properties up and
     * reading the mapping file. Initialize is called when then the call to
     * [org.apache.gora.store.DataStoreFactory.createDataStore] is made.
     *
     * @param keyClass
     * @param persistentClass
     * @param properties
     */
    override fun initialize(keyClass: Class<K>, persistentClass: Class<T>,
                            properties: Properties) {
        try {
            super.initialize(keyClass, persistentClass, properties)

            this.conf = HBaseConfiguration.create(getConf())
            admin = ConnectionFactory.createConnection(getConf()).admin
            mapping = readMapping(getConf().get(PARSE_MAPPING_FILE_KEY, DEFAULT_MAPPING_FILE))
            filterUtil = HBaseFilterUtil<K, T>(this.conf)
        } catch (ex: FileNotFoundException) {
            LOG.error("{}  is not found, please check the file.", DEFAULT_MAPPING_FILE)
            throw RuntimeException(ex)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

        // Set scanner caching option
        try {
            this.setScannerCaching(
                    Integer.valueOf(DataStoreFactory.findProperty(this.properties, this,
                            SCANNER_CACHING_PROPERTIES_KEY,
                            SCANNER_CACHING_PROPERTIES_DEFAULT.toString())))
        } catch (e: Exception) {
            LOG.error("Can not load {} from gora.properties. Setting to default value: {}.", SCANNER_CACHING_PROPERTIES_KEY, SCANNER_CACHING_PROPERTIES_DEFAULT)
            this.setScannerCaching(SCANNER_CACHING_PROPERTIES_DEFAULT) // Default value if something is wrong
        }

        if (autoCreateSchema) {
            createSchema()
        }

        try {
            val autoflush = this.conf.getBoolean("hbase.client.autoflush.default", false)
            table = HBaseTableConnection(getConf(), schemaName, autoflush)
        } catch (ex2: IOException) {
            LOG.error(ex2.message, ex2)
        }

        closeHBaseAdmin()
    }

    override fun getSchemaName(): String {
        //return the name of this table
        return mapping.tableName
    }

    override fun createSchema() {
        try {
            if (schemaExists()) {
                return
            }
            val tableDesc = mapping.table

            admin.createTable(tableDesc)
        } catch (ex2: IOException) {
            LOG.error(ex2.message, ex2)
        }

        closeHBaseAdmin()
    }

    override fun deleteSchema() {
        try {
            if (!schemaExists()) {
                return
            }
            admin.disableTable(mapping.table.tableName)
            admin.deleteTable(mapping.table.tableName)
        } catch (ex2: IOException) {
            LOG.error(ex2.message, ex2)
        }

        closeHBaseAdmin()
    }

    override fun schemaExists(): Boolean {
        try {
            return admin.tableExists(mapping.table.tableName)
        } catch (ex2: IOException) {
            LOG.error(ex2.message, ex2)
            return false
        }

    }

    override fun get(key: K, fields: Array<String>): T? {
        var fields = fields
        try {
            fields = getFieldsToQuery(fields)
            val get = Get(HBaseByteInterface.toBytes(key))
            addFields(get, fields)
            val result = table.get(get)
            return newInstance(result, fields)
        } catch (ex2: IOException) {
            LOG.error(ex2.message, ex2)
            return null
        }
    }

    /**
     * {@inheritDoc} Serializes the Persistent data and saves in HBase. Topmost
     * fields of the record are persisted in "raw" format (not avro serialized).
     * This behavior happens in maps and arrays too.
     *
     * ["null","type"] type (a.k.a. optional field) is persisted like as if it is
     * ["type"], but the column get deleted if value==null (so value read after
     * will be null).
     *
     * @param persistent
     * Record to be persisted in HBase
     */
    override fun put(key: K, persistent: T) {
        try {
            val schema = persistent.schema
            val keyRaw = HBaseByteInterface.toBytes(key)
            val timeStamp = System.currentTimeMillis()
            // Guarantee Put after Delete
            val put = Put(keyRaw, timeStamp - PUTS_AND_DELETES_PUT_TS_OFFSET)
            val delete = Delete(keyRaw, timeStamp - PUTS_AND_DELETES_DELETE_TS_OFFSET)

            val fields = schema.fields
            for (i in fields.indices) {
                if (!persistent.isDirty(i)) {
                    continue
                }
                val field = fields[i]
                val o = persistent.get(i)
                val hcol = mapping.getColumn(field.name()) ?: throw RuntimeException("HBase mapping for field ["
                        + persistent.javaClass.name + "#" + field.name()
                        + "] not found. Wrong gora-hbase-mapping.xml?")
                addPutsAndDeletes(put, delete, o, field.schema().type,
                        field.schema(), hcol, hcol.qualifier)
            }

            if (delete.size() > 0) {
                table.delete(delete)
                //        table.delete(delete);
                //        table.delete(delete); // HBase sometimes does not delete arbitrarily
            }
            if (put.size() > 0) {
                table.put(put)
            }
        } catch (ex2: IOException) {
            LOG.error(ex2.message, ex2)
        }
    }

    @Throws(IOException::class)
    private fun addPutsAndDeletes(put: Put, delete: Delete, o: Any?, type: Schema.Type,
                                  schema: Schema, hcol: HBaseColumn, qualifier: ByteArray?) {
        when (type) {
            Schema.Type.UNION -> if (isNullable(schema) && o == null) {
                if (qualifier == null) {
                    //          delete.deleteFamily(hcol.family);
                    delete.addFamily(hcol.family)
                } else {
                    //          delete.deleteColumn(hcol.family, qualifier);
                    delete.addColumns(hcol.family, qualifier)
                }
            } else {
                //        int index = GenericData.get().resolveUnion(schema, o);
                val index = getResolvedUnionIndex(schema)
                if (index > 1) {  //if more than 2 type in union, serialize directly for now
                    val serializedBytes = HBaseByteInterface.toBytes(o, schema)
                    put.addColumn(hcol.family, qualifier, serializedBytes)
                } else {
                    val resolvedSchema = schema.types[index]
                    addPutsAndDeletes(put, delete, o, resolvedSchema.type,
                            resolvedSchema, hcol, qualifier)
                }
            }
            Schema.Type.MAP -> {
                // if it's a map that has been modified, then the content should be replaced by the new one
                // This is because we don't know if the content has changed or not.
                if (qualifier == null) {
                    //delete.deleteFamily(hcol.family);
                    delete.addFamily(hcol.family)
                } else {
                    //delete.deleteColumn(hcol.family, qualifier);
                    delete.addColumns(hcol.family, qualifier)
                }
                val set = (o as Map<*, *>).entries
                for ((key, value) in set) {
                    val qual = HBaseByteInterface.toBytes(key)
                    addPutsAndDeletes(put, delete, value, schema.valueType
                            .type, schema.valueType, hcol, qual)
                }
            }
            Schema.Type.ARRAY -> {
                val array = o as List<*>
                for ((j, item) in array.withIndex()) {
                    addPutsAndDeletes(put, delete, item, schema.elementType.type,
                            schema.elementType, hcol, Bytes.toBytes(j))
                }
            }
            else -> {
                val serializedBytes = HBaseByteInterface.toBytes(o, schema)
                put.addColumn(hcol.family, qualifier, serializedBytes)
            }
        }
    }

    private fun isNullable(unionSchema: Schema): Boolean {
        for (innerSchema in unionSchema.types) {
            if (innerSchema.type == Schema.Type.NULL) {
                return true
            }
        }
        return false
    }

    fun delete(obj: T) {
        throw RuntimeException("Not implemented yet")
    }

    /**
     * Deletes the object with the given key.
     * @return always true
     */
    override fun delete(key: K): Boolean {
        try {
            table.delete(Delete(HBaseByteInterface.toBytes(key)))
            //HBase does not return success information and executing a get for
            //success is a bit costly
            return true
        } catch (ex2: IOException) {
            LOG.error(ex2.message, ex2)
            return false
        }

    }

    override fun deleteByQuery(query: Query<K, T>): Long {
        try {
            val fields = getFieldsToQuery(query.fields)
            //find whether all fields are queried, which means that complete
            //rows will be deleted
            val isAllFields = Arrays.equals(fields, getFields())

            var result: org.apache.gora.query.Result<K, T>? = null
            result = query.execute()
            val deletes = ArrayList<Delete>()
            while (result.next()) {
                val delete = Delete(HBaseByteInterface.toBytes(result.key))
                deletes.add(delete)
                if (!isAllFields) {
                    addFields(delete, query)
                }
            }
            table.delete(deletes)
            return deletes.size.toLong()
        } catch (ex: Exception) {
            LOG.error(ex.message, ex)
            return -1
        }

    }

    override fun flush() {
        try {
            table.flushCommits()
        } catch (ex: IOException) {
            LOG.error(ex.message, ex)
        }

    }

    override fun newQuery(): Query<K, T> {
        return HBaseQuery<K, T>(this)
    }

    override fun getPartitions(query: Query<K, T>): List<PartitionQuery<K, T>> {
        // taken from o.a.h.hbase.mapreduce.TableInputFormatBase
        val keys = table.startEndKeys
        if (keys == null || keys.getFirst() == null ||
                keys.getFirst().size == 0) {
            throw IOException("Expecting at least one region.")
        }

        val partitions = ArrayList<PartitionQuery<K, T>>(keys.getFirst().size)
        for (i in 0 until keys.getFirst().size) {
            val regionLocation = table.getRegionLocation(keys.getFirst()[i]).getHostname()
            val startRow = if (query.startKey != null)
                HBaseByteInterface.toBytes(query.startKey)
            else
                HConstants.EMPTY_START_ROW
            val stopRow = if (query.endKey != null)
                HBaseByteInterface.toBytes(query.endKey)
            else
                HConstants.EMPTY_END_ROW

            // determine if the given start an stop key fall into the region
            if ((startRow.size == 0 || keys.getSecond()[i].size == 0 ||
                            Bytes.compareTo(startRow, keys.getSecond()[i]) < 0) && (stopRow.size == 0 || Bytes.compareTo(stopRow, keys.getFirst()[i]) > 0)) {

                val splitStart = if (startRow.size == 0 || Bytes.compareTo(keys.getFirst()[i], startRow) >= 0)
                    keys.getFirst()[i]
                else
                    startRow

                val splitStop = if ((stopRow.size == 0 || Bytes.compareTo(keys.getSecond()[i], stopRow) <= 0) && keys.getSecond()[i].size > 0)
                    keys.getSecond()[i]
                else
                    stopRow

                val startKey = if (Arrays.equals(HConstants.EMPTY_START_ROW, splitStart))
                    null
                else
                    HBaseByteInterface.fromBytes(keyClass, splitStart)
                val endKey = if (Arrays.equals(HConstants.EMPTY_END_ROW, splitStop))
                    null
                else
                    HBaseByteInterface.fromBytes(keyClass, splitStop)

                val partition = PartitionQueryImpl<K, T>(
                        query, startKey, endKey, regionLocation)
                partition.setConf(getConf())

                partitions.add(partition)
            }
        }
        return partitions
    }

    override fun execute(query: Query<K, T>): org.apache.gora.query.Result<K, T>? {
        try {
            //check if query.fields is null
            query.setFields(*getFieldsToQuery(query.fields))

            if (query.startKey != null && query.startKey == query.endKey) {
                val get = Get(HBaseByteInterface.toBytes(query.startKey))
                addFields(get, query.fields)
                addTimeRange(get, query)
                val result = table.get(get)
                return HBaseGetResult<K, T>(this, query, result)
            } else {
                val scanner = createScanner(query)
                return HBaseScannerResult<K, T>(this, query, scanner)
            }
        } catch (ex: IOException) {
            LOG.error(ex.message, ex)
            return null
        }

    }

    @Throws(IOException::class)
    fun createScanner(query: Query<K, T>): ResultScanner {
        val scan = Scan()

        scan.caching = this.getScannerCaching()

        if (query.startKey != null) {
            scan.startRow = HBaseByteInterface.toBytes(query.startKey)
        }
        if (query.endKey != null) {
            // In HBase the end key is exclusive, so we add a trail zero to make it inclusive
            // as the Gora's query interface declares.
            val endKey = HBaseByteInterface.toBytes(query.endKey)
            val inclusiveEndKey = Arrays.copyOf(endKey, endKey.size + 1)
            scan.stopRow = inclusiveEndKey
        }
        addFields(scan, query)
        if (query.filter != null) {
            val succeeded = filterUtil.setFilter(scan, query.filter, this)
            if (succeeded) {
                // don't need local filter
                query.isLocalFilterEnabled = false
            }
        }

        return table.getScanner(scan)
    }

    private fun addFields(get: Get, fieldNames: Array<String>) {
        for (f in fieldNames) {
            val col = mapping.getColumn(f)
                    ?: throw RuntimeException("HBase mapping for field [" + f + "] not found. " +
                            "Wrong gora-hbase-mapping.xml?")
            val fieldSchema = fieldMap[f]?.schema()
                    ?: throw RuntimeException("HBase mapping for field [" + f + "] not found. " +
                            "Wrong gora-hbase-mapping.xml?")
            addFamilyOrColumn(get, col, fieldSchema)
        }
    }

    private fun addFamilyOrColumn(get: Get, col: HBaseColumn, fieldSchema: Schema) {
        when (fieldSchema.type) {
            Schema.Type.UNION -> {
                val index = getResolvedUnionIndex(fieldSchema)
                val resolvedSchema = fieldSchema.types[index]
                addFamilyOrColumn(get, col, resolvedSchema)
            }
            Schema.Type.MAP, Schema.Type.ARRAY -> get.addFamily(col.family)
            else -> get.addColumn(col.family, col.qualifier)
        }
    }

    @Throws(IOException::class)
    private fun addFields(scan: Scan, query: Query<K, T>) {
        val fields = query.fields
        for (f in fields) {
            val col = mapping.getColumn(f)
                    ?: throw RuntimeException("HBase mapping for field [" + f + "] not found. " +
                            "Wrong gora-hbase-mapping.xml?")
            val fieldSchema = fieldMap[f]?.schema()
                    ?:throw RuntimeException("HBase mapping for field [" + f + "] not found. " +
                    "Wrong gora-hbase-mapping.xml?")
            addFamilyOrColumn(scan, col, fieldSchema)
        }
    }

    private fun addFamilyOrColumn(scan: Scan, col: HBaseColumn, fieldSchema: Schema) {
        when (fieldSchema.type) {
            Schema.Type.UNION -> {
                val index = getResolvedUnionIndex(fieldSchema)
                val resolvedSchema = fieldSchema.types[index]
                addFamilyOrColumn(scan, col, resolvedSchema)
            }
            Schema.Type.MAP, Schema.Type.ARRAY -> scan.addFamily(col.family)
            else -> scan.addColumn(col.family, col.qualifier)
        }
    }

    // TODO: HBase Get, Scan, Delete should extend some common interface with
    // addFamily, etc
    @Throws(IOException::class)
    private fun addFields(delete: Delete, query: Query<K, T>) {
        val fields = query.fields
        for (f in fields) {
            val col = mapping.getColumn(f)
                    ?: throw RuntimeException("HBase mapping for field [" + f + "] not found. " +
                            "Wrong gora-hbase-mapping.xml?")
            val fieldSchema = fieldMap[f]?.schema()
                    ?: throw RuntimeException("HBase mapping for field [" + f + "] not found. " +
                            "Wrong gora-hbase-mapping.xml?")
            addFamilyOrColumn(delete, col, fieldSchema)
        }
    }

    private fun addFamilyOrColumn(delete: Delete, col: HBaseColumn,
                                  fieldSchema: Schema) {
        when (fieldSchema.type) {
            Schema.Type.UNION -> {
                val index = getResolvedUnionIndex(fieldSchema)
                val resolvedSchema = fieldSchema.types[index]
                addFamilyOrColumn(delete, col, resolvedSchema)
            }
            Schema.Type.MAP, Schema.Type.ARRAY -> delete.addFamily(col.family)
            else -> delete.addColumns(col.family, col.qualifier)
        }
    }

    @Throws(IOException::class)
    private fun addTimeRange(get: Get, query: Query<K, T>) {
        if (query.startTime > 0 || query.endTime > 0) {
            if (query.startTime == query.endTime) {
                get.setTimeStamp(query.startTime)
            } else {
                val startTime = if (query.startTime > 0) query.startTime else 0
                val endTime = if (query.endTime > 0) query.endTime else java.lang.Long.MAX_VALUE
                get.setTimeRange(startTime, endTime)
            }
        }
    }

    /**
     * Creates a new Persistent instance with the values in 'result' for the fields listed.
     * @param result result form a HTable#get()
     * @param fields List of fields queried, or null for all
     * @return A new instance with default values for not listed fields
     * null if 'result' is null.
     * @throws IOException
     */
    @Throws(IOException::class)
    fun newInstance(result: Result, fields: Array<String>): T? {
        if (result.isEmpty)
            return null

        val persistent = newPersistent()
        for (f in fields) {
            val col = mapping.getColumn(f)
                    ?: throw RuntimeException("HBase mapping for field [" + f + "] not found. " +
                            "Wrong gora-hbase-mapping.xml?")
            val field = fieldMap[f]
                    ?: throw RuntimeException("HBase mapping for field [" + f + "] not found. " +
                            "Wrong gora-hbase-mapping.xml?")
            val fieldSchema = field.schema()
            setField(result, persistent, col, field, fieldSchema)
        }
        persistent.clearDirty()
        return persistent
    }

    @Throws(IOException::class)
    private fun setField(result: Result, persistent: T, col: HBaseColumn,
                         field: Schema.Field, fieldSchema: Schema) {
        when (fieldSchema.type) {
            Schema.Type.UNION -> {
                val index = getResolvedUnionIndex(fieldSchema)
                if (index > 1) { //if more than 2 type in union, deserialize directly for now
                    val value = result.getValue(col.family, col.qualifier) ?: return
                    setField(persistent, field, value)
                } else {
                    val resolvedSchema = fieldSchema.types[index]
                    setField(result, persistent, col, field, resolvedSchema)
                }
            }
            Schema.Type.MAP -> {
                val qualMap: NavigableMap<ByteArray, ByteArray> = result.noVersionMap[col.family] ?: return
                val valueSchema = fieldSchema.valueType
                val map = java.util.HashMap<Utf8, Any>()
                for ((key, value) in qualMap) {
                    map[Utf8(Bytes.toString(key))] = HBaseByteInterface.fromBytes(valueSchema, value)
                }
                setField(persistent, field, map)
            }
            Schema.Type.ARRAY -> {
                val qualMap = result.getFamilyMap(col.family)
                val valueSchema = fieldSchema.elementType
                val arrayList = ArrayList<Any>()
                val dirtyListWrapper = DirtyListWrapper(arrayList)
                for (e in qualMap.entries) {
                    dirtyListWrapper.add(HBaseByteInterface.fromBytes(valueSchema, e.value))
                }
                setField(persistent, field, arrayList)
            }
            else -> {
                val v = result.getValue(col.family, col.qualifier) ?: return
                setField(persistent, field, v)
            }
        }
    }

    //TODO temporary solution, has to be changed after implementation of saving the index of union type
    private fun getResolvedUnionIndex(unionScema: Schema): Int {
        if (unionScema.types.size == 2) {

            // schema [type0, type1]
            val type0 = unionScema.types[0].type
            val type1 = unionScema.types[1].type

            // Check if types are different and there's a "null", like ["null","type"]
            // or ["type","null"]
            if (type0 != type1 && (type0 == Schema.Type.NULL || type1 == Schema.Type.NULL)) {

                return if (type0 == Schema.Type.NULL)
                    1
                else
                    0
            }
        }
        return 2
    }

    private fun setField(persistent: T, field: Schema.Field, map: Map<*, *>) {
        persistent.put(field.pos(), DirtyMapWrapper(map))
    }

    private fun setField(persistent: T, field: Schema.Field, value: ByteArray?) {
        persistent.put(field.pos(), HBaseByteInterface.fromBytes(field.schema(), value))
    }

    private fun setField(persistent: T, field: Schema.Field, list: List<*>) {
        persistent.put(field.pos(), DirtyListWrapper(list))
    }

    private fun readMapping(filename: String): HBaseMapping {

        val mappingBuilder = HBaseMapping.HBaseMappingBuilder()

        try {
            val builder = SAXBuilder()
            val doc = builder.build(javaClass.classLoader.getResourceAsStream(filename))
            val root = doc.rootElement

//            val resource = javaClass.classLoader.getResourceAsStream(filename)
//            val doc = Jsoup.parse(resource, "UTF-8", filename)
//            val root = doc.root() as org.jsoup.nodes.Element

            val tableElements = root.getChildren("table").mapNotNull { it as? Element }
            for (tableElement in tableElements) {
                val tableName = tableElement.getAttributeValue("name")

                val fieldElements = tableElement.getChildren("family").mapNotNull { it as? Element }
                for (fieldElement in fieldElements) {
                    val familyName = fieldElement.getAttributeValue("name")
                    val compression = fieldElement.getAttributeValue("compression")
                    val blockCache = fieldElement.getAttributeValue("blockCache")
                    val blockSize = fieldElement.getAttributeValue("blockSize")
                    val bloomFilter = fieldElement.getAttributeValue("bloomFilter")
                    val maxVersions = fieldElement.getAttributeValue("maxVersions")
                    val timeToLive = fieldElement.getAttributeValue("timeToLive")
                    val inMemory = fieldElement.getAttributeValue("inMemory")

                    mappingBuilder.addFamilyProps(tableName, familyName, compression,
                            blockCache, blockSize, bloomFilter, maxVersions, timeToLive,
                            inMemory)
                }
            }

            val classElements = root.getChildren("class").mapNotNull { it as? Element }
            var keyClassMatches = false
            for (classElement in classElements) {
                if (classElement.getAttributeValue("keyClass") == keyClass.canonicalName && classElement.getAttributeValue("name") == persistentClass.canonicalName) {
                    LOG.debug("Keyclass and nameclass match.")
                    keyClassMatches = true

                    val tableNameFromMapping = classElement.getAttributeValue("table")
                    val tableName = getSchemaName(tableNameFromMapping, persistentClass)

                    //tableNameFromMapping could be null here
                    if (tableName != tableNameFromMapping) {
                        //TODO this might not be the desired behavior as the user might have actually made a mistake.
                        LOG.warn("Mismatching schema's names. Mappingfile schema: '{}'. PersistentClass schema's name: '{}'. Assuming they are the same.", tableNameFromMapping, tableName)
                        if (tableNameFromMapping != null) {
                            mappingBuilder.renameTable(tableNameFromMapping, tableName)
                        }
                    }
                    mappingBuilder.tableName = TableName.valueOf(tableName)

                    val fields = classElement.getChildren("field").mapNotNull { it as? Element }
                    for (field in fields) {
                        val fieldName = field.getAttributeValue("name")
                        val family = field.getAttributeValue("family")
                        val qualifier = field.getAttributeValue("qualifier")
                        mappingBuilder.addField(fieldName, family, qualifier)
                        mappingBuilder.addColumnFamily(tableName, family)
                    }
                    //we found a matching key and value class definition,
                    //do not continue on other class definitions
                    break
                }
            }

            if (!keyClassMatches) {
                throw ConfigurationException("Gora-hbase-mapping does not include the name and keyClass in the databean.")
            }
        } catch (ex: MalformedURLException) {
            LOG.error("Error while trying to read the mapping file {}. "
                    + "Expected to be in the classpath "
                    + "(ClassLoader#getResource(java.lang.String)).",
                    filename)
            LOG.error("Actual classpath = {}", (javaClass.classLoader as URLClassLoader).urLs)
            throw ex
        } catch (ex: IOException) {
            LOG.error(ex.message, ex)
            throw ex
        } catch (ex: Exception) {
            LOG.error(ex.message, ex)
            throw IOException(ex)
        }

        return mappingBuilder.build()
    }

    override fun close() {
        try {
            table.close()
        } catch (ex: IOException) {
            LOG.error(ex.message, ex)
        }

    }

    override fun getConf(): Configuration {
        return conf
    }

    override fun setConf(conf: Configuration) {
        this.conf = conf
    }

    /**
     * Gets the Scanner Caching optimization value
     * @return The value used internally in [Scan.setCaching]
     */
    fun getScannerCaching(): Int {
        return this.scannerCaching
    }

    /**
     * Sets the value for Scanner Caching optimization
     *
     * @see Scan.setCaching
     * @param numRows the number of rows for caching &gt;= 0
     * @return &lt;&lt;Fluent interface&gt;&gt;
     */
    fun setScannerCaching(numRows: Int): HBaseStore<K, T> {
        if (numRows < 0) {
            LOG.warn("Invalid Scanner Caching optimization value. Cannot set to: {}.", numRows)
            return this
        }
        this.scannerCaching = numRows
        return this
    }

    private fun closeHBaseAdmin() {
        try {
            admin.close()
        } catch (ioe: IOException) {
            LOG.error("An error occured whilst closing HBase Admin", ioe)
        }

    }

    companion object {

        val LOG = LoggerFactory.getLogger(HBaseStore::class.java)

        val PARSE_MAPPING_FILE_KEY = "gora.hbase.mapping.file"

        val DEFAULT_MAPPING_FILE = "gora-hbase-mapping.xml"

        private val SCANNER_CACHING_PROPERTIES_KEY = "scanner.caching"
        private val SCANNER_CACHING_PROPERTIES_DEFAULT = 0

        private val PUTS_AND_DELETES_PUT_TS_OFFSET = 1
        private val PUTS_AND_DELETES_DELETE_TS_OFFSET = 2
    }
}
