package fun.platonic.pulsar.filter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.nio.file.Paths;

import static fun.platonic.pulsar.filter.DomainUrlFilter.PARAM_URLFILTER_DOMAIN_FILE;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Created by vincent on 17-2-23.
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class TestDomainUrlFilter extends UrlFilterTestBase {

    private final static String SAMPLES_DIR = System.getProperty("test.data", ".");

    @Test
    public void testDomainFilter() throws Exception {
        String domainFile = Paths.get(SAMPLES_DIR, "domain", "data", "general", "hosts.txt").toString();
        conf.set(PARAM_URLFILTER_DOMAIN_FILE, domainFile);
        DomainUrlFilter domainFilter = new DomainUrlFilter(conf);

        assertNotNull(domainFilter.filter("http://lucene.apache.org"));
        assertNotNull(domainFilter.filter("http://hadoop.apache.org"));
        assertNotNull(domainFilter.filter("http://www.apache.org"));
        assertNull(domainFilter.filter("http://www.google.com"));
        assertNull(domainFilter.filter("http://mail.yahoo.com"));
        assertNotNull(domainFilter.filter("http://www.foobar.net"));
        assertNotNull(domainFilter.filter("http://www.foobas.net"));
        assertNotNull(domainFilter.filter("http://www.yahoo.com"));
        assertNotNull(domainFilter.filter("http://www.foobar.be"));
        assertNull(domainFilter.filter("http://www.adobe.com"));
    }

    @Test
    public void testNoDomainAllowedFilter() throws Exception {
        String domainFile = Paths.get(SAMPLES_DIR, "domain", "data", "none", "hosts.txt").toString();
        conf.set(PARAM_URLFILTER_DOMAIN_FILE, domainFile);
        DomainUrlFilter domainFilter = new DomainUrlFilter(conf);

        assertNull(domainFilter.filter("http://lucene.apache.org"));
        assertNull(domainFilter.filter("http://hadoop.apache.org"));
        assertNull(domainFilter.filter("http://www.apache.org"));
        assertNull(domainFilter.filter("http://www.google.com"));
        assertNull(domainFilter.filter("http://mail.yahoo.com"));
        assertNull(domainFilter.filter("http://www.foobar.net"));
        assertNull(domainFilter.filter("http://www.foobas.net"));
        assertNull(domainFilter.filter("http://www.yahoo.com"));
        assertNull(domainFilter.filter("http://www.foobar.be"));
        assertNull(domainFilter.filter("http://www.adobe.com"));
    }
}
