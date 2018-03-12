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
package org.warps.pulsar.normalizer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.warps.pulsar.crawl.filter.UrlNormalizers;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/test-context/filter-beans.xml"})
public class TestUrlNormalizers {

    private static String[] registeredNormalizers = {
            "org.warps.pulsar.normalizer.BasicUrlNormalizer",
            "org.warps.pulsar.normalizer.RegexUrlNormalizer",
            "org.warps.pulsar.normalizer.PassUrlNormalizer"
    };
    @Autowired
    private UrlNormalizers urlNormalizers;

    @Test
    public void testURLNormalizers() {
        assertEquals(3, urlNormalizers.getURLNormalizers(UrlNormalizers.SCOPE_DEFAULT).size());

        String url = "http://www.example.com/";
        String normalizedUrl = urlNormalizers.normalize(url, UrlNormalizers.SCOPE_DEFAULT);
        assertEquals(url, normalizedUrl);

        url = "http://www.example.org//path/to//somewhere.html";
        String normalizedSlashes = urlNormalizers.normalize(url, UrlNormalizers.SCOPE_DEFAULT);
        assertEquals("http://www.example.org/path/to/somewhere.html", normalizedSlashes);

        // check the order
        String[] impls = urlNormalizers.getURLNormalizers(UrlNormalizers.SCOPE_DEFAULT)
                .stream().map(urlNormalizer -> urlNormalizer.getClass().getName())
                .toArray(String[]::new);
        assertArrayEquals(impls, registeredNormalizers);
    }
}
