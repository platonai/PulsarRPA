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

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
class TestUrlValidator : UrlFilterTestBase() {
    private var validUrl: String? = null
    private var invalidUrl: String? = null
    private val preUrl = "http://example."
    @Before
    @Throws(Exception::class)
    fun setUp() {
        tldLength = conf.getInt("urlfilter.tld.length", 8)
    }

    @Test
    fun testFilter() {
        val urlValidator = UrlValidator(conf)
        validUrl = generateValidTld(tldLength)
        invalidUrl = generateInvalidTld(tldLength)
        Assert.assertNotNull(urlValidator)
        // invalid urls
//        Assert.assertNull("Filtering on a null object should return null",
//                urlValidator.filter(null))
        Assert.assertNull("Invalid url: example.com/file[/].html",
                urlValidator.filter("example.com/file[/].html"))
        Assert.assertNull("Invalid url: http://www.example.com/space here.html",
                urlValidator.filter("http://www.example.com/space here.html"))
        Assert.assertNull("Invalid url: /main.html", urlValidator.filter("/main.html"))
        Assert.assertNull("Invalid url: www.example.com/main.html",
                urlValidator.filter("www.example.com/main.html"))
        Assert.assertNull("Invalid url: ftp:www.example.com/main.html",
                urlValidator.filter("ftp:www.example.com/main.html"))
        Assert.assertNull("Inalid url: http://999.000.456.32/pulsar/trunk/README.txt",
                urlValidator.filter("http://999.000.456.32/pulsar/trunk/README.txt"))
        Assert.assertNull("Invalid url: http://www.example.com/ma|in\\toc.html",
                urlValidator.filter(" http://www.example.com/ma|in\\toc.html"))
        // test tld limit
        Assert.assertNull("InValid url: $invalidUrl", urlValidator.filter(invalidUrl!!))
        // valid urls
        Assert.assertNotNull("Valid url: https://issues.apache.org/jira/PULSAR-1127",
                urlValidator.filter("https://issues.apache.org/jira/PULSAR-1127"))
        Assert.assertNotNull(
                "Valid url: http://domain.tld/function.cgi?url=http://fonzi.com/&amp;name=Fonzi&amp;mood=happy&amp;coat=leather",
                urlValidator
                        .filter("http://domain.tld/function.cgi?url=http://fonzi.com/&amp;name=Fonzi&amp;mood=happy&amp;coat=leather"))
        Assert.assertNotNull(
                "Valid url: http://validator.w3.org/feed/check.cgi?url=http%3A%2F%2Ffeeds.feedburner.com%2Fperishablepress",
                urlValidator
                        .filter("http://validator.w3.org/feed/check.cgi?url=http%3A%2F%2Ffeeds.feedburner.com%2Fperishablepress"))
        Assert.assertNotNull("Valid url: ftp://alfa.bravo.pi/foo/bar/plan.pdf",
                urlValidator.filter("ftp://alfa.bravo.pi/mike/check/plan.pdf"))
        // test tld limit
        Assert.assertNotNull("Valid url: $validUrl", urlValidator.filter(validUrl!!))
    }

    /**
     * Generate Sample of Valid Tld.
     */
    fun generateValidTld(length: Int): String {
        val buffer = StringBuilder()
        for (i in 1..length) {
            val c = ('a'.toDouble() + Math.random() * 26).toChar()
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
            val c = ('a'.toDouble() + Math.random() * 26).toChar()
            buffer.append(c)
        }
        return preUrl + buffer.toString()
    }

    companion object {
        private var tldLength = 0
    }
}
