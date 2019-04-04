package ai.platon.pulsar.crawl.parse;

import ai.platon.pulsar.common.Urls;
import ai.platon.pulsar.common.config.MutableConfig;
import ai.platon.pulsar.crawl.filter.CrawlFilters;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Ignore
public class TestCrawlFilter {

    MutableConfig conf;
    private String[] detailUrls = {
            "http://mall.jumei.com/product_200918.html?from=store_lancome_list_items_7_4"
    };
    private CrawlFilters crawlFilters;

    @Before
    public void setUp() throws IOException {
        String crawlFilterRules = "{}";
        conf = new MutableConfig();
        conf.set(CrawlFilters.CRAWL_FILTER_RULES, crawlFilterRules);
        crawlFilters = new CrawlFilters(conf);
    }

    @Test
    public void testKeyRange() throws MalformedURLException {
        Map<String, String> keyRange = crawlFilters.getReversedKeyRanges();

        System.out.println(keyRange);

        assertTrue(keyRange.get("com.jumei.mall:http").equals("com.jumei.mall:http/\uFFFF"));
        assertTrue(keyRange.get("com.jumei.lancome:http/search.html").equals("com.jumei.lancome:http/search.html\uFFFF"));
        assertFalse(keyRange.get("com.jumei.lancome:http/search.html").equals("com.jumei.lancome:http/search.html\\uFFFF"));

        for (String detailUrl : detailUrls) {
            assertTrue(crawlFilters.testKeyRangeSatisfied(Urls.reverseUrl(detailUrl)));
        }
    }

    @Test
    public void testMaxKeyRange() throws MalformedURLException {
        String[] keyRange = crawlFilters.getMaxReversedKeyRange();
        System.out.println(keyRange[0] + ", " + keyRange[1]);

        assertTrue('\uFFFF' - 'a' == 65438);

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
