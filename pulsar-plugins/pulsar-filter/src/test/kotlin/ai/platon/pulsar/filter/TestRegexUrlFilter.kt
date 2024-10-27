
package ai.platon.pulsar.filter

import ai.platon.pulsar.skeleton.crawl.filter.CrawlUrlFilter
import org.springframework.boot.test.context.SpringBootTest
import java.io.Reader
import kotlin.test.Test

/**
 * JUnit based test of class `RegexURLFilter`.
 *
 * @author Jrme Charron
 */
class TestRegexUrlFilter : RegexUrlFilterBaseTest("sample") {
    
    override fun getURLFilter(reader: Reader): CrawlUrlFilter {
        return RegexUrlFilter(reader, conf)
    }
    
    @Test
    fun test() {
        test("WholeWebCrawling")
        test("IntranetCrawling")
        bench(50, "Benchmarks")
        bench(100, "Benchmarks")
        bench(200, "Benchmarks")
        bench(400, "Benchmarks")
        bench(800, "Benchmarks")
    }
}
