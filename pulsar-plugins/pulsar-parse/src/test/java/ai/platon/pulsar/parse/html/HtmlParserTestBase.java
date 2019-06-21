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

package ai.platon.pulsar.parse.html;

import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.common.config.PulsarConstants;
import ai.platon.pulsar.persist.WebPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import java.nio.charset.Charset;

@ContextConfiguration(locations = {"classpath:/test-context/parse-beans.xml"})
public class HtmlParserTestBase {
    public static final Logger LOG = LoggerFactory.getLogger(HtmlParserTestBase.class);
    public final static String SAMPLES_DIR = System.getProperty("test.data", ".");

    protected static final String encodingTestKeywords = "français, español, русский язык, čeština, ελληνικά";
    protected static final String encodingTestBody = "<ul>\n  <li>français\n  <li>español\n  <li>русский язык\n  <li>čeština\n  <li>ελληνικά\n</ul>";
    protected static final String encodingTestBody2 =
            "<div>" +
                    "  <div>" +
                    "    <ul class='u1'><li>first item</li><li>second item</li></ul>" +
                    "    <ul class='u2'><li>first item</li><li>second item</li></ul>" +
                    "    <div class='d1'>d1</div>" +
                    "    <div class='d2'>d2</div>" +
                    "    <div class='d3'>d3</div>" +
                    "  </div>" +
                    "</div>";
    protected static final String encodingTestContent = "<title>"
            + encodingTestKeywords + "</title>\n"
            + "<meta name=\"keywords\" content=\"" + encodingTestKeywords + "\"/>\n"
            + "</head>\n<body>" + encodingTestBody + encodingTestBody2 + "</body>\n</html>";
    protected static final String exampleUrl = PulsarConstants.EXAMPLE_URL;
    protected static String[][] encodingTestPages = {
            {
                    "HTML4, utf-8, meta http-equiv, no quotes",
                    "utf-8",
                    "success",
                    "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" "
                            + "\"http://www.w3.org/TR/html4/loose.dtd\">\n"
                            + "<html>\n<head>\n"
                            + "<meta http-equiv=Content-Type content=\"text/html; charset=utf-8\" />"
                            + encodingTestContent
            },
            {
                    "HTML4, utf-8, meta http-equiv, single quotes",
                    "utf-8",
                    "success",
                    "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" "
                            + "\"http://www.w3.org/TR/html4/loose.dtd\">\n"
                            + "<html>\n<head>\n"
                            + "<meta http-equiv='Content-Type' content='text/html; charset=utf-8' />"
                            + encodingTestContent
            },
            {
                    "XHTML, utf-8, meta http-equiv, double quotes",
                    "utf-8",
                    "success",
                    "<?xml version=\"1.0\"?>\n<html xmlns=\"http://www.w3.org/1999/xhtml\">"
                            + "<html>\n<head>\n"
                            + "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />"
                            + encodingTestContent
            },
            {
                    "HTML5, utf-8, meta charset",
                    "utf-8",
                    "success",
                    "<!DOCTYPE html>\n<html>\n<head>\n" + "<meta charset=\"utf-8\">"
                            + encodingTestContent
            },
            {
                    "HTML5, utf-8, BOM",
                    "utf-8",
                    "success",
                    "\ufeff<!DOCTYPE html>\n<html>\n<head>\n" + encodingTestContent
            },
            {
                    "HTML5, utf-16, BOM",
                    "utf-16",
                    "failed",
                    "\ufeff<!DOCTYPE html>\n<html>\n<head>\n" + encodingTestContent
            }
    };

    @Autowired
    protected ImmutableConfig conf;

    protected WebPage getPage(String html, Charset charset) {
        WebPage page = WebPage.newWebPage(exampleUrl);

        page.setLocation(exampleUrl);
        page.setContent(html.getBytes(charset));
        page.setContentType("text/html");

        return page;
    }
}
