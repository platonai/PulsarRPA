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

package ai.platon.pulsar.parse.metatags;

import ai.platon.pulsar.common.URLUtil;
import ai.platon.pulsar.common.Urls;
import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.common.config.MutableConfig;
import ai.platon.pulsar.persist.WebPage;
import ai.platon.pulsar.crawl.parse.PageParser;
import ai.platon.pulsar.crawl.parse.ParseResult;
import ai.platon.pulsar.crawl.parse.html.HTMLMetaTags;
import ai.platon.pulsar.crawl.parse.html.ParseContext;
import org.apache.html.dom.HTMLDocumentImpl;
import org.cyberneko.html.parsers.DOMFragmentParser;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

import static ai.platon.pulsar.common.config.CapabilityTypes.METATAG_NAMES;
import static org.junit.Assert.*;

/**
 * TODO: Test failed
 * */
@Ignore("Failed")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/test-context/parse-beans.xml"})
public class TestMetaTagsParser {

    @Autowired
    private ImmutableConfig immutableConfig;

    @Autowired
    private PageParser pageParser;

    @Autowired
    private MetaTagsParser metaTagsParser;

    private MutableConfig conf;

    private String sampleDir = System.getProperty("test.data", ".");
    private String sampleFile = "testMetatags.html";
    private String description = "This is a test of description";
    private String keywords = "This is a test of keywords";

    public static void getMetaTagsHelper(HTMLMetaTags metaTags, Node node, URL currURL) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            if ("body".equalsIgnoreCase(node.getNodeName())) {
                // META tags should not be under body
                return;
            }

            if ("meta".equalsIgnoreCase(node.getNodeName())) {

                NamedNodeMap attrs = node.getAttributes();
                Node nameNode = null;
                Node equivNode = null;
                Node contentNode = null;
                // Retrieves name, http-equiv and content attribues
                for (int i = 0; i < attrs.getLength(); i++) {
                    Node attr = attrs.item(i);
                    String attrName = attr.getNodeName().toLowerCase();
                    if (attrName.equals("name")) {
                        nameNode = attr;
                    } else if (attrName.equals("http-equiv")) {
                        equivNode = attr;
                    } else if (attrName.equals("content")) {
                        contentNode = attr;
                    }
                    // System.out.println(attr.getTextContent());
                    // System.out.println(attr.getNodeValue());
                }
                if (nameNode != null) {
                    if (contentNode != null) {
                        String name = nameNode.getNodeValue().toLowerCase();
                        metaTags.getGeneralTags().put(name, contentNode.getNodeValue());
                    }
                }

                if (equivNode != null) {
                    if (contentNode != null) {
                        String name = equivNode.getNodeValue().toLowerCase();
                        String content = contentNode.getNodeValue();

                        metaTags.getHttpEquivTags().setProperty(name, content);
                    }
                }
            }
        }
        // System.out.println(metaTags);
        NodeList children = node.getChildNodes();
        if (children != null) {
            int len = children.getLength();
            for (int i = 0; i < len; i++) {
                getMetaTagsHelper(metaTags, children.item(i), currURL);
            }
        }
    }

    private static DocumentFragment getDOMDocument(byte[] content) throws IOException, SAXException {
        InputSource input = new InputSource(new ByteArrayInputStream(content));
        input.setEncoding("utf-8");
        DOMFragmentParser parser = new DOMFragmentParser();
        DocumentFragment node = new HTMLDocumentImpl().createDocumentFragment();
        parser.parse(input, node);
        return node;
    }

    @Before
    public void setup() {
        conf = new MutableConfig(immutableConfig);
        conf.set(METATAG_NAMES, "*");
        metaTagsParser.reload(conf);
    }

    /**
     * This test use parse-html with other parse filters.
     */
    @Test
    public void testMetaTagsParserWithConf() {
        // check that we get the same values
        Map<String, String> metadata = parseMetaTags(sampleFile, false);

        assertEquals(description, metadata.get(MetaTagsParser.PARSE_META_PREFIX + "description"));
        assertEquals(keywords, metadata.get(MetaTagsParser.PARSE_META_PREFIX + "keywords"));
    }

    /**
     * This test generate custom DOM tree without parse-html for testing just
     * parse-metatags.
     */
    @Test
    public void testFilter() {
        // check that we get the same values
        Map<String, String> metadata = parseMetaTags(sampleFile, false);

        assertEquals(description, metadata.get(MetaTagsParser.PARSE_META_PREFIX + "description"));
        assertEquals(keywords, metadata.get(MetaTagsParser.PARSE_META_PREFIX + "keywords"));
    }

    /**
     * @param fileName      This variable set test file.
     * @param usePageParser If value is True method use PageParser
     * @return If successfully document parsed, it return metatags
     */
    public Map<String, String> parseMetaTags(String fileName, boolean usePageParser) {
        try {
            Path path = Paths.get(sampleDir, "metatags", "sample", fileName);
            String urlString = "file:" + path.toAbsolutePath();
            byte[] bytes = Files.readAllBytes(path);

            WebPage page = WebPage.newWebPage(urlString);
            page.setBaseUrl(page.getUrl());
            page.setContent(bytes);
            page.setContentType("text/html");

            if (usePageParser) {
                pageParser.parse(page);
            } else {
                DocumentFragment node = getDOMDocument(bytes);
                URL baseUrl = Urls.getURLOrNull(urlString);
                HTMLMetaTags metaTags = new HTMLMetaTags(node, baseUrl);

                ParseResult parseResult = new ParseResult();
                metaTagsParser.filter(new ParseContext(page, metaTags, node, parseResult));
                assertTrue(parseResult.isParsed());
            }

            // System.out.println(page.getContentAsString());

            return page.getMetadata().asStringMap();
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.toString());
        }

        return Collections.emptyMap();
    }
}
