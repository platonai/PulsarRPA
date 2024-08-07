
package ai.platon.pulsar.filter

import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.common.stringify
import ai.platon.pulsar.skeleton.crawl.filter.CrawlUrlFilter

import java.io.BufferedReader
import java.io.Reader
import java.util.function.Consumer
import kotlin.streams.toList
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail


abstract class RegexUrlFilterBaseTest(testDir: String) : UrlFilterTestBase(testDir) {

    protected abstract fun getURLFilter(reader: Reader): CrawlUrlFilter

    protected fun bench(loops: Int, file: String) {
        try {
            val rulesReader = ResourceLoader.getResourceAsReader("$testResourcePrefix/$file.rules")!!
            val urlReader = ResourceLoader.getResourceAsReader("$testResourcePrefix/$file.urls")!!
            bench(loops, rulesReader, urlReader)
        } catch (e: Exception) {
            fail(e.toString())
        }
    }

    protected fun bench(loops: Int, reader: Reader, urls: Reader) {
        val start = System.currentTimeMillis()
        try {
            val filter = getURLFilter(reader)
            val expected = readURLFile(urls)
            for (i in 0 until loops) {
                test(filter, expected)
            }
        } catch (e: Exception) {
            fail(e.toString())
        }

        LOG.info("Bench time (" + loops + ") " + (System.currentTimeMillis() - start) + "ms")
    }

    protected fun test(file: String) {
        try {
            val rulesReader = ResourceLoader.getResourceAsReader("$testResourcePrefix/$file.rules")!!
            val urlReader = ResourceLoader.getResourceAsReader("$testResourcePrefix/$file.urls")!!
            test(rulesReader, urlReader)
        } catch (e: Exception) {
            fail(e.toString())
        }
    }

    protected fun test(reader: Reader, urls: Reader) {
        try {
            test(getURLFilter(reader), readURLFile(urls))
        } catch (e: Exception) {
            fail(e.stringify())
        }
    }

    protected fun test(filter: CrawlUrlFilter, expected: List<FilteredURL>) {
        expected.forEach(Consumer { url: FilteredURL ->
            val result = filter.filter(url.url)
            if (result != null) {
                assertTrue(url.sign, url.url)
            } else {
                assertFalse(url.sign, url.url)
            }
        })
    }

    class FilteredURL(line: String) {
        var sign = false
        var url: String

        init {
            when (line[0]) {
                '+' -> sign = true
                '-' -> sign = false
                else -> {
                }
            }
            url = line.substring(1)
        }
    }

    companion object {
        private fun readURLFile(reader: Reader): List<FilteredURL> {
            return BufferedReader(reader).lines().map { line: String -> FilteredURL(line) }.toList()
        }
    }
}
