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
package org.warps.pulsar.filter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.warps.pulsar.common.ResourceLoader;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * JUnit based test of class <code>RegexURLFilter</code>.
 *
 * @author J&eacute;r&ocirc;me Charron
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/test-context/filter-beans.xml"})
public class TestDateUrlFilter extends UrlFilterTestBase {

    @Autowired
    DateUrlFilter dateUrlFilter;

    @Before
    public void setUp() throws IOException {
        dateUrlFilter = new DateUrlFilter(ZoneId.systemDefault(), conf);
    }

    @Test
    public void testNotSupportedDateFormat() {
        String dataFile = Paths.get(TEST_DIR, "datedata", "urls_with_not_supported_old_date.txt").toString();
        List<String> urls = new ResourceLoader().readAllLines(null, dataFile);

        for (String url : urls) {
            assertNotNull(url, dateUrlFilter.filter(url));
        }
    }

    @Test
    public void testDateTimeDetector() {
        String dataFile = Paths.get(TEST_DIR, "datedata", "urls_with_old_date.txt").toString();
        List<String> urls = new ResourceLoader().readAllLines(null, dataFile);

        for (String url : urls) {
            assertNull(url, dateUrlFilter.filter(url));
        }
    }
}
