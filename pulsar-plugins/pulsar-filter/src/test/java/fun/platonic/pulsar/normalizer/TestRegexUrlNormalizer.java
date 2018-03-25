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

package fun.platonic.pulsar.normalizer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import fun.platonic.pulsar.crawl.filter.UrlNormalizers;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/test-context/filter-beans.xml"})
public class TestRegexUrlNormalizer {
    private static final Logger LOG = LoggerFactory.getLogger(TestRegexUrlNormalizer.class);

    @Autowired
    private RegexUrlNormalizer normalizer;
    private HashMap<String, List<NormalizedURL>> testData = new HashMap<>();

    private String PWD = System.getProperty("test.data", ".");
    private Path SAMPLE_DIR = Paths.get(PWD, "normregex", "sample");

    @Before
    public void setUp() throws IOException {
        File[] configs = SAMPLE_DIR.toFile()
                .listFiles(f -> f.getName().endsWith(".xml") && f.getName().startsWith("regex-normalize-"));

        for (File config : configs) {
            try {
                FileReader reader = new FileReader(config);
                String cname = config.getName();
                cname = cname.substring(16, cname.indexOf(".xml"));
                normalizer.setConfiguration(reader, cname);
                List<NormalizedURL> urls = readTestFile(cname);
                testData.put(cname, urls);
            } catch (Exception e) {
                LOG.warn("Could load config from '" + config + "': " + e.toString());
            }
        }
    }

    @Test
    public void testNormalizerDefault() throws Exception {
        normalizeTest(testData.get(UrlNormalizers.SCOPE_DEFAULT), UrlNormalizers.SCOPE_DEFAULT);
    }

    @Test
    public void testNormalizerScope() throws Exception {
        for (String scope : testData.keySet()) {
            normalizeTest(testData.get(scope), scope);
        }
    }

    private void normalizeTest(List<NormalizedURL> urls, String scope) {
        for (NormalizedURL url1 : urls) {
            String url = url1.url;
            String normalized = normalizer.normalize(url1.url, scope);
            String expected = url1.expectedURL;
            LOG.info("scope: " + scope + " url: " + url + " | normalized: " + normalized + " | expected: " + expected);
            assertEquals(url1.expectedURL, normalized);
        }
    }

    private List<NormalizedURL> readTestFile(String scope) throws IOException {
        Path testFile = Paths.get(SAMPLE_DIR.toString(), "regex-normalize-" + scope + ".test");
        return Files.readAllLines(testFile).stream()
                .map(String::trim)
                .filter(l -> !l.isEmpty())
                .filter(l -> !l.startsWith("#")).map(NormalizedURL::new)
                .collect(Collectors.toList());
    }

    private static class NormalizedURL {
        String url;
        String expectedURL;

        public NormalizedURL(String line) {
            String[] fields = line.split("\\s+");
            url = fields[0];
            expectedURL = fields[1];
        }
    }
}
