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

import org.junit.Assert
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import ai.platon.pulsar.crawl.filter.SCOPE_DEFAULT
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import kotlin.test.*

@RunWith(SpringRunner::class)
@ContextConfiguration(locations = ["classpath:/test-context/filter-beans.xml"])
class TestPassUrlNormalizer {
    @Autowired
    private val normalizer: PassUrlNormalizer? = null
    @Test
    fun testPassURLNormalizer() {
        val url = "http://www.example.com/test/..//"
        val result = normalizer!!.normalize(url, SCOPE_DEFAULT)
        Assert.assertEquals(url, result)
    }
}
