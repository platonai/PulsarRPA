package ai.platon.pulsar.persist

import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.IllegalApplicationStateException
import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.common.Runtimes
import ai.platon.pulsar.common.config.AppConstants.*
import ai.platon.pulsar.common.config.CapabilityTypes.STORAGE_DATA_STORE_CLASS
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.persist.gora.GoraStorage
import ai.platon.pulsar.persist.gora.generated.GWebPage
import ai.platon.pulsar.persist.mongo.MongoDBUtils
import org.apache.commons.lang3.SystemUtils
import org.apache.gora.mongodb.store.MongoStoreParameters
import org.apache.gora.persistency.Persistent
import org.apache.gora.store.DataStore
import org.slf4j.LoggerFactory

/**
 * Created by vincent on 19-1-19.
 * Copyright @ 2013-2019 Platon AI. All rights reserved
 */
class DataStorageFactory(conf: ImmutableConfig) {
    private val hadoopConf = HadoopUtils.toHadoopConfiguration(conf)
    private val pageStoreClass: Class<out DataStore<String, GWebPage>> get() = detectDataStoreClass(hadoopConf)

    private var _dataStore: DataStore<String, GWebPage>? = null

    val storeClassName: String get() = detectDataStoreClassName(hadoopConf)

    @get:Synchronized
    val schemaName: String get() = _dataStore?.schemaName ?: "(unknown, not initialized)"

    @Synchronized
    fun isInitialized() = _dataStore != null

    @Synchronized
    fun canConnect() = runCatching { _dataStore?.schemaExists() == true }.getOrNull() ?: false

    @Synchronized
    fun getOrCreatePageStore(): DataStore<String, GWebPage> {
        if (_dataStore == null) {
            _dataStore = createPageStore0()
        }
        return _dataStore!!
    }

    private fun createPageStore0(): DataStore<String, GWebPage> {
        if (!AppContext.isActive) {
            throw IllegalApplicationStateException("Inactive application context")
        }

        val pageStore = GoraStorage.createDataStore(hadoopConf, String::class.java, GWebPage::class.java, pageStoreClass)
        logger.info("Backend data store is created: {}, realSchema: {}", pageStoreClass.name, pageStore.schemaName)
        return pageStore
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DataStorageFactory::class.java)

        fun detectDataStoreClassName(conf: ImmutableConfig): String {
            return detectDataStoreClassName(HadoopUtils.toHadoopConfiguration(conf))
        }

        /**
         * Return the DataStore persistent class used to persist WebPage.
         *
         * @param conf AppConstants configuration
         * @return the DataStore persistent class
         */
        fun detectDataStoreClassName(conf: HadoopConfiguration): String {
            if (!AppContext.isActive) {
                throw IllegalApplicationStateException("Inactive application context")
            }

            val specified = conf.get(STORAGE_DATA_STORE_CLASS)
            if (specified != null) {
                return specified
            }

            val mongoServers = conf.get(MongoStoreParameters.PROP_MONGO_SERVERS)
            if (mongoServers != null) {
                return MONGO_STORE_CLASS
            }

            val isDistributedFs = conf["fs.defaultFS", ""].startsWith("hdfs://")
            var dataStoreClass = when {
                SystemUtils.IS_OS_WINDOWS -> when {
                    Runtimes.checkIfProcessRunning(".*mongod.exe .+") -> MONGO_STORE_CLASS
                    else -> FILE_BACKEND_STORE_CLASS
                }
                SystemUtils.IS_OS_LINUX -> when {
                    isDistributedFs -> HBASE_STORE_CLASS
                    Runtimes.checkIfProcessRunning(".+HMaster.+") -> HBASE_STORE_CLASS
                    Runtimes.checkIfProcessRunning(".+/usr/bin/mongod .+") -> MONGO_STORE_CLASS
                    else -> FILE_BACKEND_STORE_CLASS
                }
                else -> FILE_BACKEND_STORE_CLASS
            }

            /**
             * Sometimes MongoClient is not available or not configured
             * */
            if (MONGO_STORE_CLASS == dataStoreClass && !checkIfMongoClientAvailable(conf)) {
                logger.info("MongoDB is running but mongo client is not available, fallback to FileBackendPageStore")
                dataStoreClass = FILE_BACKEND_STORE_CLASS
            }

            return dataStoreClass
        }

        /**
         * Return the DataStore persistent class used to persist webpages.
         *
         * @param conf AppConstants configuration
         * @return the DataStore persistent class
         */
        @Throws(ClassNotFoundException::class)
        fun <K, V : Persistent> detectDataStoreClass(conf: HadoopConfiguration): Class<out DataStore<K, V>> {
            return Class.forName(detectDataStoreClassName(conf)) as Class<out DataStore<K, V>>
        }

        private fun checkIfMongoClientAvailable(conf: HadoopConfiguration): Boolean {
            val mongoServers = conf.get(MongoStoreParameters.PROP_MONGO_SERVERS)
            if (mongoServers != null) {
                return MongoDBUtils.isMongoReachable(mongoServers)
            }
            return ResourceLoader.exists("gora-mongodb-mapping.xml")
        }
    }
}
