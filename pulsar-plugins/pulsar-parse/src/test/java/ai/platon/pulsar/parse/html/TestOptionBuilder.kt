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

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.options.EntityOptions
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner

/**
 * Unit tests for PrimerParser.
 */
@ContextConfiguration(locations = ["classpath:/test-context/parse-beans.xml"])
@RunWith(SpringRunner::class)
class TestOptionBuilder {
    @Autowired
    private val conf: ImmutableConfig? = null

    @Before
    fun setup() {
    }

    @Test
    fun test1() {
        val args = "-Ftitle=.art_tit! -Fcontent=.art_content! -Finfo=.art_info! -Fauthor=.editer! -Fnobody=.not-exist"
        val options = EntityOptions.parse(args)
        val cssRules = options.cssRules
        Assert.assertEquals(".art_tit!", cssRules["title"])
        Assert.assertEquals(".art_content!", cssRules["content"])
        println(options.params)
        println(options.toString())
    }

    @Test
    fun testEntityOptionBuilder() {
        val options = EntityOptions.newBuilder()
                .name("entity")
                .root(":root")
                .css("title", ".art_tit!")
                .css("content", ".art_content!")
                .css(".avatar")
                .css(".name")
                .css(".gender")
                .css(".company", ".room_num", ".tel")
                .xpath("//body/div[4]")
                .re("(author:)(\\b)")
                .re("price", "(price:)(\\d+)")
                .re("(author:)(\\b)", "(address:)(\\b)", "(tel:)(\\d{8,13})")
                .c_root(".comments")
                .c_item(".comment")
                .c_css(".name")
                .c_css(".comment_content")
                .c_css(".publish_time")
                .c_re("(reply:)(\\d+)")
                .build()
        val cssRules = options.cssRules
        Assert.assertEquals(".art_tit!", cssRules["title"])
        Assert.assertEquals(".art_content!", cssRules["content"])
        println(options.params.sorted())
        println(options.params.sorted().withKVDelimiter(" ").withCmdLineStyle().formatAsLine())
        println(options.toString())
    }

    @Test
    fun testEntityOptionBuilder2() {
        val options = EntityOptions.newBuilder()
                .css(".art_tit!", ".art_content!", ".avatar", ".name", ".gender", ".company", ".room_num", ".tel")
                .re("(author:)(\\b)", "(address:)(\\b)", "(tel:)(\\d{8,13})", "(price:)(\\d+)")
                .c_root(".comments")
                .c_item(".comment")
                .c_css(".name", ".comment_content", ".publish_time")
                .c_re("(reply:)(\\d+)")
                .build()
        val cssRules = options.cssRules
        Assert.assertTrue(cssRules.containsValue(".art_tit!"))
        Assert.assertTrue(cssRules.containsValue(".art_content!"))
        val c_cssRules = options.collectionOptions.cssRules
        Assert.assertTrue(c_cssRules.containsValue(".name"))
        Assert.assertTrue(c_cssRules.containsValue(".comment_content"))
    }
}