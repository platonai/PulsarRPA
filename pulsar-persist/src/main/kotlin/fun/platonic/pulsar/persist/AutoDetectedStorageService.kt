package `fun`.platonic.pulsar.persist

import `fun`.platonic.pulsar.common.RuntimeUtils
import `fun`.platonic.pulsar.common.StringUtil
import `fun`.platonic.pulsar.common.config.CapabilityTypes.*
import `fun`.platonic.pulsar.common.config.ImmutableConfig
import `fun`.platonic.pulsar.common.config.PulsarConstants.*
import `fun`.platonic.pulsar.persist.gora.GoraStorage
import `fun`.platonic.pulsar.persist.gora.generated.GWebPage
import org.apache.gora.persistency.Persistent
import org.apache.gora.store.DataStore
import org.apache.gora.util.GoraException
import org.apache.hadoop.conf.Configuration
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by vincent on 19-1-19.
 * Copyright @ 2013-2019 Platon AI. All rights reserved
 */
class AutoDetectedStorageService(conf: ImmutableConfig): AutoCloseable {

    val storeClassName: String = detectDataStoreClassName(conf)
    val pageStoreClass: Class<out DataStore<String, GWebPage>> = detectDataStoreClass(conf)
    val pageStore: DataStore<String, GWebPage>
    val isClosed = AtomicBoolean()

    init {
        try {
            pageStore = GoraStorage.createDataStore(conf.unbox(), String::class.java, GWebPage::class.java, pageStoreClass)
        } catch (e: ClassNotFoundException) {
            log.error(StringUtil.stringifyException(e))
            throw RuntimeException(e.message)
        } catch (e: GoraException) {
            log.error(StringUtil.stringifyException(e))
            throw RuntimeException(e.message)
        }
    }

    override fun close() {
        if (isClosed.getAndSet(true)) {
            return
        }

        pageStore.close()
    }

    companion object {

        private val log = LoggerFactory.getLogger(AutoDetectedStorageService::class.java)

        /**
         * Return the DataStore persistent class used to persist WebPage.
         *
         * @param conf PulsarConstants configuration
         * @return the DataStore persistent class
         */
        fun detectDataStoreClassName(conf: ImmutableConfig): String {
            return when {
                conf.isDryRun -> MEM_STORE_CLASS
                conf.isDistributedFs -> conf.get(STORAGE_DATA_STORE_CLASS, HBASE_STORE_CLASS)
                RuntimeUtils.checkIfProcessRunning(".+HMaster.+") ->
                    conf.get(STORAGE_DATA_STORE_CLASS, HBASE_STORE_CLASS)
                RuntimeUtils.checkIfProcessRunning(".+/usr/bin/mongod .+") ->
                    conf.get(STORAGE_DATA_STORE_CLASS, MONGO_STORE_CLASS)
                RuntimeUtils.checkIfProcessRunning(".+/tmp/.+extractmongod .+") ->
                    conf.get(STORAGE_DATA_STORE_CLASS, MONGO_STORE_CLASS)
                else -> MEM_STORE_CLASS
            }
        }

        /**
         * Return the DataStore persistent class used to persist WebPage.
         *
         * @param conf PulsarConstants configuration
         * @return the DataStore persistent class
         */
        @Throws(ClassNotFoundException::class)
        fun <K, V : Persistent> detectDataStoreClass(conf: ImmutableConfig): Class<out DataStore<K, V>> {
            return Class.forName(detectDataStoreClassName(conf)) as Class<out DataStore<K, V>>
        }

        @Throws(ClassNotFoundException::class)
        fun <K, V : Persistent> detectDataStoreClass(conf: Configuration): Class<out DataStore<K, V>> {
            return detectDataStoreClass(ImmutableConfig(conf))
        }
    }
}
