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
package ai.platon.pulsar.filter;

import ai.platon.pulsar.crawl.filter.UrlFilter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.io.Reader;

import static org.junit.Assert.fail;

/**
 * JUnit based test of class <code>RegexURLFilter</code>.
 *
 * @author J&eacute;r&ocirc;me Charron
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class TestRegexUrlFilter extends RegexUrlFilterBaseTest {

    @Override
    protected UrlFilter getURLFilter(Reader reader) {
        try {
            return new RegexUrlFilter(reader);
        } catch (IOException e) {
            fail(e.toString());
            return null;
        }
    }

    @Test
    public void test() {
        test("WholeWebCrawling");
        test("IntranetCrawling");
        bench(50, "Benchmarks");
        bench(100, "Benchmarks");
        bench(200, "Benchmarks");
        bench(400, "Benchmarks");
        bench(800, "Benchmarks");
    }
}
