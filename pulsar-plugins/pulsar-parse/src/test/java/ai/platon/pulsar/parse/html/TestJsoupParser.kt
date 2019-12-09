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
package ai.platon.pulsar.parse.html

import org.junit.Test
import java.io.IOException
import java.nio.file.Paths

/**
 * Unit tests for PrimerParser
 */
class TestJsoupParser {
    @Test
    @Throws(IOException::class)
    fun test1() {
        val path = Paths.get(SAMPLES_DIR, "selector/1/pages/html_example_3_news.html")
        // Jsoup.parse(path.toFile(), Charset.defaultCharset().toString());
//    JsoupParser parser = new JsoupParser(path);
//    parser.css("author", ".editer!")
//      .css(".art_tit!", ".art_txt!", ".art_info!", ".not-exist")
//      .as(parser)
//      .extract()
//      .assertContains("author", "（责任编辑：刘洋）")
//      .assertContainsValue("（责任编辑：刘洋）");
    }

    companion object {
        val SAMPLES_DIR = System.getProperty("test.data", ".")
    }
}