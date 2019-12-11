package ai.platon.pulsar.crawl.parse;

import ai.platon.pulsar.common.Urls;
import ai.platon.pulsar.common.config.MutableConfig;
import ai.platon.pulsar.crawl.filter.CrawlFilters;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;

import static org.junit.Assert.*;

@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/test-context/filter-beans.xml"})
public class TestCrawlFilter {

    private String[] detailUrls = {
            "http://mall.jumei.com/product_200918.html?from=store_lancome_list_items_7_4"
    };

    @Autowired
    MutableConfig conf;
    @Autowired
    private CrawlFilters crawlFilters;

    @Before
    public void setUp() throws IOException {
        String crawlFilterRules = "{}";
        conf.set(CrawlFilters.CRAWL_FILTER_RULES, crawlFilterRules);
    }

    @Test
    public void testKeyRange() throws MalformedURLException {
        Map<String, String> keyRange = crawlFilters.getReversedKeyRanges();

        System.out.println(keyRange);

        assertEquals("com.jumei.mall:http/\uFFFF", keyRange.get("com.jumei.mall:http"));
        assertEquals("com.jumei.lancome:http/search.html\uFFFF", keyRange.get("com.jumei.lancome:http/search.html"));
        assertNotEquals("com.jumei.lancome:http/search.html\\uFFFF", keyRange.get("com.jumei.lancome:http/search.html"));

        for (String detailUrl : detailUrls) {
            assertTrue(crawlFilters.testKeyRangeSatisfied(Urls.reverseUrl(detailUrl)));
        }
    }

    @Test
    public void testMaxKeyRange() throws MalformedURLException {
        String[] keyRange = crawlFilters.getMaxReversedKeyRange();
        System.out.println(keyRange[0] + ", " + keyRange[1]);

        assertEquals(65438, '\uFFFF' - 'a');

        for (String detailUrl : detailUrls) {
            detailUrl = Urls.reverseUrl(detailUrl);

            assertTrue("com.jumei.lancome:http/search.html".compareTo(detailUrl) < 0);
            assertTrue("com.jumei.mall:http/\uFFFF".compareTo(detailUrl) > 0);
            // Note : \uFFFF, not \\uFFFF
            assertFalse("com.jumei.mall:http/\\uFFFF".compareTo(detailUrl) > 0);
        }

        for (String detailUrl : detailUrls) {
            detailUrl = Urls.reverseUrl(detailUrl);
            assertTrue(keyRange[0].compareTo(detailUrl) < 0);
            assertTrue(keyRange[1].compareTo(detailUrl) > 0);
        }
    }
}
