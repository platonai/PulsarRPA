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
package fun.platonic.pulsar.crawl.common;

import fun.platonic.pulsar.common.EncodingDetector;
import fun.platonic.pulsar.common.HttpHeaders;
import fun.platonic.pulsar.common.config.MutableConfig;
import fun.platonic.pulsar.persist.WebPage;
import org.apache.avro.util.Utf8;
import org.junit.Before;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;

public class TestEncodingDetector {
    private static byte[] contentInOctets;

    static {
        try {
            contentInOctets = "çñôöøДЛжҶ".getBytes("utf-8");
        } catch (UnsupportedEncodingException e) {
            // not possible
        }
    }

    private MutableConfig conf;

    @Before
    public void setup() {
        conf = new MutableConfig();
    }

    @Test
    public void testGuessing() {
        // first disable auto detection
        conf.setInt(EncodingDetector.MIN_CONFIDENCE_KEY, -1);

        // MultiMetadata metadata = new MultiMetadata();
        EncodingDetector detector;
        // Content content;
        String encoding;

        String url = "http://www.example.com/";
        WebPage page = WebPage.newWebPage(url);
        page.setBaseUrl(url);
        page.setContentType("text/plain");
        page.setContent(contentInOctets);

        detector = new EncodingDetector(conf);
        detector.autoDetectClues(page, true);
        encoding = detector.guessEncoding(page, "utf-8");
        // no information is available, so it should return default encoding
        assertEquals("utf-8", encoding.toLowerCase());

        page = WebPage.newWebPage(url);
        page.setBaseUrl(url);
        page.setContentType("text/plain");
        page.setContent(contentInOctets);
        page.getHeaders().put(HttpHeaders.CONTENT_TYPE, "text/plain; charset=UTF-16");

        detector = new EncodingDetector(conf);
        detector.autoDetectClues(page, true);
        encoding = detector.guessEncoding(page, "utf-8");
        assertEquals("utf-16", encoding.toLowerCase());

        page = WebPage.newWebPage(url);
        page.setBaseUrl(url);
        page.setContentType("text/plain");
        page.setContent(contentInOctets);

        detector = new EncodingDetector(conf);
        detector.autoDetectClues(page, true);
        detector.addClue("windows-1254", "sniffed");
        encoding = detector.guessEncoding(page, "utf-8");
        assertEquals("windows-1254", encoding.toLowerCase());

        // enable autodetection
        conf.setInt(EncodingDetector.MIN_CONFIDENCE_KEY, 50);
        page = WebPage.newWebPage(url);
        page.setBaseUrl(url);
        page.setContentType("text/plain");
        page.setContent(contentInOctets);
        page.unbox().getMetadata().put(new Utf8(HttpHeaders.CONTENT_TYPE),
                ByteBuffer.wrap("text/plain; charset=UTF-16".getBytes()));

        detector = new EncodingDetector(conf);
        detector.autoDetectClues(page, true);
        detector.addClue("utf-32", "sniffed");
        encoding = detector.guessEncoding(page, "utf-8");
        assertEquals("utf-8", encoding.toLowerCase());
    }
}
