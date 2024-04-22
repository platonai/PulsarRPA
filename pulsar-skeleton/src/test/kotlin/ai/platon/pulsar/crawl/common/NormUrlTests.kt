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
import ai.platon.pulsar.common.urls.NormURL
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Test class for NormURL
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
        val normURL = NormURL.parse(configuredUrl, volatileConfig)
        assertNormUrl(url, args, configuredUrl, normURL)
    }

    @Test
    fun testNILUrl() {
        val url = AppConstants.NIL_PAGE_URL
        val args = ""
        val configuredUrl = url
        assertNormUrl(url, args, configuredUrl, NormURL.NIL)
    }

    private fun assertNormUrl(url: String, args: String, configuredUrl: String, normURL: NormURL) {
        assertEquals(url, normURL.url.toString())
        assertEquals(url, normURL.spec)
        assertEquals(configuredUrl, normURL.configuredUrl)
        assertEquals(configuredUrl, normURL.toString())

        val (spec, options) = normURL
        assertEquals(spec, url)
    }
}
