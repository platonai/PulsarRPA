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
package ai.platon.pulsar.common.urls

import ai.platon.pulsar.common.urls.UrlUtils.reverseUrl
import ai.platon.pulsar.common.urls.UrlUtils.unreverseUrl
import org.junit.Assert
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

typealias uc = UrlCommon

class TestUrlUtils {

    @Test
    fun testReverseUrl() {
        println(reverseUrl("http://sz.sxrb.com/sxxww/dspd/szpd/wsjk"))
        println(reverseUrl("http://sz.sxrb.com/sxxww/"))

        assertReverse(UrlCommon.urlString1, UrlCommon.reversedUrlString1)
        assertReverse(UrlCommon.urlString2, UrlCommon.reversedUrlString2)
        assertReverse(UrlCommon.urlString3, UrlCommon.reversedUrlString3)
        assertReverse(UrlCommon.urlString4, UrlCommon.reversedUrlString4)
        assertReverse(UrlCommon.urlString5, UrlCommon.reversedUrlString5)
        assertReverse(UrlCommon.urlString5, UrlCommon.reversedUrlString5)
        assertReverse(UrlCommon.urlString6, UrlCommon.reversedUrlString6)
        assertReverse(UrlCommon.urlString7, UrlCommon.reversedUrlString7)
        assertReverse(UrlCommon.urlString8, UrlCommon.reversedUrlString8)
        assertReverse(UrlCommon.urlString9, UrlCommon.reversedUrlString9)
    }

    @Test
    fun testUnreverseUrl() {
        println(unreverseUrl("com.sxrb.www:http/sxxww/zthj/xmtdt/6619357.shtml"))
        assertUnreverse(UrlCommon.reversedUrlString1, UrlCommon.urlString1)
        assertUnreverse(UrlCommon.reversedUrlString2, UrlCommon.urlString2)
        assertUnreverse(UrlCommon.reversedUrlString3, UrlCommon.urlString3)
        assertUnreverse(UrlCommon.reversedUrlString4, UrlCommon.urlString4)
        assertUnreverse(UrlCommon.reversedUrlString5, UrlCommon.urlString5rev)
        assertUnreverse(UrlCommon.reversedUrlString6, UrlCommon.urlString6)
        assertUnreverse(UrlCommon.reversedUrlString7, UrlCommon.urlString7)
        assertUnreverse(UrlCommon.reversedUrlString8, UrlCommon.urlString8)
        assertUnreverse(UrlCommon.reversedUrlString9, UrlCommon.urlString9)
    }

    @Test
    fun testRemoveParameters() {
        val url = "https://www.amazon.com/s?k=sleep&i=amazonfresh&bbn=10329849011&page=2&qid=1609388361&ref=sr_pg_2"
        val stripedUrl = "https://www.amazon.com/s?k=sleep&i=amazonfresh"

        assertTrue { "10329849011" !in UrlUtils.removeQueryParameters(url, "bbn") }
        assertTrue { "page" !in UrlUtils.removeQueryParameters(url, "page") }

        assertEquals(stripedUrl, UrlUtils.removeQueryParameters(url, "bbn", "page", "ref", "qid"))
    }

    @Test
    fun testKeepParameters() {
        val url = "https://www.amazon.com/s?k=sleep&i=amazonfresh&bbn=10329849011&page=2&qid=1609388361&ref=sr_pg_2"
        val stripedUrl = "https://www.amazon.com/s?k=sleep&i=amazonfresh"

        assertTrue { "10329849011" in UrlUtils.keepQueryParameters(url, "bbn") }

        assertTrue { "page" in UrlUtils.keepQueryParameters(url, "page") }
        assertTrue { "bbn" !in UrlUtils.keepQueryParameters(url, "page") }

        assertEquals(stripedUrl, UrlUtils.keepQueryParameters(url, "k", "i"))
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
