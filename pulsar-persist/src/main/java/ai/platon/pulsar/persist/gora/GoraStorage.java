package ai.platon.pulsar.persist.gora;

import ai.platon.pulsar.common.config.Params;
import ai.platon.pulsar.persist.gora.generated.GWebPage;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.util.GoraException;
import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static ai.platon.pulsar.common.config.CapabilityTypes.*;
import static ai.platon.pulsar.common.config.PulsarConstants.HBASE_STORE_CLASS;
import static ai.platon.pulsar.common.config.PulsarConstants.MONGO_STORE_CLASS;

/**
 * Created by vincent on 17-5-15.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
public class GoraStorage {
    public static final Logger LOG = LoggerFactory.getLogger(GoraStorage.class);

    // load properties from gora.properties
    public static Properties properties = DataStoreFactory.createProps();
    private static Map<String, Object> dataStores = new HashMap<>();

    @SuppressWarnings("unchecked")
    public synchronized static <K, V extends Persistent> DataStore<K, V>
    createDataStore(
            Configuration conf,
            Class<K> keyClass,
            Class<V> persistentClass
    ) throws GoraException, ClassNotFoundException {
        String className = conf.get(STORAGE_DATA_STORE_CLASS, MONGO_STORE_CLASS);
        Class<? extends DataStore<K, V>> dataStoreClass = (Class<? extends DataStore<K, V>>)Class.forName(className);
        return createDataStore(conf, keyClass, persistentClass, dataStoreClass);
    }

    /**
     * Creates a store for the given persistentClass. Currently supports WebPage store
     */
    @SuppressWarnings("unchecked")
    public synchronized static <K, V extends Persistent> DataStore<K, V>
    createDataStore(
            Configuration conf,
            Class<K> keyClass,
            Class<V> persistentClass,
            Class<? extends DataStore<K, V>> dataStoreClass
    ) throws GoraException {
        String crawlId = conf.get(STORAGE_CRAWL_ID, "");
        String schemaPrefix = "";
        if (!crawlId.isEmpty()) {
            schemaPrefix = crawlId + "_";
        }

        String schema;
        if (GWebPage.class.equals(persistentClass)) {
            schema = conf.get(STORAGE_SCHEMA_WEBPAGE, "webpage");
            conf.set(STORAGE_PREFERRED_SCHEMA_NAME, schemaPrefix + "webpage");
        } else {
            throw new UnsupportedOperationException("Unable to create storage for class " + persistentClass);
        }

        Object o = dataStores.get(schema);
        if (o == null) {
            DataStore<K, V> dataStore = DataStoreFactory.createDataStore(dataStoreClass,
                    keyClass, persistentClass, conf, properties, schema);

            dataStores.put(schema, dataStore);

            Params.of(
                    "Backend data store", dataStore.getClass().getSimpleName(),
                    "realSchema", dataStore.getSchemaName()
            ).withLogger(LOG).info(true);

            return dataStore;
        }

        return (DataStore<K, V>) o;
    }
}
