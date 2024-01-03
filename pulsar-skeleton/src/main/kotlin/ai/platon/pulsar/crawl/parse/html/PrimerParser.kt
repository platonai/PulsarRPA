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
package ai.platon.pulsar.crawl.parse.html

import ai.platon.pulsar.common.DomUtil
import ai.platon.pulsar.common.EncodingDetector
import ai.platon.pulsar.common.NodeWalker
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.config.AppConstants.CACHING_FORBIDDEN_CONTENT
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.CapabilityTypes.PARSE_CACHING_FORBIDDEN_POLICY
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.urls.UrlUtils.resolveURL
import ai.platon.pulsar.crawl.filter.CrawlFilters
import ai.platon.pulsar.crawl.parse.ParseResult
import ai.platon.pulsar.crawl.parse.Parser
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.HyperlinkPersistable
import ai.platon.pulsar.persist.ParseStatus
import ai.platon.pulsar.persist.WebPage
import com.google.common.collect.Maps
import org.jsoup.helper.W3CDom
import org.jsoup.nodes.Document
import org.slf4j.LoggerFactory
import org.w3c.dom.DocumentFragment
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.net.MalformedURLException
import java.net.URL

/**
 * A very simple DOM parser
 *
 * A collection of methods for extracting content from DOM trees.
 *
 * This class holds a few utility methods for pulling content out of DOM nodes,
 * such as getLiveLinks, getPageText, etc.
 */
class PrimerParser(val conf: ImmutableConfig) {
    private val logger = LoggerFactory.getLogger(Parser::class.java)
    private val tracer = logger.takeIf { it.isTraceEnabled }

    private val cachingPolicy = conf.get(PARSE_CACHING_FORBIDDEN_POLICY, CACHING_FORBIDDEN_CONTENT)
    private var encodingDetector = EncodingDetector(conf)
    private val linkParams = HashMap<String, LinkParams>()

    init {
        // forceTags is used to override configurable tag ignoring, later on
        val forceTags = arrayListOf<String>()
        linkParams.clear()
        linkParams["a"] = LinkParams("a", "href", 1)
        linkParams["area"] = LinkParams("area", "href", 0)
        if (conf.getBoolean("parser.html.form.use_action", true)) {
            linkParams["form"] = LinkParams("form", "action", 1)
            if (conf["parser.html.form.use_action"] != null) {
                forceTags.add("form")
            }
        }
        linkParams["frame"] = LinkParams("frame", "src", 0)
        linkParams["iframe"] = LinkParams("iframe", "src", 0)
        linkParams["script"] = LinkParams("script", "src", 0)
        linkParams["link"] = LinkParams("link", "href", 0)
        linkParams["img"] = LinkParams("img", "src", 0)
        // remove unwanted link tags from the linkParams map
        val ignoreTags = conf.getStrings("parser.html.outlinks.ignore_tags")
        var i = 0
        while (i < ignoreTags.size) {
            if (!forceTags.contains(ignoreTags[i])) {
                linkParams.remove(ignoreTags[i])
            }
            i++
        }
    }

    fun detectEncoding(page: WebPage) {
        val encoding = encodingDetector.sniffEncoding(page)
        if (encoding != null && encoding.isNotEmpty()) {
            page.encoding = encoding
        } else {
            logger.warn("Failed to detect encoding, url: " + page.url)
        }
    }

    @Throws(Exception::class)
    fun parseHTMLDocument(page: WebPage): ParseContext {
        tracer?.trace("{}.\tParsing page | {} | {} | {} | {}",
            page.id, Strings.compactFormat(page.contentLength),
            page.protocolStatus, page.htmlIntegrity, page.url
        )

        if (page.encoding == null) {
            detectEncoding(page)
        }

        val jsoupParser = JsoupParser(page, conf)
        jsoupParser.parse()

        return ParseContext(page, jsoupParser.document)
    }

