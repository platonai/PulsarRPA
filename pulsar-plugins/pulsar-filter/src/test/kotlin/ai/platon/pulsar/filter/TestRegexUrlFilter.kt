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

import ai.platon.pulsar.crawl.filter.UrlFilter
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import java.io.Reader

/**
 * JUnit based test of class `RegexURLFilter`.
 *
 * @author Jrme Charron
 */
@RunWith(SpringJUnit4ClassRunner::class)
class TestRegexUrlFilter : RegexUrlFilterBaseTest() {

    override fun getURLFilter(reader: Reader): UrlFilter {
        return RegexUrlFilter(reader, conf)
    }

    @Test
    fun test() {
        test("WholeWebCrawling")
        test("IntranetCrawling")
        bench(50, "Benchmarks")
        bench(100, "Benchmarks")
        bench(200, "Benchmarks")
        bench(400, "Benchmarks")
        bench(800, "Benchmarks")
    }
}
