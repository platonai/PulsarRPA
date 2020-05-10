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

import ai.platon.pulsar.common.Urls.reverseUrl
import ai.platon.pulsar.common.Urls.unreverseUrl
import org.junit.Assert
import org.junit.Test

class TestUrls {
    val urlString1 = "http://foo.com/"
    val urlString2 = "http://foo.com:8900/"
    val urlString3 = "ftp://bar.baz.com/"
    val urlString4 = "http://bar.baz.com:8983/to/index.html?a=b&c=d"
    val urlString5 = "http://foo.com?a=/a/b&c=0"
    val urlString5rev = "http://foo.com/?a=/a/b&c=0"
    val urlString6 = "http://foo.com"
    val urlString7 = "file:///var/www/index.html"
    val urlString8 = "https://www.amazon.com/Best-Sellers-Home-Kitchen/zgbs/home-garden/ref=zg_bs_unv_hg_1_510136_4"
    val reversedUrlString1 = "com.foo:http/"
    val reversedUrlString2 = "com.foo:http:8900/"
    val reversedUrlString3 = "com.baz.bar:ftp/"
    val reversedUrlString4 = "com.baz.bar:http:8983/to/index.html?a=b&c=d"
    val reversedUrlString5 = "com.foo:http/?a=/a/b&c=0"
    val reversedUrlString6 = "com.foo:http"
    val reversedUrlString7 = ":file/var/www/index.html"
    val reversedUrlString8 = "com.amazon.www:https/Best-Sellers-Home-Kitchen/zgbs/home-garden/ref=zg_bs_unv_hg_1_510136_4"

    @Test
    @Throws(Exception::class)
    fun testReverseUrl() {
        println(reverseUrl("http://sz.sxrb.com/sxxww/dspd/szpd/wsjk"))
        println(reverseUrl("http://sz.sxrb.com/sxxww/"))

        assertReverse(urlString1, reversedUrlString1)
        assertReverse(urlString2, reversedUrlString2)
        assertReverse(urlString3, reversedUrlString3)
        assertReverse(urlString4, reversedUrlString4)
        assertReverse(urlString5, reversedUrlString5)
        assertReverse(urlString5, reversedUrlString5)
        assertReverse(urlString6, reversedUrlString6)
        assertReverse(urlString7, reversedUrlString7)
        assertReverse(urlString8, reversedUrlString8)
    }

    @Test
    @Throws(Exception::class)
    fun testUnreverseUrl() {
        println(unreverseUrl("com.sxrb.www:http/sxxww/zthj/xmtdt/6619357.shtml"))
        assertUnreverse(reversedUrlString1, urlString1)
        assertUnreverse(reversedUrlString2, urlString2)
        assertUnreverse(reversedUrlString3, urlString3)
        assertUnreverse(reversedUrlString4, urlString4)
        assertUnreverse(reversedUrlString5, urlString5rev)
        assertUnreverse(reversedUrlString6, urlString6)
        assertUnreverse(reversedUrlString7, urlString7)
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