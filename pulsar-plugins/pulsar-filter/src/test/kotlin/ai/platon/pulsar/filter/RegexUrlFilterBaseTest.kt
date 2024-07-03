/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.platon.pulsar.filter

import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.common.stringify
import ai.platon.pulsar.skeleton.crawl.filter.CrawlUrlFilter
import org.junit.Assert
import java.io.BufferedReader
import java.io.Reader
import java.util.function.Consumer
import kotlin.streams.toList

// JDK imports
abstract class RegexUrlFilterBaseTest(testDir: String) : UrlFilterTestBase(testDir) {

    protected abstract fun getURLFilter(reader: Reader): CrawlUrlFilter

    protected fun bench(loops: Int, file: String) {
        try {
            val rulesReader = ResourceLoader.getResourceAsReader("$testResourcePrefix/$file.rules")!!
            val urlReader = ResourceLoader.getResourceAsReader("$testResourcePrefix/$file.urls")!!
            bench(loops, rulesReader, urlReader)
        } catch (e: Exception) {
            Assert.fail(e.toString())
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
            Assert.fail(e.toString())
        }

        LOG.info("Bench time (" + loops + ") " + (System.currentTimeMillis() - start) + "ms")
    }

    protected fun test(file: String) {
        try {
            val rulesReader = ResourceLoader.getResourceAsReader("$testResourcePrefix/$file.rules")!!
            val urlReader = ResourceLoader.getResourceAsReader("$testResourcePrefix/$file.urls")!!
            test(rulesReader, urlReader)
        } catch (e: Exception) {
            Assert.fail(e.toString())
        }
    }

    protected fun test(reader: Reader, urls: Reader) {
        try {
            test(getURLFilter(reader), readURLFile(urls))
        } catch (e: Exception) {
            Assert.fail(e.stringify())
        }
    }

    protected fun test(filter: CrawlUrlFilter, expected: List<FilteredURL>) {
        expected.forEach(Consumer { url: FilteredURL ->
            val result = filter.filter(url.url)
            if (result != null) {
                Assert.assertTrue(url.url, url.sign)
            } else {
                Assert.assertFalse(url.url, url.sign)
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
