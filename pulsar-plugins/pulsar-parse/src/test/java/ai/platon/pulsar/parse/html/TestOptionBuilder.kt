/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.platon.pulsar.parse.html;

import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.common.options.EntityOptions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for PrimerParser.
 */
@ContextConfiguration(locations = {"classpath:/test-context/parse-beans.xml"})
@RunWith(SpringJUnit4ClassRunner.class)
public class TestOptionBuilder {

    @Autowired
    private ImmutableConfig conf;

    @Before
    public void setup() {
    }

    @Test
    public void test1() {
        String args = "-Ftitle=.art_tit! -Fcontent=.art_content! -Finfo=.art_info! -Fauthor=.editer! -Fnobody=.not-exist";
        EntityOptions options = EntityOptions.parse(args);
        Map<String, String> cssRules = options.getCssRules();
        assertEquals(".art_tit!", cssRules.get("title"));
        assertEquals(".art_content!", cssRules.get("content"));
        System.out.println(options.getParams());
        System.out.println(options.toString());
    }

    @Test
    public void testEntityOptionBuilder() {
        EntityOptions options = EntityOptions.newBuilder()
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
                .build();

        Map<String, String> cssRules = options.getCssRules();
        assertEquals(".art_tit!", cssRules.get("title"));
        assertEquals(".art_content!", cssRules.get("content"));
        System.out.println(options.getParams().sorted());
        System.out.println(options.getParams().sorted().withKVDelimiter(" ").withCmdLineStyle().formatAsLine());
        System.out.println(options.toString());
    }

    @Test
    public void testEntityOptionBuilder2() {
        EntityOptions options = EntityOptions.newBuilder()
                .css(".art_tit!", ".art_content!", ".avatar", ".name", ".gender", ".company", ".room_num", ".tel")
                .re("(author:)(\\b)", "(address:)(\\b)", "(tel:)(\\d{8,13})", "(price:)(\\d+)")
                .c_root(".comments")
                .c_item(".comment")
                .c_css(".name", ".comment_content", ".publish_time")
                .c_re("(reply:)(\\d+)")
                .build();

        Map<String, String> cssRules = options.getCssRules();
        assertTrue(cssRules.containsValue(".art_tit!"));
        assertTrue(cssRules.containsValue(".art_content!"));

        Map<String, String> c_cssRules = options.getCollectionOptions().getCssRules();
        assertTrue(c_cssRules.containsValue(".name"));
        assertTrue(c_cssRules.containsValue(".comment_content"));
    }
}
