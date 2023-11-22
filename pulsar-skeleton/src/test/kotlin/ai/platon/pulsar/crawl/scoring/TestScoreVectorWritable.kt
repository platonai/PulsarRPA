package ai.platon.pulsar.crawl.scoring

import ai.platon.pulsar.common.ScoreVector
import ai.platon.pulsar.common.config.MutableConfig
import ai.platon.pulsar.persist.gora.GoraStorage
import ai.platon.pulsar.persist.gora.generated.GWebPage
import org.apache.gora.store.DataStore
import kotlin.test.*

class TestScoreVectorWritable {
    private var conf = MutableConfig()
    private lateinit var datastore: DataStore<String, GWebPage>
    private var persistentDataStore = false

    @BeforeTest
    @Throws(Exception::class)
    fun setUp() {
        conf["storage.data.store.class"] = "org.apache.gora.memory.store.MemStore"
        datastore = GoraStorage.createDataStore(conf, String::class.java, GWebPage::class.java)
    }

    @AfterTest
    @Throws(Exception::class)
    fun tearDown() {
        // empty the database after test
        if (!persistentDataStore) {
            datastore.deleteByQuery(datastore.newQuery())
            datastore.flush()
            datastore.close()
        }
    }

    @Test
    fun testIO() {
        val score = ScoreVector("11", 1, 2, 3, 4, 5, 6, -7, -8, 9, 10, 11)
        // page = store.get(key);
        // assertNotNull(page);
    }
}
