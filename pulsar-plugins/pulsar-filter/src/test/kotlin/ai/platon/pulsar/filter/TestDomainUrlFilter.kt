package ai.platon.pulsar.filter

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import java.nio.file.Paths

/**
 * Created by vincent on 17-2-23.
 */
@RunWith(SpringJUnit4ClassRunner::class)
class TestDomainUrlFilter : UrlFilterTestBase() {
    @Test
    @Throws(Exception::class)
    fun testDomainFilter() {
        val domainFile = Paths.get(SAMPLES_DIR, "domain", "data", "general", "hosts.txt").toString()
        conf[DomainUrlFilter.PARAM_URLFILTER_DOMAIN_FILE] = domainFile
        val domainFilter = DomainUrlFilter(conf)
        Assert.assertNotNull(domainFilter.filter("http://lucene.apache.org"))
        Assert.assertNotNull(domainFilter.filter("http://hadoop.apache.org"))
        Assert.assertNotNull(domainFilter.filter("http://www.apache.org"))
        Assert.assertNull(domainFilter.filter("http://www.google.com"))
        Assert.assertNull(domainFilter.filter("http://mail.yahoo.com"))
        Assert.assertNotNull(domainFilter.filter("http://www.foobar.net"))
        Assert.assertNotNull(domainFilter.filter("http://www.foobas.net"))
        Assert.assertNotNull(domainFilter.filter("http://www.yahoo.com"))
        Assert.assertNotNull(domainFilter.filter("http://www.foobar.be"))
        Assert.assertNull(domainFilter.filter("http://www.adobe.com"))
    }

    @Test
    @Throws(Exception::class)
    fun testNoDomainAllowedFilter() {
        val domainFile = Paths.get(SAMPLES_DIR, "domain", "data", "none", "hosts.txt").toString()
        conf[DomainUrlFilter.PARAM_URLFILTER_DOMAIN_FILE] = domainFile
        val domainFilter = DomainUrlFilter(conf)
        Assert.assertNull(domainFilter.filter("http://lucene.apache.org"))
        Assert.assertNull(domainFilter.filter("http://hadoop.apache.org"))
        Assert.assertNull(domainFilter.filter("http://www.apache.org"))
        Assert.assertNull(domainFilter.filter("http://www.google.com"))
        Assert.assertNull(domainFilter.filter("http://mail.yahoo.com"))
        Assert.assertNull(domainFilter.filter("http://www.foobar.net"))
        Assert.assertNull(domainFilter.filter("http://www.foobas.net"))
        Assert.assertNull(domainFilter.filter("http://www.yahoo.com"))
        Assert.assertNull(domainFilter.filter("http://www.foobar.be"))
        Assert.assertNull(domainFilter.filter("http://www.adobe.com"))
    }

    companion object {
        private val SAMPLES_DIR = System.getProperty("test.data", ".")
    }
}
