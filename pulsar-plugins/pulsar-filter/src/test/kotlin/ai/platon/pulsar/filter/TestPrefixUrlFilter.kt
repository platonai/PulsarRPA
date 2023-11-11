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

import org.junit.Assert
import kotlin.test.*
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import java.io.IOException
import java.util.stream.Stream

@RunWith(SpringRunner::class)
@ContextConfiguration(locations = ["classpath:/test-context/filter-beans.xml"])
class TestPrefixUrlFilter : UrlFilterTestBase("") {
    @Autowired
    lateinit var prefixUrlFilter: PrefixUrlFilter

    @Test
    @Throws(IOException::class)
    fun testModeAccept() {
        prefixUrlFilter.reload(prefixes)
        val filteredUrls = urls.map { prefixUrlFilter.filter(it) }.toTypedArray()
        Assert.assertArrayEquals(urlsModeAccept, filteredUrls)
    }

    companion object {
        private const val prefixes = ("# this is a comment\n" + "\n"
                + "http://\n" + "https://\n" + "file://\n" + "ftp://\n")
        private val urls = arrayOf(
                "http://www.example.com/", "https://www.example.com/",
                "ftp://www.example.com/", "file://www.example.com/",
                "abcd://www.example.com/", "www.example.com/"
        )
        private val urlsModeAccept = arrayOf(urls[0], urls[1], urls[2], urls[3], null, null)
    }
}
