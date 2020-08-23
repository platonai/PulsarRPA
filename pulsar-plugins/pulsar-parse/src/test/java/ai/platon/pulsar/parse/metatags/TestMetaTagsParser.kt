/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.platon.pulsar.parse.metatags

import ai.platon.pulsar.common.Urls.getURLOrNull
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.MutableConfig
import ai.platon.pulsar.crawl.parse.PageParser
import ai.platon.pulsar.crawl.parse.ParseResult
import ai.platon.pulsar.crawl.parse.html.HTMLMetaTags
import ai.platon.pulsar.crawl.parse.html.ParseContext
import ai.platon.pulsar.persist.WebPage
import org.apache.html.dom.HTMLDocumentImpl
import org.cyberneko.html.parsers.DOMFragmentParser
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.w3c.dom.DocumentFragment
import org.w3c.dom.Node
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths

/**
 * TODO: Test failed
 */
@Ignore("Failed")
@RunWith(SpringJUnit4ClassRunner::class)
@ContextConfiguration(locations = ["classpath:/test-context/parse-beans.xml"])
class TestMetaTagsParser {
    @Autowired
    private val immutableConfig: ImmutableConfig? = null
    @Autowired
    private val pageParser: PageParser? = null
    @Autowired
    private val metaTagsParser: MetaTagsParser? = null
    private var conf: MutableConfig? = null
    private val sampleDir = System.getProperty("test.data", ".")
    private val sampleFile = "testMetatags.html"
    private val description = "This is a test of description"
    private val keywords = "This is a test of keywords"
    @Before
    fun setup() {
        conf = MutableConfig(immutableConfig)
        conf!![CapabilityTypes.METATAG_NAMES] = "*"
        // TODO: reload is deleted
// metaTagsParser.reload(conf);
    }

    /**
     * This test use parse-html with other parse filters.
     */
    @Test
    fun testMetaTagsParserWithConf() { // check that we get the same values
        val metadata = parseMetaTags(sampleFile, false)
        Assert.assertEquals(description, metadata[MetaTagsParser.PARSE_META_PREFIX + "description"])
        Assert.assertEquals(keywords, metadata[MetaTagsParser.PARSE_META_PREFIX + "keywords"])
    }

    /**
     * This test generate custom DOM tree without parse-html for testing just
     * parse-metatags.
     */
    @Test
    fun testFilter() { // check that we get the same values
        val metadata = parseMetaTags(sampleFile, false)
        Assert.assertEquals(description, metadata[MetaTagsParser.PARSE_META_PREFIX + "description"])
        Assert.assertEquals(keywords, metadata[MetaTagsParser.PARSE_META_PREFIX + "keywords"])
    }

    /**
     * @param fileName      This variable set test file.
     * @param usePageParser If value is True method use PageParser
     * @return If successfully document parsed, it return metatags
     */
    fun parseMetaTags(fileName: String?, usePageParser: Boolean): Map<String, String> {
        try {
            val path = Paths.get(sampleDir, "metatags", "sample", fileName)
            val urlString = "file:" + path.toAbsolutePath()
            val bytes = Files.readAllBytes(path)
            val page = WebPage.newWebPage(urlString)
            page.location = page.url
            page.setContent(bytes)
            page.contentType = "text/html"
            if (usePageParser) {
                pageParser!!.parse(page)
            } else {
                val node = getDOMDocument(bytes)
                val baseUrl = getURLOrNull(urlString)
                val metaTags = HTMLMetaTags(node, baseUrl)
                val parseResult = ParseResult()
                metaTagsParser!!.filter(ParseContext(page, parseResult, metaTags = metaTags, documentFragment = node))
                Assert.assertTrue(parseResult.isParsed)
            }
            // System.out.println(page.getContentAsString());
            return page.metadata.asStringMap()
        } catch (e: Exception) {
            e.printStackTrace()
            Assert.fail(e.toString())
        }
        return emptyMap()
    }

    companion object {
        fun getMetaTagsHelper(metaTags: HTMLMetaTags, node: Node, currURL: URL?) {
            if (node.nodeType == Node.ELEMENT_NODE) {
                if ("body".equals(node.nodeName, ignoreCase = true)) { // META tags should not be under body
                    return
                }
                if ("meta".equals(node.nodeName, ignoreCase = true)) {
                    val attrs = node.attributes
                    var nameNode: Node? = null
                    var equivNode: Node? = null
                    var contentNode: Node? = null
                    // Retrieves name, http-equiv and content attribues
                    for (i in 0 until attrs.length) {
                        val attr = attrs.item(i)
                        val attrName = attr.nodeName.toLowerCase()
                        if (attrName == "name") {
                            nameNode = attr
                        } else if (attrName == "http-equiv") {
                            equivNode = attr
                        } else if (attrName == "content") {
                            contentNode = attr
                        }
                        // System.out.println(attr.getTextContent());
// System.out.println(attr.getNodeValue());
                    }
                    if (nameNode != null) {
                        if (contentNode != null) {
                            val name = nameNode.nodeValue.toLowerCase()
                            metaTags.generalTags.put(name, contentNode.nodeValue)
                        }
                    }
                    if (equivNode != null) {
                        if (contentNode != null) {
                            val name = equivNode.nodeValue.toLowerCase()
                            val content = contentNode.nodeValue
                            metaTags.httpEquivTags.setProperty(name, content)
                        }
                    }
                }
            }
            // System.out.println(metaTags);
            val children = node.childNodes
            if (children != null) {
                val len = children.length
                for (i in 0 until len) {
                    getMetaTagsHelper(metaTags, children.item(i), currURL)
                }
            }
        }

        @Throws(IOException::class, SAXException::class)
        private fun getDOMDocument(content: ByteArray): DocumentFragment {
            val input = InputSource(ByteArrayInputStream(content))
            input.encoding = "utf-8"
            val parser = DOMFragmentParser()
            val node = HTMLDocumentImpl().createDocumentFragment()
            parser.parse(input, node)
            return node
        }
    }
}