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
package ai.platon.pulsar.filter

import ai.platon.pulsar.common.ResourceLoader.readAllLines
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import java.io.IOException
import java.time.ZoneId
import kotlin.test.*

/**
 * JUnit based test of class `RegexURLFilter`.
 *
 * @author Jrme Charron
 */
@RunWith(SpringRunner::class)
@ContextConfiguration(locations = ["classpath:/test-context/filter-beans.xml"])
class TestDateUrlFilter : UrlFilterTestBase("datedata") {
    @Autowired
    lateinit var dateUrlFilter: DateUrlFilter

    @BeforeTest
    @Throws(IOException::class)
    fun setUp() {
        dateUrlFilter = DateUrlFilter(ZoneId.systemDefault(), conf)
    }

    @Test
    fun testNotSupportedDateFormat() {
        val urls = readAllLines("datedata/urls_with_not_supported_old_date.txt")
        for (url in urls) {
            assertNotNull(dateUrlFilter.filter(url), url)
        }
    }

    @Test
    fun testDateTimeDetector() {
        val urls = readAllLines("datedata/urls_with_old_date.txt")
        for (url in urls) {
            assertNull(dateUrlFilter.filter(url), url)
        }
    }
}