    private fun initParseResult(metaTags: HTMLMetaTags): ParseResult {
        if (metaTags.noIndex) {
            return ParseResult(ParseStatus.SUCCESS, ParseStatus.SUCCESS_NO_INDEX)
        }

        val parseResult = ParseResult(ParseStatus.SUCCESS, ParseStatus.SUCCESS_OK)
        if (metaTags.refresh) {
            parseResult.minorCode = ParseStatus.SUCCESS_REDIRECT
            parseResult.args[ParseStatus.REFRESH_HREF] = metaTags.refreshHref.toString()
            parseResult.args[ParseStatus.REFRESH_TIME] = metaTags.refreshTime.toString()
        }

        return parseResult
    }

    /**
     * This method takes a [StringBuilder] and a DOM [Node], and will
     * append all the content text found beneath the DOM node to the
     * `StringBuilder`.
     *
     * If `abortOnNestedAnchors` is true, DOM traversal will be aborted
     * and the `StringBuffer` will not contain any text encountered
     * after a nested anchor is found.
     *
     * @return true if nested anchors were found
     */
    fun getPageText(sb: StringBuilder, root: Node, abortOnNestedAnchors: Boolean): Boolean {
        return getTextHelper(sb, root, abortOnNestedAnchors, 0)
    }

    /**
     * This is a convinience method, equivalent to
     * [getPageText(sb, node, false)][.getPageText].
     */
    fun getPageText(sb: StringBuilder, root: Node) {
        getPageText(sb, root, false)
    }

    fun getPageText(root: Node): String {
        val sb = StringBuilder()
        getPageText(sb, root, false)
        return sb.toString()
    }

    fun getPageTitle(root: Node): String {
        val sb = StringBuilder()
        getPageTitle(sb, root)
        return sb.toString()
    }

    /**
     * This method takes a [StringBuffer] and a DOM [Node], and will
     * append the content text found beneath the first `title` node to
     * the `StringBuffer`.
     *
     * @return true if a title node was found, false otherwise
     */
    private fun getPageTitle(sb: StringBuilder, root: Node): Boolean {
        val walker = NodeWalker(root)

        while (walker.hasNext()) {
            val node = walker.nextNode()
            val name = node.nodeName
            val type = node.nodeType

            // stop after HEAD
            if ("body".equals(name, ignoreCase = true)) {
                return false
            }

            if (type == Node.ELEMENT_NODE) {
                if ("title".equals(name, ignoreCase = true)) {
                    getPageText(sb, node)
                    return true
                }
            }
        }

        return false
    }

    fun getMetadata(root: Node): Map<String, String> {
        val metadata: MutableMap<String, String> = Maps.newLinkedHashMap()
        val sb = StringBuilder()
        val walker = NodeWalker(root)
        while (walker.hasNext()) {
            val currentNode = walker.nextNode()
            val nodeName = currentNode.nodeName
            val nodeType = currentNode.nodeType

            // stop after HEAD
            if ("body".equals(nodeName, ignoreCase = true)) {
                return metadata
            }

            if (nodeType == Node.ELEMENT_NODE) {
                if ("title".equals(nodeName, ignoreCase = true)) {
                    sb.setLength(0)
                    getPageText(sb, currentNode)
                    metadata["meta-title"] = sb.toString()
                } else if ("meta".equals(nodeName, ignoreCase = true)) {
                    getMetadataFromMetaTag(metadata, root)
                }
            } // if nodeType ...
        }

        return metadata
    }

    private fun getMetadataFromMetaTag(metadata: MutableMap<String, String>, metaNode: Node) {
        var attrValue: String? = DomUtil.getAttribute(metaNode, "name") ?: return
        if ("keywords".equals(attrValue, ignoreCase = true)) {
            attrValue = DomUtil.getAttribute(metaNode, "content")
            if (attrValue != null) {
                metadata["meta-keywords"] = attrValue
            }
        } else if ("description".equals(attrValue, ignoreCase = true)) {
            attrValue = DomUtil.getAttribute(metaNode, "content")
            if (attrValue != null) {
                metadata["meta-description"] = attrValue
            }
        }
    }

