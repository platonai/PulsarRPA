package ai.platon.pulsar.filter

import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.common.config.MutableConfig
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Created by vincent on 17-2-23.
 */
@RunWith(SpringRunner::class)
class TestDomainUrlFilter : UrlFilterTestBase("") {
    override var conf: MutableConfig = MutableConfig()

    @Test
    fun testDomainFilter() {
        val rules = ResourceLoader.readString("domain/data/general/hosts.txt")
        conf[DomainUrlFilter.PARAM_URLFILTER_DOMAIN_RULES] = rules

        val domainFilter = DomainUrlFilter(conf)

//        domainFilter.domainSet.forEach { println() }
//        println(conf[DomainUrlFilter.PARAM_URLFILTER_DOMAIN_RULES])
        assertTrue { domainFilter.domainSet.contains("apache.org") }

        assertNotNull(domainFilter.filter("http://lucene.apache.org"))
        assertNotNull(domainFilter.filter("http://hadoop.apache.org"))
        assertNotNull(domainFilter.filter("http://www.apache.org"))
        assertNull(domainFilter.filter("http://www.google.com"))
        assertNull(domainFilter.filter("http://mail.yahoo.com"))
        assertNotNull(domainFilter.filter("http://www.foobar.net"))
        assertNotNull(domainFilter.filter("http://www.foobas.net"))
        assertNotNull(domainFilter.filter("http://www.yahoo.com"))
        assertNotNull(domainFilter.filter("http://www.foobar.be"))
        assertNull(domainFilter.filter("http://www.adobe.com"))
    }

    @Test
    fun testNoDomainAllowedFilter() {
        val rules = ResourceLoader.readString("domain/data/none/hosts.txt")
        conf[DomainUrlFilter.PARAM_URLFILTER_DOMAIN_RULES] = rules

        val domainFilter = DomainUrlFilter(conf)

        assertNull(domainFilter.filter("http://lucene.apache.org"))
        assertNull(domainFilter.filter("http://hadoop.apache.org"))
        assertNull(domainFilter.filter("http://www.apache.org"))
        assertNull(domainFilter.filter("http://www.google.com"))
        assertNull(domainFilter.filter("http://mail.yahoo.com"))
        assertNull(domainFilter.filter("http://www.foobar.net"))
        assertNull(domainFilter.filter("http://www.foobas.net"))
        assertNull(domainFilter.filter("http://www.yahoo.com"))
        assertNull(domainFilter.filter("http://www.foobar.be"))
        assertNull(domainFilter.filter("http://www.adobe.com"))
    }
}
