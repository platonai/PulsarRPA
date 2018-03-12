package org.warps.pulsar.persist.gora;

import org.apache.gora.persistency.Persistent;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.util.GoraException;
import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.warps.pulsar.common.RuntimeUtils;
import org.warps.pulsar.common.config.ImmutableConfig;
import org.warps.pulsar.common.config.Params;
import org.warps.pulsar.persist.gora.generated.GWebPage;

import java.util.HashMap;
import java.util.Map;

import static org.warps.pulsar.common.PulsarConstants.HBASE_STORE_CLASS;
import static org.warps.pulsar.common.PulsarConstants.MEM_STORE_CLASS;
import static org.warps.pulsar.common.config.CapabilityTypes.*;

/**
 * Created by vincent on 17-5-15.
 * Copyright @ 2013-2017 Warpspeed Information. All rights reserved
 */
public class GoraStorage {
    public static final Logger LOG = LoggerFactory.getLogger(GoraStorage.class);

    private static Map<String, Object> dataStores = new HashMap<>();

    /**
     * Creates a store for the given persistentClass. Currently supports WebPage store
     */
    @SuppressWarnings("unchecked")
    public synchronized static <K, V extends Persistent> DataStore<K, V> createDataStore(
            Configuration conf, Class<K> keyClass, Class<V> persistentClass) throws ClassNotFoundException, GoraException {
        String crawlId = conf.get(CRAWL_ID, "");
        String schemaPrefix = "";
        if (!crawlId.isEmpty()) {
            schemaPrefix = crawlId + "_";
        }

        String schema;
        if (GWebPage.class.equals(persistentClass)) {
            schema = conf.get("storage.schema.webpage", "webpage");
            conf.set("preferred.schema.name", schemaPrefix + "webpage");
        } else {
            throw new UnsupportedOperationException("Unable to create storage for class " + persistentClass);
        }

        Object o = dataStores.get(schema);
        if (o == null) {
            Class<? extends DataStore<K, V>> dataStoreClass = getDataStoreClass(conf);
            DataStore<K, V> dataStore = DataStoreFactory.createDataStore(dataStoreClass, keyClass, persistentClass, conf, schema);
            dataStores.put(schema, dataStore);

            Params.of(
                    "Backend data store", dataStore.getClass().getSimpleName(),
                    "realSchema", dataStore.getSchemaName()
            ).withLogger(LOG).info(true);

            return dataStore;
        }

        return (DataStore<K, V>) o;
    }

    /**
     * Return the DataStore persistent class used to persist WebPage.
     *
     * @param conf PulsarConstants configuration
     * @return the DataStore persistent class
     */
    @SuppressWarnings("unchecked")
    private static <K, V extends Persistent> Class<? extends DataStore<K, V>>
    getDataStoreClass(Configuration conf) throws ClassNotFoundException {
        boolean autoDetect = conf.getBoolean(STORAGE_DETECT_DATA_STORE, true);
        String className;
        boolean isDistributedFs = ImmutableConfig.isDistributedFs(conf);
        if (autoDetect && !isDistributedFs) {
            boolean localHBaseRunning = RuntimeUtils.checkIfJavaProcessRunning("HMaster");
            className = localHBaseRunning ? HBASE_STORE_CLASS : MEM_STORE_CLASS;
//            try {
//                HBaseAdmin.checkHBaseAvailable(conf);
//                className = HBASE_STORE_CLASS;
//            } catch (Exception e) {
//                className = MEM_STORE_CLASS;
//            }
        } else {
            className = conf.get(STORAGE_DATA_STORE_CLASS, MEM_STORE_CLASS);
        }

        return (Class<? extends DataStore<K, V>>) Class.forName(className);
    }
}
