package ai.platon.pulsar.persist

import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.Runtimes
import ai.platon.pulsar.common.config.AppConstants.*
import ai.platon.pulsar.common.config.CapabilityTypes.STORAGE_DATA_STORE_CLASS
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.persist.gora.GoraStorage
import ai.platon.pulsar.persist.gora.generated.GWebPage
import org.apache.commons.lang3.SystemUtils
import org.apache.gora.persistency.Persistent
import org.apache.gora.store.DataStore
import org.slf4j.LoggerFactory

/**
 * Created by vincent on 19-1-19.
 * Copyright @ 2013-2019 Platon AI. All rights reserved
 */
class AutoDetectStorageProvider(val conf: ImmutableConfig) {
    private val log = LoggerFactory.getLogger(AutoDetectStorageProvider::class.java)

    val storeClassName: String get() = detectDataStoreClassName(conf)
    val pageStoreClass: Class<out DataStore<String, GWebPage>> get() = detectDataStoreClass(conf)

    fun createPageStore(): DataStore<String, GWebPage> {
        if (!AppContext.isActive) {
            throw IllegalStateException("App context is inactive")
        }

        val pageStore = GoraStorage.createDataStore(conf, String::class.java, GWebPage::class.java, pageStoreClass)
        log.info("Storage is created: {} realSchema: {}", pageStoreClass, pageStore.schemaName)
        return pageStore
    }

    companion object {

        /**
         * Return the DataStore persistent class used to persist WebPage.
         *
         * @param conf AppConstants configuration
         * @return the DataStore persistent class
         */
        fun detectDataStoreClassName(conf: ImmutableConfig): String {
            if (!AppContext.isActive) {
                throw IllegalStateException("App context is inactive")
            }

            val specified = conf.get(STORAGE_DATA_STORE_CLASS)
            if (specified != null) {
                return specified
            }

            when {
                SystemUtils.IS_OS_WINDOWS -> return when {
                    conf.isDryRun -> FILE_BACKEND_STORE_CLASS
                    SystemUtils.IS_OS_WINDOWS && Runtimes.checkIfProcessRunning(".*mongod.exe .+") -> MONGO_STORE_CLASS
                    else -> FILE_BACKEND_STORE_CLASS
                }
                SystemUtils.IS_OS_LINUX -> return when {
                    conf.isDryRun -> FILE_BACKEND_STORE_CLASS
                    conf.isDistributedFs -> HBASE_STORE_CLASS
                    Runtimes.checkIfProcessRunning(".+HMaster.+") -> HBASE_STORE_CLASS
                    Runtimes.checkIfProcessRunning(".+/usr/bin/mongod .+") -> MONGO_STORE_CLASS
                    Runtimes.checkIfProcessRunning(".+/tmp/.+extractmongod .+") -> MONGO_STORE_CLASS
                    else -> FILE_BACKEND_STORE_CLASS
                }
                else -> return FILE_BACKEND_STORE_CLASS
            }
        }

        /**
         * Return the DataStore persistent class used to persist webpages.
         *
         * @param conf AppConstants configuration
         * @return the DataStore persistent class
         */
        @Throws(ClassNotFoundException::class)
        fun <K, V : Persistent> detectDataStoreClass(conf: ImmutableConfig): Class<out DataStore<K, V>> {
            return Class.forName(detectDataStoreClassName(conf)) as Class<out DataStore<K, V>>
        }
    }
}
