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
package fun.platonic.pulsar.filter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(SpringJUnit4ClassRunner.class)
public class TestUrlValidator extends UrlFilterTestBase {

    private static int tldLength;
    private String validUrl;
    private String invalidUrl;
    private String preUrl = "http://example.";

    @Before
    public void setUp() throws Exception {
        tldLength = conf.getInt("urlfilter.tld.length", 8);
    }

    @Test
    public void testFilter() {
        UrlValidator urlValidator = new UrlValidator(conf);

        validUrl = generateValidTld(tldLength);
        invalidUrl = generateInvalidTld(tldLength);

        assertNotNull(urlValidator);

        // invalid urls
        assertNull("Filtering on a null object should return null",
                urlValidator.filter(null));
        assertNull("Invalid url: example.com/file[/].html",
                urlValidator.filter("example.com/file[/].html"));
        assertNull("Invalid url: http://www.example.com/space here.html",
                urlValidator.filter("http://www.example.com/space here.html"));
        assertNull("Invalid url: /main.html", urlValidator.filter("/main.html"));
        assertNull("Invalid url: www.example.com/main.html",
                urlValidator.filter("www.example.com/main.html"));
        assertNull("Invalid url: ftp:www.example.com/main.html",
                urlValidator.filter("ftp:www.example.com/main.html"));
        assertNull("Inalid url: http://999.000.456.32/pulsar/trunk/README.txt",
                urlValidator.filter("http://999.000.456.32/pulsar/trunk/README.txt"));
        assertNull("Invalid url: http://www.example.com/ma|in\\toc.html",
                urlValidator.filter(" http://www.example.com/ma|in\\toc.html"));
        // test tld limit
        assertNull("InValid url: " + invalidUrl, urlValidator.filter(invalidUrl));

        // valid urls
        assertNotNull("Valid url: https://issues.apache.org/jira/PULSAR-1127",
                urlValidator.filter("https://issues.apache.org/jira/PULSAR-1127"));
        assertNotNull(
                "Valid url: http://domain.tld/function.cgi?url=http://fonzi.com/&amp;name=Fonzi&amp;mood=happy&amp;coat=leather",
                urlValidator
                        .filter("http://domain.tld/function.cgi?url=http://fonzi.com/&amp;name=Fonzi&amp;mood=happy&amp;coat=leather"));
        assertNotNull(
                "Valid url: http://validator.w3.org/feed/check.cgi?url=http%3A%2F%2Ffeeds.feedburner.com%2Fperishablepress",
                urlValidator
                        .filter("http://validator.w3.org/feed/check.cgi?url=http%3A%2F%2Ffeeds.feedburner.com%2Fperishablepress"));
        assertNotNull("Valid url: ftp://alfa.bravo.pi/foo/bar/plan.pdf",
                urlValidator.filter("ftp://alfa.bravo.pi/mike/check/plan.pdf"));
        // test tld limit
        assertNotNull("Valid url: " + validUrl, urlValidator.filter(validUrl));

    }

    /**
     * Generate Sample of Valid Tld.
     */
    public String generateValidTld(int length) {
        StringBuilder buffer = new StringBuilder();
        for (int i = 1; i <= length; i++) {
            char c = (char) ('a' + Math.random() * 26);
            buffer.append(c);
        }
        return preUrl + buffer.toString();
    }

    /**
     * Generate Sample of Invalid Tld. character
     */
    public String generateInvalidTld(int length) {
        StringBuilder buffer = new StringBuilder();
        for (int i = 1; i <= length + 1; i++) {
            char c = (char) ('a' + Math.random() * 26);
            buffer.append(c);
        }
        return preUrl + buffer.toString();
    }
}
