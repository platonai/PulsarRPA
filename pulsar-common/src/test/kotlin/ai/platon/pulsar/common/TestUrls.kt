/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.platon.pulsar.common

import ai.platon.pulsar.common.url.Urls
import ai.platon.pulsar.common.url.Urls.reverseUrl
import ai.platon.pulsar.common.url.Urls.unreverseUrl
import org.junit.Assert
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

typealias uc = UrlCommon

class TestUrls {
    @Test
    @Throws(Exception::class)
    fun testReverseUrl() {
        println(reverseUrl("http://sz.sxrb.com/sxxww/dspd/szpd/wsjk"))
        println(reverseUrl("http://sz.sxrb.com/sxxww/"))

        assertReverse(uc.urlString1, uc.reversedUrlString1)
        assertReverse(uc.urlString2, uc.reversedUrlString2)
        assertReverse(uc.urlString3, uc.reversedUrlString3)
        assertReverse(uc.urlString4, uc.reversedUrlString4)
        assertReverse(uc.urlString5, uc.reversedUrlString5)
        assertReverse(uc.urlString5, uc.reversedUrlString5)
        assertReverse(uc.urlString6, uc.reversedUrlString6)
        assertReverse(uc.urlString7, uc.reversedUrlString7)
        assertReverse(uc.urlString8, uc.reversedUrlString8)
    }

    @Test
    @Throws(Exception::class)
    fun testUnreverseUrl() {
        println(unreverseUrl("com.sxrb.www:http/sxxww/zthj/xmtdt/6619357.shtml"))
        assertUnreverse(uc.reversedUrlString1, uc.urlString1)
        assertUnreverse(uc.reversedUrlString2, uc.urlString2)
        assertUnreverse(uc.reversedUrlString3, uc.urlString3)
        assertUnreverse(uc.reversedUrlString4, uc.urlString4)
        assertUnreverse(uc.reversedUrlString5, uc.urlString5rev)
        assertUnreverse(uc.reversedUrlString6, uc.urlString6)
        assertUnreverse(uc.reversedUrlString7, uc.urlString7)
    }

    @Test
    @Throws(Exception::class)
    fun testRemoveParameters() {
        val url = "https://www.amazon.com/s?k=sleep&i=amazonfresh&bbn=10329849011&page=2&qid=1609388361&ref=sr_pg_2"
        val stripedUrl = "https://www.amazon.com/s?k=sleep&i=amazonfresh"

        assertTrue { "10329849011" !in Urls.removeQueryParameters(url, "bbn") }
        assertTrue { "page" !in Urls.removeQueryParameters(url, "page") }

        assertEquals(stripedUrl, Urls.removeQueryParameters(url, "bbn", "page", "ref", "qid"))
    }

    @Test
    @Throws(Exception::class)
    fun testKeepParameters() {
        val url = "https://www.amazon.com/s?k=sleep&i=amazonfresh&bbn=10329849011&page=2&qid=1609388361&ref=sr_pg_2"
        val stripedUrl = "https://www.amazon.com/s?k=sleep&i=amazonfresh"

        assertTrue { "10329849011" in Urls.keepQueryParameters(url, "bbn") }

        assertTrue { "page" in Urls.keepQueryParameters(url, "page") }
        assertTrue { "bbn" !in Urls.keepQueryParameters(url, "page") }

        assertEquals(stripedUrl, Urls.keepQueryParameters(url, "k", "i"))
    }

    companion object {
        @Throws(Exception::class)
        private fun assertReverse(url: String, expectedReversedUrl: String) {
            val reversed = reverseUrl(url)
            Assert.assertEquals(expectedReversedUrl, reversed)
        }

        private fun assertUnreverse(reversedUrl: String, expectedUrl: String) {
            val unreversed = unreverseUrl(reversedUrl)
            Assert.assertEquals(expectedUrl, unreversed)
        }
    }
}