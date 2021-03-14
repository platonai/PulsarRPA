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
package ai.platon.pulsar.normalizer

import ai.platon.pulsar.crawl.filter.UrlNormalizer
import ai.platon.pulsar.crawl.filter.UrlNormalizers
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@ContextConfiguration(locations = ["classpath:/test-context/filter-beans.xml"])
class TestUrlNormalizers {
    @Autowired
    private val urlNormalizers: UrlNormalizers? = null
    @Test
    fun testURLNormalizers() {
        Assert.assertEquals(3, urlNormalizers!!.getURLNormalizers(UrlNormalizers.SCOPE_DEFAULT).size.toLong())
        var url = "http://www.example.com/"
        val normalizedUrl = urlNormalizers.normalize(url, UrlNormalizers.SCOPE_DEFAULT)
        Assert.assertEquals(url, normalizedUrl)
        url = "http://www.example.org//path/to//somewhere.html"
        val normalizedSlashes = urlNormalizers.normalize(url, UrlNormalizers.SCOPE_DEFAULT)
        Assert.assertEquals("http://www.example.org/path/to/somewhere.html", normalizedSlashes)

        // check the order
        val impls = urlNormalizers.getURLNormalizers(UrlNormalizers.SCOPE_DEFAULT)
                .map { it.javaClass.name }
                .toTypedArray()
        Assert.assertArrayEquals(impls, registeredNormalizers)
    }

    companion object {
        private val registeredNormalizers = arrayOf(
                "ai.platon.pulsar.normalizer.BasicUrlNormalizer",
                "ai.platon.pulsar.normalizer.RegexUrlNormalizer",
                "ai.platon.pulsar.normalizer.PassUrlNormalizer"
        )
    }
}