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
package ai.platon.pulsar.crawl.common

import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.urls.NormUrl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Test class for NormUrl
 */
class NormUrlTests {
    private val defaultUrl = "https://shopee.co.th/กระเป๋าเป้ผู้ชาย-cat.49.1037.10297?page=1"
    private val defaultArgs = """
        -i 1s -ii 1s -ol ".shopee-search-item-result__item a" -sc 10
    """.trimIndent()
    private val defaultNormalizedArgs = LoadOptions.normalize(defaultArgs)
    private val defaultConfiguredUrl = "$defaultUrl $defaultNormalizedArgs"
    private val defaultNormalizedConfiguredUrl = "$defaultUrl $defaultNormalizedArgs"
    private val defaultOptions = LoadOptions.DEFAULT
    private val volatileConfig = defaultOptions.conf

    @Test
    fun testParse() {
        val url = defaultUrl
        val args = defaultNormalizedArgs
        val configuredUrl = defaultNormalizedConfiguredUrl
        val normUrl = NormUrl.parse(configuredUrl, volatileConfig)
        assertNormUrl(url, args, configuredUrl, normUrl)
    }

    @Test
    fun testNILUrl() {
        val url = AppConstants.NIL_PAGE_URL
        val args = ""
        val configuredUrl = url
        assertNormUrl(url, args, configuredUrl, NormUrl.NIL)
    }

    private fun assertNormUrl(url: String, args: String, configuredUrl: String, normUrl: NormUrl) {
        assertFalse(normUrl.isEmpty)
        assertTrue(normUrl.isNotEmpty)

        assertEquals(url, normUrl.url.toString())
        assertEquals(url, normUrl.spec)
        assertEquals(configuredUrl, normUrl.configuredUrl)
        assertEquals(configuredUrl, normUrl.toString())

        val (spec, options) = normUrl
        assertEquals(spec, url)
    }
}
