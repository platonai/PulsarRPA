package fun.platonic.pulsar.crawl.common;

import com.google.common.collect.Lists;
import fun.platonic.pulsar.common.DateTimeUtil;
import fun.platonic.pulsar.common.MetricsSystem;
import fun.platonic.pulsar.common.UrlUtil;
import fun.platonic.pulsar.common.WeakPageIndexer;
import fun.platonic.pulsar.common.config.MutableConfig;
import fun.platonic.pulsar.crawl.fetch.TaskStatusTracker;
import fun.platonic.pulsar.persist.HypeLink;
import fun.platonic.pulsar.persist.WebPage;
import fun.platonic.pulsar.persist.gora.db.WebDb;
import fun.platonic.pulsar.persist.gora.generated.GHypeLink;
import fun.platonic.pulsar.persist.gora.generated.GWebPage;
import org.apache.avro.util.Utf8;
import org.apache.gora.persistency.impl.DirtyCollectionWrapper;
import org.apache.gora.persistency.impl.DirtyListWrapper;
import org.apache.gora.store.DataStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static fun.platonic.pulsar.common.PulsarConstants.EXAMPLE_URL;
import static fun.platonic.pulsar.common.PulsarConstants.URL_TRACKER_HOME_URL;
import static fun.platonic.pulsar.common.config.CapabilityTypes.CRAWL_ID;
import static org.junit.Assert.*;

/**
 * Created by vincent on 16-7-20.
 * Copyright @ 2013-2016 Warpspeed Information. All rights reserved
 */
public class TestStorage {
    public static final Logger LOG = LoggerFactory.getLogger(TestStorage.class);

    private MutableConfig conf;
    private WebDb webDb;
    private MetricsSystem metricsSystem;
    private WeakPageIndexer urlTrackerIndexer;
    private TaskStatusTracker taskStatusTracker;
    private DataStore<String, GWebPage> store;

    private List<CharSequence> exampleUrls = IntStream.range(10000, 10050)
            .mapToObj(i -> EXAMPLE_URL + "/" + i)
            .collect(Collectors.toList());

    private String exampleUrl;

    public TestStorage() {
        conf = new MutableConfig();
        conf.set(CRAWL_ID, "test");

        webDb = new WebDb(conf);
        metricsSystem = new MetricsSystem(webDb, conf);
        taskStatusTracker = new TaskStatusTracker(webDb, metricsSystem, conf);
        urlTrackerIndexer = new WeakPageIndexer(URL_TRACKER_HOME_URL, webDb);
        store = webDb.getStore();
    }

    @Before
    public void setup() {
        exampleUrl = EXAMPLE_URL + "/" + DateTimeUtil.format(Instant.now(), "MMdd");
    }

    @After
    public void teardown() {
        // webDb.delete(exampleUrl);
        webDb.flush();

        LOG.debug("In shell: \nget '{}', '{}'", store.getSchemaName(), UrlUtil.reverseUrlOrEmpty(exampleUrl));
    }

    @Test
    public void testWebDb() {
        String url = EXAMPLE_URL + "/" + Instant.now().toEpochMilli();
        WebPage page = WebPage.newInternalPage(url);
        assertEquals(url, page.getUrl());

        // webDb.put(page.getUrl(), page, true);
        webDb.put(page.getUrl(), page);
        webDb.flush();

        page = webDb.getOrNil(url);
        WebPage page2 = webDb.getOrNil(url);
        assertEquals(page.getUrl(), page2.getUrl());
        assertEquals(page.getContentAsString(), page2.getContentAsString());

        assertTrue(page.isNotNil());
        assertTrue(page.isInternal());

        page.addLinks(exampleUrls);
        webDb.put(url, page);
        webDb.flush();
        WebPage page3 = webDb.getOrNil(url);
        assertEquals(exampleUrls.size(), page3.getLinks().size());

        page.addLinks(exampleUrls);
        webDb.put(url, page);
        webDb.flush();
        WebPage page4 = webDb.getOrNil(url);
        assertEquals(exampleUrls.size(), page4.getLinks().size());

        webDb.delete(url);
        webDb.flush();
        page = webDb.getOrNil(url);
        assertTrue(page.isNil());

        webDb.delete(url);
    }

    @Test
    public void testModifyNestedSimpleArray() {
        String key = UrlUtil.reverseUrlOrEmpty(exampleUrl);
        GWebPage page = store.get(key);
        assertNotNull(page);

        int i = 0;
        assertTrue(page.getLinks().get(i) instanceof Utf8);
        String modifiedLink = EXAMPLE_URL + "/" + "0-modified";
        page.getLinks().set(i, modifiedLink);
        store.put(key, page);
        store.flush();
        page = store.get(key);
        assertNotNull(page);
        assertEquals(modifiedLink, page.getLinks().get(i).toString());

        i = 1;
        page.getLinks().set(i, new Utf8());
        store.put(key, page);
        store.flush();
        page = store.get(key);
        assertNotNull(page);
        assertEquals("", page.getLinks().get(i).toString());

        i = 2;
        page.getLinks().set(i, "");
        store.put(key, page);
        store.flush();
        page = store.get(key);
        assertNotNull(page);
        assertEquals("", page.getLinks().get(i).toString());

        i = 3;
        page.getLinks().set(i, new Utf8(""));
        store.put(key, page);
        store.flush();
        page = store.get(key);
        assertNotNull(page);
        assertEquals("", page.getLinks().get(i).toString());
    }

