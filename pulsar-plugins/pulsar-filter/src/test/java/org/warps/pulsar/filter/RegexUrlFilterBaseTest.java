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

// JDK imports

import org.warps.pulsar.common.StringUtil;
import org.warps.pulsar.crawl.filter.UrlFilter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import static org.junit.Assert.*;

public abstract class RegexUrlFilterBaseTest extends UrlFilterTestBase {

    protected String sampleDir = Paths.get(TEST_DIR, "sample").toString();

    protected RegexUrlFilterBaseTest() {
    }

    protected RegexUrlFilterBaseTest(String sampleDir) {
        this.sampleDir = sampleDir;
    }

    private static ArrayList<FilteredURL> readURLFile(Reader reader) throws IOException {
        return new BufferedReader(reader).lines().map(FilteredURL::new).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    protected abstract UrlFilter getURLFilter(Reader rules);

    protected void bench(int loops, String file) {
        try {
            Path rulesPath = Paths.get(sampleDir, file + ".rules");
            Path urlPath = Paths.get(sampleDir, file + ".urls");
            bench(loops, new FileReader(rulesPath.toFile()), new FileReader(urlPath.toFile()));
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    protected void bench(int loops, Reader rules, Reader urls) {
        long start = System.currentTimeMillis();
        try {
            UrlFilter filter = getURLFilter(rules);
            ArrayList<FilteredURL> expected = readURLFile(urls);
            for (int i = 0; i < loops; i++) {
                test(filter, expected);
            }
        } catch (Exception e) {
            fail(e.toString());
        }
        LOG.info("bench time (" + loops + ") " + (System.currentTimeMillis() - start) + "ms");
    }

    protected void test(String file) {
        try {
            Path rulesPath = Paths.get(sampleDir, file + ".rules");
            Path urlPath = Paths.get(sampleDir, file + ".urls");
            LOG.info("Rules File : " + rulesPath);
            LOG.info("Urls File : " + urlPath);

            test(new FileReader(rulesPath.toFile()), new FileReader(urlPath.toFile()));
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    protected void test(Reader rules, Reader urls) {
        try {
            test(getURLFilter(rules), readURLFile(urls));
        } catch (Exception e) {
            fail(StringUtil.stringifyException(e));
        }
    }

    protected void test(UrlFilter filter, ArrayList<FilteredURL> expected) {
        expected.forEach(url -> {
            String result = filter.filter(url.url);
            if (result != null) {
                assertTrue(url.url, url.sign);
            } else {
                assertFalse(url.url, url.sign);
            }
        });
    }

    private static class FilteredURL {
        boolean sign;
        String url;

        FilteredURL(String line) {
            switch (line.charAt(0)) {
                case '+':
                    sign = true;
                    break;
                case '-':
                    sign = false;
                    break;
                default:
                    // Simply ignore...
            }
            url = line.substring(1);
        }
    }
}
