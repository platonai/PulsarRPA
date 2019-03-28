package ai.platon.pulsar.persist.gora

import ai.platon.pulsar.common.PulsarPaths
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.gora.generated.GWebPage
import org.apache.gora.avro.store.AvroStore
import org.apache.gora.avro.store.DataFileAvroStore
import org.apache.gora.memory.store.MemStore
import org.apache.gora.persistency.impl.PersistentBase
import org.apache.gora.query.PartitionQuery
import org.apache.gora.query.Query
import org.apache.gora.query.Result
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.reflect.jvm.isAccessible

class ToyFileStore<K, T: PersistentBase>: MemStore<K, T>() {
    val LOG = LoggerFactory.getLogger(ToyFileStore::class.java)

    private val baseDir = PulsarPaths.dataDir
    private val isClosed = AtomicBoolean()
    private lateinit var inputFile: Path
    private lateinit var outputFile: Path
    private lateinit var outputStore: AvroStore<K, T>

    @Synchronized
    override fun initialize(keyClass: Class<K>, persistentClass: Class<T>, properties: Properties) {
        super.initialize(keyClass, persistentClass, properties)

        val warnings = """
+===============================================================================+
|  Warning:                                                                     |
|      You are using a TOY file store, which is designed for demos only.        |
|      Do NOT use it for a product environment.                                 |
|      Run a HBase instead, it will be detected automatically.                  |
+===============================================================================+"""
        LOG.warn(warnings)

        val filename = persistentClass.simpleName.toLowerCase()
        inputFile = Paths.get(baseDir.toString(), "toystore", "$filename.in.data")
        outputFile = Paths.get(baseDir.toString(), "toystore", "$filename.out.data")

        try {
            cache()
            initializeOutputStoreInAdvance()
        } catch (e: Throwable) {
            LOG.error(e.toString())
        }
    }

    override fun getSchemaName(): String {
        return "ToyFileStore"
    }

    @Synchronized
    override fun get(key: K): T? {
        return super.get(key)
    }

    @Synchronized
    override fun get(key: K, fields: Array<out String>?): T? {
        return super.get(key, fields)
    }

    @Synchronized
    override fun execute(query: Query<K, T>?): Result<K, T> {
        return super.execute(query)
    }

    @Synchronized
    override fun put(key: K, obj: T) {
        super.put(key, obj)
    }

    @Synchronized
    override fun delete(key: K): Boolean {
        return super.delete(key)
    }

    @Synchronized
    override fun deleteByQuery(query: Query<K, T>): Long {
        return super.deleteByQuery(query)
    }

    @Synchronized
    override fun deleteSchema() {
        super.deleteSchema()
    }

    @Synchronized
    override fun getPartitions(query: Query<K, T>?): MutableList<PartitionQuery<K, T>> {
        return super.getPartitions(query)
    }

    @Synchronized
    private fun cache() {
        if (!Files.exists(inputFile)) {
            return
        }

        val store = getFileStore(inputFile)
        val query = store.newQuery()
        val result = query.execute()
        while (result.next()) {
            val value = result.get()
            if (value is GWebPage) {
                val url = value.baseUrl?.toString()
                if (url != null) {
                    val page = WebPage.box(url, value, false)
                    val key = page.metadata["key"]
                    if (key != null) {
                        println(page.url + "\t[" + page.contentAsString.length + "]\t" + key)
                        @Suppress("UNCHECKED_CAST")
                        this.put(key as K, value)
                    }
                }
            }
        }

        LOG.info("Loaded total ${map.size} items from toy file store")

        store.close()
    }

    @Synchronized
    private fun dump() {
        var count = 0
        map.forEach { k, v ->
            val key = k.toString()

            ++count
            if (v is GWebPage) {
                val page = WebPage.box(key, v, true)
                // println(page.baseUrl + "\t" + key)
                println("cache key as url: " + page.url
                        + "\tlength: [" + page.content?.array()?.size + "]"
                        + "\tmeta key: " + page.metadata["key"])
                page.metadata["key"] = key

                outputStore.put(k as K, v as T)
            }
        }

//        val query = this.newQuery()
//        val result = query.execute()
//        var count = 0
//        while (result.next()) {
//            ++count
//            val key = result.key.toString()
//            val value = result.get()
//            if (value is GWebPage) {
//                val page = WebPage.box(key, value, true)
//                // println(page.baseUrl + "\t" + key)
//                println("cache key as url: " + page.url
//                        + "\tlength: [" + page.content?.array()?.size + "]"
//                        + "\tmeta key: " + page.metadata["key"])
//                page.metadata["key"] = key
//            }
//            outputStore.put(result.key, result.get())
//        }

        LOG.info("Persisting total $count items into toy file store")

        outputStore.flush()
        outputStore.close()

        Files.deleteIfExists(inputFile)
        Files.copy(outputFile, inputFile)
    }

    @Synchronized
    override fun close() {
        if (isClosed.getAndSet(true)) {
            return
        }

        try {
            dump()
        } catch (e: Throwable) {
            LOG.error(ai.platon.pulsar.common.StringUtil.stringifyException(e))
        }

        super.deleteSchema()
    }

    /**
     * Create a output store and initialize DataFileWriter in advance rather than in [close]
     * because hadoop forbid any Hadoop thing be created in destruction phrase
     * */
    private fun initializeOutputStoreInAdvance() {
        Files.deleteIfExists(outputFile)
        outputStore = getFileStore(outputFile)
        DataFileAvroStore::class.members.filter { it.name == "getWriter" }.forEach {
            it.isAccessible = true
            it.call(outputStore)
        }
    }

    private fun getFileStore(dataFile: Path): DataFileAvroStore<K, T> {
        val store = DataFileAvroStore<K, T>()
        store.initialize(keyClass, persistentClass, properties)

        Files.createDirectories(dataFile.parent)

        store.outputPath = dataFile.toString()
        store.inputPath = dataFile.toString()

        return store
    }

    private fun getInternalConnection() {

    }
}
