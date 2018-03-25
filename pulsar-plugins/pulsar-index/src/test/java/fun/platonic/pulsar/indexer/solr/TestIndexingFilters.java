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
package fun.platonic.pulsar.indexer.solr;

import fun.platonic.pulsar.common.StringUtil;
import fun.platonic.pulsar.common.UrlUtil;
import fun.platonic.pulsar.common.config.ImmutableConfig;
import fun.platonic.pulsar.common.config.MutableConfig;
import fun.platonic.pulsar.crawl.index.IndexDocument;
import fun.platonic.pulsar.crawl.index.IndexingException;
import fun.platonic.pulsar.crawl.index.IndexingFilters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import fun.platonic.pulsar.persist.WebPage;

import static org.junit.Assert.assertNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/test-context/index-beans.xml"})
public class TestIndexingFilters {

    private String indexingFiltersClassNames = StringUtil.humanize(IndexingFilters.class, "classes", ".");

    @Autowired
    private ImmutableConfig immutableConfig;

    private MutableConfig conf;

    @Autowired
    private IndexingFilters filters;

    @Before
    public void setup() {
        conf = new MutableConfig(immutableConfig);
    }

    /**
     * Test behaviour when defined filter does not exist.
     */
    @Test
    public void testNonExistingIndexingFilter() {
//    conf.addResource("pulsar-default.xml");
//    conf.addResource("crawl-tests.xml");

        String class1 = "NonExistingFilter";
        String class2 = "org.apache.pulsar.indexer.basic.BasicIndexingFilter";
        conf.set(indexingFiltersClassNames, class1 + "," + class2);

        String url = "http://www.example.com/";
        WebPage page = WebPage.newWebPage(url);
        page.setPageText("text");
        page.setPageTitle("title");

        String key = UrlUtil.reverseUrlOrEmpty(url);
        filters.filter(new IndexDocument(key), url, page);
    }

    /**
     * Test behaviour when IndexDocument is null
     */
    @Test
    public void testIndexDocumentNullIndexingFilter() throws IndexingException {
//    conf.addResource("pulsar-default.xml");
//    conf.addResource("crawl-tests.xml");

        String url = "http://www.example.com/";
        WebPage page = WebPage.newWebPage(url);
        page.setPageText("text");
        page.setPageTitle("title");
        IndexDocument doc = filters.filter(null, url, page);

        assertNull(doc);
    }
}
