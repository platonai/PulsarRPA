/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.platon.pulsar.filter

import kotlin.test.*

class TestUrlValidator : UrlFilterTestBase() {
    private var validUrl: String? = null
    private var invalidUrl: String? = null
    private val preUrl = "http://example."

    @BeforeTest
    @Throws(Exception::class)
    fun setUp() {
        tldLength = conf.getInt("urlfilter.tld.length", 8)
    }

    @Test
    fun testFilter() {
        val urlValidator = UrlValidator(conf)
        validUrl = generateValidTld(tldLength)
        invalidUrl = generateInvalidTld(tldLength)
        assertNotNull(urlValidator)
        // invalid urls
//        assertNull("Filtering on a null object should return null",
//                urlValidator.filter(null))
        assertNull(urlValidator.filter("example.com/file[/].html"),
            "Invalid url: example.com/file[/].html")
        assertNull(urlValidator.filter("http://www.example.com/space here.html"),
            "Invalid url: http://www.example.com/space here.html")
        assertNull(urlValidator.filter("/main.html"), "Invalid url: /main.html")
        assertNull(urlValidator.filter("www.example.com/main.html"),
            "Invalid url: www.example.com/main.html")
        assertNull(urlValidator.filter("ftp:www.example.com/main.html"), "Invalid url: ftp:www.example.com/main.html")
        assertNull(urlValidator.filter("http://999.000.456.32/pulsar/trunk/README.txt"),
            "Invalid url: http://999.000.456.32/pulsar/trunk/README.txt")
        assertNull(urlValidator.filter(" http://www.example.com/ma|in\\toc.html"),
            "Invalid url: http://www.example.com/ma|in\\toc.html")
        // test tld limit
        assertNull(urlValidator.filter(invalidUrl!!), "InValid url: $invalidUrl")
        // valid urls
        assertNotNull(urlValidator.filter("https://issues.apache.org/jira/PULSAR-1127"),
            "Valid url: https://issues.apache.org/jira/PULSAR-1127")
        assertNotNull(urlValidator.filter("http://domain.tld/function.cgi?url=http://fonzi.com/&amp;name=Fonzi&amp;mood=happy&amp;coat=leather"),
                "Valid url: http://domain.tld/function.cgi?url=http://fonzi.com/&amp;name=Fonzi&amp;mood=happy&amp;coat=leather")
        assertNotNull(
            urlValidator
                .filter("http://validator.w3.org/feed/check.cgi?url=http%3A%2F%2Ffeeds.feedburner.com%2Fperishablepress"),
                "Valid url: http://validator.w3.org/feed/check.cgi?url=http%3A%2F%2Ffeeds.feedburner.com%2Fperishablepress")
        assertNotNull(
            urlValidator.filter("ftp://alfa.bravo.pi/mike/check/plan.pdf"), "Valid url: ftp://alfa.bravo.pi/foo/bar/plan.pdf")
        // test tld limit
        assertNotNull(urlValidator.filter(validUrl!!), "Valid url: $validUrl")
    }

    /**
     * Generate Sample of Valid Tld.
     */
    fun generateValidTld(length: Int): String {
        val buffer = StringBuilder()
        for (i in 1..length) {
            val c = ('a'.code.toDouble() + Math.random() * 26).toInt().toChar()
            buffer.append(c)
        }
        return preUrl + buffer.toString()
    }

    /**
     * Generate Sample of Invalid Tld. character
     */
    fun generateInvalidTld(length: Int): String {
        val buffer = StringBuilder()
        for (i in 1..length + 1) {
            val c = ('a'.code.toDouble() + Math.random() * 26).toInt().toChar()
            buffer.append(c)
        }
        return preUrl + buffer.toString()
    }

    companion object {
        private var tldLength = 0
    }
}
