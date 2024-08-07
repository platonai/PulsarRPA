
package ai.platon.pulsar.filter

import ai.platon.pulsar.common.ResourceLoader.readAllLines
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import java.io.IOException
import java.time.ZoneId
import kotlin.test.*

/**
 * JUnit based test of class `RegexURLFilter`.
 *
 * @author Jrme Charron
 */
@ContextConfiguration(locations = ["classpath:/test-context/filter-beans.xml"])
class TestDateUrlFilter : UrlFilterTestBase("datedata") {
    @Autowired
    lateinit var dateUrlFilter: DateUrlFilter
    
    @BeforeTest
    @Throws(IOException::class)
    fun setUp() {
        dateUrlFilter = DateUrlFilter(ZoneId.systemDefault(), conf)
    }
    
    @Test
    fun testNotSupportedDateFormat() {
        val urls = readAllLines("datedata/urls_with_not_supported_old_date.txt")
        for (url in urls) {
            assertNotNull(dateUrlFilter.filter(url), url)
        }
    }
    
    @Test
    fun testDateTimeDetector() {
        val urls = readAllLines("datedata/urls_with_old_date.txt")
        for (url in urls) {
            assertNull(dateUrlFilter.filter(url), url)
        }
    }
}
