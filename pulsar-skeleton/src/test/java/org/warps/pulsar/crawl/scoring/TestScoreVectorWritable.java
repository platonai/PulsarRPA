package org.warps.pulsar.crawl.scoring;

import org.apache.gora.store.DataStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.warps.pulsar.common.config.MutableConfig;
import org.warps.pulsar.persist.gora.GoraStorage;
import org.warps.pulsar.persist.gora.generated.GWebPage;

public class TestScoreVectorWritable {

    protected MutableConfig conf = new MutableConfig();
    protected DataStore<String, GWebPage> datastore;
    protected boolean persistentDataStore = false;

    @Before
    public void setUp() throws Exception {
        conf.set("storage.data.store.class", "org.apache.gora.memory.store.MemStore");
        datastore = GoraStorage.createDataStore(conf.unbox(), String.class, GWebPage.class);
    }

    @After
    public void tearDown() throws Exception {
        // empty the database after test
        if (!persistentDataStore) {
            datastore.deleteByQuery(datastore.newQuery());
            datastore.flush();
            datastore.close();
        }
    }

    @Test
    public void testIO() {
        ScoreVector score = new ScoreVector("11", 1, 2, 3, 4, 5, 6, -7, -8, 9, 10, 11);
        // page = store.get(key);
        // assertNotNull(page);
    }
}
