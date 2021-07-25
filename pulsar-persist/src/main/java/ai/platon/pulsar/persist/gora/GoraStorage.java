package ai.platon.pulsar.persist.gora;

import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.common.config.Params;
import ai.platon.pulsar.persist.HadoopUtils;
import ai.platon.pulsar.persist.gora.generated.GWebPage;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.util.GoraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static ai.platon.pulsar.common.config.AppConstants.MONGO_STORE_CLASS;
import static ai.platon.pulsar.common.config.CapabilityTypes.*;

public class GoraStorage {
    public static final Logger logger = LoggerFactory.getLogger(GoraStorage.class);

    // load properties from gora.properties
    public static Properties properties = DataStoreFactory.createProps();
    private static Map<String, Object> dataStores = new HashMap<>();

    @SuppressWarnings("unchecked")
    public synchronized static <K, V extends Persistent> DataStore<K, V>
    createDataStore(ImmutableConfig conf, Class<K> keyClass, Class<V> persistentClass)
            throws GoraException, ClassNotFoundException {
        String className = conf.get(STORAGE_DATA_STORE_CLASS, MONGO_STORE_CLASS);
        Class<? extends DataStore<K, V>> dataStoreClass = (Class<? extends DataStore<K, V>>)Class.forName(className);
        return createDataStore(conf, keyClass, persistentClass, dataStoreClass);
    }

    @SuppressWarnings("unchecked")
    public synchronized static <K, V extends Persistent> DataStore<K, V>
    createDataStore(ImmutableConfig conf,
                    Class<K> keyClass, Class<V> persistentClass, Class<? extends DataStore<K, V>> dataStoreClass
    ) throws GoraException {
        String crawlId = conf.get(STORAGE_CRAWL_ID, "");
        String schemaPrefix = "";
        if (!crawlId.isEmpty()) {
            schemaPrefix = crawlId + "_";
        }

        String schema;
        if (GWebPage.class.equals(persistentClass)) {
            schema = conf.get(STORAGE_SCHEMA_WEBPAGE, "webpage");
        } else {
            throw new UnsupportedOperationException("Unable to create storage for class " + persistentClass);
        }

        Object o = dataStores.get(schema);
        if (o == null) {
            org.apache.hadoop.conf.Configuration hadoopConf = HadoopUtils.INSTANCE.toHadoopConfiguration(conf);
            hadoopConf.set(STORAGE_PREFERRED_SCHEMA_NAME, schemaPrefix + "webpage");
            DataStore<K, V> dataStore = DataStoreFactory.createDataStore(dataStoreClass,
                    keyClass, persistentClass, hadoopConf, properties, schema);

            dataStores.put(schema, dataStore);

            Params.of(
                    "Backend data store", dataStore.getClass().getSimpleName(),
                    "realSchema", dataStore.getSchemaName()
            ).withLogger(logger).info(true);

            return dataStore;
        }

        return (DataStore<K, V>) o;
    }

    public synchronized static void close() {
        dataStores.forEach((schema, store) -> {
            if (store instanceof DataStore) {
                logger.info("Closing data store <{}>", schema);
                ((DataStore<?, ?>) store).close();
            }
        });
        dataStores.clear();
    }
}