    /**
     * If Node contains a BASE tag then it's HREF is returned.
     */
    fun getBaseURLFromTag(root: Node): URL? {
        val walker = NodeWalker(root)
        while (walker.hasNext()) {
            val currentNode = walker.nextNode()
            val nodeName = currentNode.nodeName
            val nodeType = currentNode.nodeType
            // is this root a BASE tag?
            if (nodeType == Node.ELEMENT_NODE) {
                if ("body".equals(nodeName, ignoreCase = true)) { // stop after HEAD
                    return null
                }
                if ("base".equals(nodeName, ignoreCase = true)) {
                    val attrs = currentNode.attributes
                    for (i in 0 until attrs.length) {
                        val attr = attrs.item(i)
                        if ("href".equals(attr.nodeName, ignoreCase = true)) {
                            try {
                                return URL(attr.nodeValue)
                            } catch (ignored: MalformedURLException) {
                            }
                        }
                    }
                }
            }
        }
        // no.
        return null
    }

    // returns true if abortOnNestedAnchors is true and we find nested
    // anchors
    private fun getTextHelper(sb: StringBuilder, root: Node, abortOnNestedAnchors: Boolean, anchorDepth_: Int): Boolean {
        var anchorDepth = anchorDepth_
        var abort = false
        val walker = NodeWalker(root)
        while (walker.hasNext()) {
            val currentNode = walker.nextNode()
            val nodeName = currentNode.nodeName
            val nodeType = currentNode.nodeType
            if ("script".equals(nodeName, ignoreCase = true)) {
                walker.skipChildren()
            }
            if ("style".equals(nodeName, ignoreCase = true)) {
                walker.skipChildren()
            }
            if (abortOnNestedAnchors && "a".equals(nodeName, ignoreCase = true)) {
                anchorDepth++
                if (anchorDepth > 1) {
                    abort = true
                    break
                }
            }
            if (nodeType == Node.COMMENT_NODE) {
                walker.skipChildren()
            }
            if (nodeType == Node.TEXT_NODE) { // cleanup and trim the value
                var text = currentNode.nodeValue
                text = text.replace("\\s+".toRegex(), " ")
                text = text.trim { it <= ' ' }
                if (text.isNotEmpty()) {
                    if (sb.isNotEmpty()) {
                        sb.append(' ')
                    }
                    sb.append(text)
                }
            }
        }
        return abort
    }

    private fun hasOnlyWhiteSpace(root: Node): Boolean {
        for (element in root.nodeValue) {
            if (!Character.isWhitespace(element)) return false
        }
        return true
    }

    // this only covers a few cases of empty links that are symptomatic
    // of nekohtml's DOM-fixup process...
    private fun shouldThrowAwayLink(root: Node, children: NodeList, childLen: Int, params: LinkParams): Boolean {
        if (childLen == 0) { // this has no inner structure
            return params.childLen != 0
        } else if (childLen == 1
                && children.item(0).nodeType == Node.ELEMENT_NODE
                && params.elName.equals(children.item(0).nodeName, ignoreCase = true)) { // single nested link
            return true
        } else if (childLen == 2) {
            val c0 = children.item(0)
            val c1 = children.item(1)
            if (c0.nodeType == Node.ELEMENT_NODE
                    && params.elName.equals(c0.nodeName, ignoreCase = true)
                    && c1.nodeType == Node.TEXT_NODE && hasOnlyWhiteSpace(c1)) { // single link followed by whitespace root
                return true
            }
            if (c1.nodeType == Node.ELEMENT_NODE
                    && params.elName.equals(c1.nodeName, ignoreCase = true)
                    && c0.nodeType == Node.TEXT_NODE && hasOnlyWhiteSpace(c0)) { // whitespace root followed by single link
                return true
            }
        } else if (childLen == 3) {
            val c0 = children.item(0)
            val c1 = children.item(1)
            val c2 = children.item(2)
            if (c1.nodeType == Node.ELEMENT_NODE
                    && params.elName.equals(c1.nodeName, ignoreCase = true)
                    && c0.nodeType == Node.TEXT_NODE
                    && c2.nodeType == Node.TEXT_NODE && hasOnlyWhiteSpace(c0)
                    && hasOnlyWhiteSpace(c2)) { // single link surrounded by whitespace nodes
                return true
            }
        }

        return false
    }

