package ai.platon.pulsar.filter

import ai.platon.pulsar.crawl.filter.UrlFilter
import org.junit.Test
import java.io.Reader
import java.nio.file.Paths

/**
 * Created by vincent on 17-2-23.
 */
class TestAutomatonUrlFilter : RegexUrlFilterBaseTest("automaton/sample") {

    override fun getURLFilter(reader: Reader): UrlFilter {
        return AutomatonUrlFilter(reader, conf)
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
