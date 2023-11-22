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
package ai.platon.pulsar.normalizer

import ai.platon.pulsar.crawl.filter.ChainedUrlNormalizer
import org.junit.Assert
import kotlin.test.*
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import ai.platon.pulsar.crawl.filter.SCOPE_DEFAULT
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@ContextConfiguration(locations = ["classpath:/test-context/filter-beans.xml"])
class TestBasicUrlNormalizer {
    @Autowired
    private val normalizer: BasicUrlNormalizer? = null
    @Test
    @Throws(Exception::class)
    fun testNormalizer() {
        // check that leading and trailing spaces are removed
        normalizeTest(" http://foo.com/ ", "http://foo.com/")

        // check that protocol is lower cased
        normalizeTest("HTTP://foo.com/", "http://foo.com/")

        // check that host is lower cased
        normalizeTest("http://Foo.Com/index.html", "http://foo.com/index.html")
        normalizeTest("http://Foo.Com/index.html", "http://foo.com/index.html")

        // check that port number is normalized
        normalizeTest("http://foo.com:80/index.html", "http://foo.com/index.html")
        normalizeTest("http://foo.com:81/", "http://foo.com:81/")

        // check that null path is normalized
        normalizeTest("http://foo.com", "http://foo.com/")

        // check that references are removed
        normalizeTest("http://foo.com/foo.html#ref", "http://foo.com/foo.html")

        // // check that encoding is normalized
        // normalizeTest("http://foo.com/%66oo.html", "http://foo.com/foo.html");

        // check that unnecessary "../" are removed
        normalizeTest("http://foo.com/aa/./foo.html", "http://foo.com/aa/foo.html")
        normalizeTest("http://foo.com/aa/../", "http://foo.com/")
        normalizeTest("http://foo.com/aa/bb/../", "http://foo.com/aa/")
        normalizeTest("http://foo.com/aa/..", "http://foo.com/aa/..")
        normalizeTest("http://foo.com/aa/bb/cc/../../foo.html",
                "http://foo.com/aa/foo.html")
        normalizeTest("http://foo.com/aa/bb/../cc/dd/../ee/foo.html",
                "http://foo.com/aa/cc/ee/foo.html")
        normalizeTest("http://foo.com/../foo.html", "http://foo.com/foo.html")
        normalizeTest("http://foo.com/../../foo.html", "http://foo.com/foo.html")
        normalizeTest("http://foo.com/../aa/../foo.html", "http://foo.com/foo.html")
        normalizeTest("http://foo.com/aa/../../foo.html", "http://foo.com/foo.html")
        normalizeTest("http://foo.com/aa/../bb/../foo.html/../../",
                "http://foo.com/")
        normalizeTest("http://foo.com/../aa/foo.html", "http://foo.com/aa/foo.html")
        normalizeTest("http://foo.com/../aa/../foo.html", "http://foo.com/foo.html")
        normalizeTest("http://foo.com/a..a/foo.html",
                "http://foo.com/a..a/foo.html")
        normalizeTest("http://foo.com/a..a/../foo.html", "http://foo.com/foo.html")
        normalizeTest("http://foo.com/foo.foo/../foo.html",
                "http://foo.com/foo.html")
        normalizeTest("http://foo.com//aa/bb/foo.html",
                "http://foo.com/aa/bb/foo.html")
        normalizeTest("http://foo.com/aa//bb/foo.html",
                "http://foo.com/aa/bb/foo.html")
        normalizeTest("http://foo.com/aa/bb//foo.html",
                "http://foo.com/aa/bb/foo.html")
        normalizeTest("http://foo.com//aa//bb//foo.html",
                "http://foo.com/aa/bb/foo.html")
        normalizeTest("http://foo.com////aa////bb////foo.html",
                "http://foo.com/aa/bb/foo.html")
    }

    @Throws(Exception::class)
    private fun normalizeTest(weird: String, normal: String) {
        Assert.assertEquals(normal, normalizer!!.normalize(weird, SCOPE_DEFAULT))
    }
}