    /**
     * This method finds all anchors below the supplied DOM `root`, and
     * creates appropriate [HyperlinkPersistable] records for each (relative to the
     * supplied `base` URL), and adds them to the `outlinks`
     * [ArrayList].
     *
     * Links without inner structure (tags, text, etc) are discarded, as are links
     * which contain only single nested links and empty text nodes (this is a
     * common DOM-fixup artifact, at least with nekohtml).
     */
    fun collectLinks(base: URL, root: Node): MutableSet<HyperlinkPersistable> {
        return collectLinks(base, root, null)
    }

    fun collectLinks(base: URL, root: Node, crawlFilters: CrawlFilters?): MutableSet<HyperlinkPersistable> {
        return collectLinks(base, mutableSetOf(), root, crawlFilters)
    }

    fun collectLinks(base: URL, hyperlinks: MutableSet<HyperlinkPersistable>, root: Node, crawlFilters: CrawlFilters?): MutableSet<HyperlinkPersistable> {
        val walker = NodeWalker(root)
        while (walker.hasNext()) {
            val currentNode = walker.nextNode()
            if (crawlFilters == null || crawlFilters.isAllowed(currentNode)) {
                getLinksStep2(base, hyperlinks, currentNode, crawlFilters)
                walker.skipChildren()
            } else {
                logger.debug("Block disallowed, skip : " + DomUtil.getPrettyName(currentNode))
            }
        }

        return hyperlinks
    }

    private fun getLinksStep2(base: URL, hyperlinks: MutableSet<HyperlinkPersistable>, root: Node, crawlFilters: CrawlFilters?) {
        val walker = NodeWalker(root)
        // log.debug("Get hypeLinks for " + DomUtil.getPrettyName(root));
        while (walker.hasNext()) {
            val currentNode = walker.nextNode()
            if (crawlFilters != null && crawlFilters.isDisallowed(currentNode)) {
                logger.debug("Block disallowed, skip : " + DomUtil.getPrettyName(currentNode))
                walker.skipChildren()
                continue
            }

            var nodeName = currentNode.nodeName
            val nodeType = currentNode.nodeType
            val children = currentNode.childNodes
            val childLen = children?.length ?: 0
            if (nodeType == Node.ELEMENT_NODE) {
                nodeName = nodeName.toLowerCase()
                val params = linkParams[nodeName]
                if (params != null) {
                    if (!shouldThrowAwayLink(currentNode, children, childLen, params)) {
                        val linkText = StringBuilder()
                        getPageText(linkText, currentNode, true)
                        val attrs = currentNode.attributes
                        var target: String? = null
                        var noFollow = false
                        var post = false
                        var allow = true
                        for (i in 0 until attrs.length) {
                            val attr = attrs.item(i)
                            val attrName = attr.nodeName
                            if (params.attrName.equals(attrName, ignoreCase = true)) {
                                target = attr.nodeValue
                            } else if ("rel".equals(attrName, ignoreCase = true)
                                    && "nofollow".equals(attr.nodeValue, ignoreCase = true)) {
                                noFollow = true
                            } else if ("rel".equals(attrName, ignoreCase = true)
                                    && "qi-nofollow".equals(attr.nodeValue, ignoreCase = true)) {
                                allow = false
                            } else if ("method".equals(attrName, ignoreCase = true)
                                    && "post".equals(attr.nodeValue, ignoreCase = true)) {
                                post = true
                            }
                        }

                        if (target != null && !noFollow && !post) try {
                            val url = resolveURL(base, target)
                            hyperlinks.add(HyperlinkPersistable(url.toString(), linkText.toString().trim { it <= ' ' }))
                        } catch (ignored: MalformedURLException) {
                        }
                    } // if not should throw away

                    // this should not have any children, skip them
                    if (params.childLen == 0) {
                    }
                }
            }
        }
    }

    private class LinkParams(var elName: String, var attrName: String, var childLen: Int) {
        override fun toString(): String {
            return "LP[el=$elName,attr=$attrName,len=$childLen]"
        }
    }
}
