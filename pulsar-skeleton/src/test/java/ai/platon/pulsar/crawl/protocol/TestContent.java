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

package ai.platon.pulsar.crawl.protocol;

import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.crawl.common.WritableTestUtils;
import ai.platon.pulsar.crawl.protocol.io.ContentWritable;
import ai.platon.pulsar.persist.metadata.MultiMetadata;
import ai.platon.pulsar.persist.metadata.SpellCheckedMultiMetadata;
import org.apache.tika.mime.MimeTypes;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestContent {

    private static ImmutableConfig conf = new ImmutableConfig();

    @Test
    @Ignore("Failed to write-read content")
    public void testContent() throws Exception {
        String page = "<HTML><BODY><H1>Hello World</H1><P>The Quick Brown Fox Jumped Over the Lazy Fox.</BODY></HTML>";

        String url = "http://www.foo.com/";

        SpellCheckedMultiMetadata metaData = new SpellCheckedMultiMetadata();
        metaData.put("Host", "www.foo.com");
        metaData.put("Content-Type", "text/html");

        Content r = new Content(url, url, page.getBytes("UTF-8"), "text/html", metaData, conf);

        WritableTestUtils.testWritable(new ContentWritable(r));
        Assert.assertEquals("text/html", r.getMetadata().get("Content-Type"));
        Assert.assertEquals("text/html", r.getMetadata().get("content-type"));
        Assert.assertEquals("text/html", r.getMetadata().get("CONTENTYPE"));
    }

    @Test
    public void testGetContentType() throws Exception {
        Content c;
        MultiMetadata p = new MultiMetadata();

        c = new Content("http://www.foo.com/", "http://www.foo.com/",
                "".getBytes("UTF8"), "text/html; charset=UTF-8", p, conf);
        assertEquals("text/html", c.getContentType());

        c = new Content("http://www.foo.com/foo.html", "http://www.foo.com/",
                "".getBytes("UTF8"), "", p, conf);
        assertEquals("text/html", c.getContentType());

        c = new Content("http://www.foo.com/foo.html", "http://www.foo.com/",
                "".getBytes("UTF8"), null, p, conf);
        assertEquals("text/html", c.getContentType());

        c = new Content("http://www.foo.com/", "http://www.foo.com/",
                "<html></html>".getBytes("UTF8"), "", p, conf);
        assertEquals("text/html", c.getContentType());

        c = new Content("http://www.foo.com/foo.html", "http://www.foo.com/",
                "<html></html>".getBytes("UTF8"), "text/plain", p, conf);
        assertEquals("text/html", c.getContentType());

        c = new Content("http://www.foo.com/foo.png", "http://www.foo.com/",
                "<html></html>".getBytes("UTF8"), "text/plain", p, conf);
        assertEquals("text/html", c.getContentType());

        c = new Content("http://www.foo.com/", "http://www.foo.com/",
                "".getBytes("UTF8"), "", p, conf);
        assertEquals(MimeTypes.OCTET_STREAM, c.getContentType());

        c = new Content("http://www.foo.com/", "http://www.foo.com/",
                "".getBytes("UTF8"), null, p, conf);
        assertNotNull(c.getContentType());
    }
}
