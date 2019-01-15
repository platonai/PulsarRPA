/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package fun.platonic.pulsar.persist;

import fun.platonic.pulsar.common.config.MutableConfig;
import org.apache.avro.util.Utf8;
import org.apache.gora.query.Result;
import org.apache.gora.store.DataStore;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import fun.platonic.pulsar.persist.gora.GoraStorage;
import fun.platonic.pulsar.persist.gora.generated.GWebPage;
import fun.platonic.pulsar.persist.metadata.Mark;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static fun.platonic.pulsar.common.config.PulsarConstants.SHORTEST_VALID_URL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static fun.platonic.pulsar.persist.metadata.Name.CASH_KEY;

/**
 * Tests basic Gora functionality by writing and reading webpages.
 */
public class TestGoraStorageInMemory {

    protected MutableConfig conf;
    protected FileSystem fs;
    protected Path testdir = new Path("/tmp/pulsar/test/working");
    protected DataStore<String, GWebPage> datastore;
    protected boolean persistentDataStore = false;

    private static void readWriteGoraWebPage(String id, DataStore<String, GWebPage> store) throws Exception {
        GWebPage page = GWebPage.newBuilder().build();

        int max = 500;
        for (int i = 0; i < max; i++) {
            // store a page with title
            String key = "key-" + id + "-" + i;
            String title = "title" + i;
            page.setPageTitle(new Utf8(title));
            store.put(key, page);
            store.flush();

            // retrieve page and check title
            page = store.get(key);
            assertNotNull(page);
            assertEquals(title, page.getPageTitle().toString());
        }

        // scan over the rows
        Result<String, GWebPage> result = store.execute(store.newQuery());
        int count = 0;
        while (result.next()) {
            try {
                // only count keys in the store for the current id
                if (result.getKey().contains(id))
                    ++count;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // check amount
        assertEquals(max, count);
    }

    private static void readWriteWebPage(String id, DataStore<String, GWebPage> store) throws Exception {
        int max = 1000;

        for (int i = 0; i < max; i++) {
            String url = SHORTEST_VALID_URL + "/" + id + "/" + i;
            WebPage page = WebPage.newWebPage(url);

            page.setBaseUrl(url);
            page.setPageText("text");
            page.setDistance(0);
            page.getHeaders().put("header1", "header1");
            page.getMarks().put(Mark.FETCH, "mark1");
            page.getMetadata().set(CASH_KEY, "metadata1");
            page.getInlinks().put("http://www.a.com/1", "");
            page.getInlinks().put("http://www.a.com/2", "");

            store.put(url, page.unbox());
            store.flush();

            // retrieve page and check title
            GWebPage goraPage = store.get(url);
            assertNotNull(goraPage);

            page = WebPage.box(url, goraPage);
            assertEquals("text", page.getPageText());
            assertEquals(0, page.getDistance());
            assertEquals("header1", page.getHeaders().get("header1"));
            // assertNotEquals("mark1", page.getMark(Mark.FETCH));
            assertEquals(new Utf8("mark1"), page.getMarks().get(Mark.FETCH));
            assertEquals("metadata1", page.getMetadata().getOrDefault(CASH_KEY, ""));
            assertEquals(2, page.getInlinks().size());
        }

        // scan over the rows
        Result<String, GWebPage> result = store.execute(store.newQuery());
        int count = 0;
        while (result.next()) {
            try {
                // only count keys in the store for the current id
                if (result.getKey().contains(id)) {
                    ++count;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // check amount
        assertEquals(max, count);
    }

    public static void main(String[] args) throws Exception {
        MutableConfig conf = new MutableConfig();

        DataStore<String, GWebPage> store = GoraStorage.createDataStore(conf.unbox(), String.class, GWebPage.class);
        readWriteGoraWebPage("test1", store);
        readWriteWebPage("test2", store);
        System.out.println("Done.");
    }

    @Before
    public void setUp() throws Exception {
        conf = new MutableConfig();
        conf.set("storage.data.store.class", "org.apache.gora.memory.store.MemStore");
        // conf.set("storage.data.store.class", "org.apache.gora.sql.store.SqlStore");
        fs = FileSystem.get(conf.unbox());
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
        fs.delete(testdir, true);
    }

    /**
     * Sequentially read and write pages to a store.
     *
     * @throws Exception
     */
    @Test
    // @Ignore("GORA-326 Removal of _g_dirty field from _ALL_FIELDS array and Field Enum in Persistent classes")
    public void testSinglethreaded() throws Exception {
        String id = "singlethread";
        readWriteGoraWebPage(id, datastore);
        readWriteWebPage(id, datastore);
    }

    /**
     * Tests multiple thread reading and writing to the same store, this should be
     * no problem because {@link DataStore} implementations claim to be thread
     * safe.
     *
     * @throws Exception
     */
    @Test
    public void testMultithreaded() throws Exception {
        // create a fixed thread pool
        int numThreads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(numThreads);

        // define a list of tasks
        Collection<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < numThreads; i++) {
            tasks.add(() -> {
                try {
                    // run a sequence
                    readWriteGoraWebPage(Thread.currentThread().getName(), datastore);
                    return 0;
                } catch (Exception e) {
                    e.printStackTrace();
                    return 1;
                }
            });
        }

        // submit them at once
        List<Future<Integer>> results = pool.invokeAll(tasks);

        // check results
        for (Future<Integer> result : results) {
            assertEquals(0, (int) result.get());
        }
    }
}