    /**
     * TODO: We can not clear an array, HBase keeps unchanged
     */
    @Test
    public void testClearNestedSimpleArray() {
        String key = UrlUtil.reverseUrlOrEmpty(exampleUrl);
        GWebPage page = store.get(key);
        assertNotNull(page);

        assertTrue(page.getLinks().get(0) instanceof Utf8);
        page.getLinks().clear();
        assertTrue(page.getLinks().isEmpty());
        assertTrue(page.isLinksDirty());

        assertTrue(page.getLinks() instanceof DirtyCollectionWrapper);
        DirtyCollectionWrapper wrapper = (DirtyCollectionWrapper) page.getLinks();
        assertTrue(wrapper.isDirty());
        assertTrue(wrapper.isEmpty());

        DirtyListWrapper<CharSequence> links =
                new DirtyListWrapper<>(Lists.newArrayList(EXAMPLE_URL + "/-1", EXAMPLE_URL + "/-2", EXAMPLE_URL + "/1000000"));
        page.setLinks(links);

        store.put(key, page);
        store.flush();

        page = store.get(key);
        assertNotNull(page);
        assertEquals(2, page.getLinks().size());
    }

    /**
     * TODO: We can not clear an array, HBase keeps unchanged
     */
    @Test
    public void testUpdateNestedComplexArray() {
        String key = UrlUtil.reverseUrlOrEmpty(exampleUrl);
        GWebPage page = store.get(key);
        assertNotNull(page);

        assertTrue(page.getLiveLinks().get(0).getAnchor() instanceof Utf8);
        page.getLiveLinks().clear();
        assertTrue(page.getLiveLinks().isEmpty());

        store.put(key, page);
        store.flush();

        page = store.get(key);
        assertNotNull(page);
        assertEquals(0, page.getLiveLinks().size());
    }

    @Test
    public void testUpdateNestedArray2() {
        WebPage page = webDb.getOrNil(exampleUrl);
        page.setLinks(new ArrayList<>());
        // page.getLinks().clear();
        assertTrue(page.getLinks().isEmpty());
        assertTrue(page.unbox().isDirty());

        page.getLinks().add(EXAMPLE_URL);
        page.getLinks().add(EXAMPLE_URL + "/1");
        webDb.put(page.getUrl(), page);
        webDb.flush();

        page = webDb.getOrNil(exampleUrl);
        assertTrue(page.isNotNil());
        assertEquals(2, page.getLinks().size());
    }

    @Test
    public void testUpdateNestedMap() {
        WebPage page = webDb.getOrNil(exampleUrl);
        page.getInlinks().clear();
        assertTrue(page.getInlinks().isEmpty());
        webDb.put(page.getUrl(), page);
        webDb.flush();

        page = webDb.getOrNil(exampleUrl);
        assertTrue(page.isNotNil());
        assertTrue(page.getInlinks().isEmpty());
    }

    @Test
    public void testWeakPageIndexer() {
        final int pageNo = 1;
        final String indexPageUrl = URL_TRACKER_HOME_URL + "/" + pageNo;
        webDb.delete(indexPageUrl);
        webDb.flush();

        urlTrackerIndexer.indexAll(exampleUrls);
        urlTrackerIndexer.commit();

        WebPage page = webDb.getOrNil(indexPageUrl);
        assertTrue(page.isNotNil());
        assertTrue(page.isInternal());
        assertEquals(exampleUrls.size(), page.getLiveLinks().size());

        urlTrackerIndexer.takeAll(pageNo);
        page = webDb.getOrNil(indexPageUrl);
        assertTrue(page.isNotNil());
        assertTrue(page.getLiveLinks().isEmpty());
    }

    public WebPage createExamplePage() {
        webDb.delete(exampleUrl);
        webDb.flush();

        LOG.debug("Random url: " + exampleUrl);

        WebPage page = WebPage.newWebPage(exampleUrl);
        for (int i = 1; i < 20; ++i) {
            String url = EXAMPLE_URL + "/" + i;
            String url2 = EXAMPLE_URL + "/" + (i - 1);

            GHypeLink link = HypeLink.parse(url2).unbox();
            page.getLiveLinks().put(link.getUrl(), link);
            page.getLinks().add(url2);

            page.getInlinks().put(url, url2);
        }
        webDb.put(exampleUrl, page);
        webDb.flush();

        return page;
    }
}
