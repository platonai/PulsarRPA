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
package ai.platon.pulsar.indexer.solr;

import ai.platon.pulsar.common.StringUtil;
import ai.platon.pulsar.common.Urls;
import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.crawl.index.IndexDocument;
import ai.platon.pulsar.crawl.index.IndexingException;
import ai.platon.pulsar.crawl.index.IndexingFilters;
import ai.platon.pulsar.persist.WebPage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/test-context/index-beans.xml"})
public class TestIndexingFilters {

    private String indexingFiltersClassNames = StringUtil.humanize(IndexingFilters.class, "classes", ".");

    @Autowired
    private ImmutableConfig conf;

    @Autowired
    private IndexingFilters filters;

    @Before
    public void setup() {

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
        conf.unbox().set(indexingFiltersClassNames, class1 + "," + class2);

        String url = "http://www.example.com/";
        WebPage page = WebPage.newWebPage(url);
        page.setPageText("text");
        page.setPageTitle("title");

        String key = Urls.reverseUrlOrEmpty(url);
        filters.filter(new IndexDocument(key), url, page);
    }

    /**
     * Test behaviour when IndexDocument is null
     */
    @Test
    public void testIndexDocumentNullIndexingFilter() throws IndexingException {
        String url = "http://www.example.com/";
        WebPage page = WebPage.newWebPage(url);
        page.setPageText("text");
        page.setPageTitle("title");
        IndexDocument doc = filters.filter(null, url, page);

        assertNull(doc);
    }
}
