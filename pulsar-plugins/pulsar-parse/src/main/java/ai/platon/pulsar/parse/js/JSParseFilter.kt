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
package ai.platon.pulsar.parse.js

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.crawl.parse.ParseFilter
import ai.platon.pulsar.crawl.parse.ParseResult
import ai.platon.pulsar.crawl.parse.ParseResult.Companion.failed
import ai.platon.pulsar.crawl.parse.Parser
import ai.platon.pulsar.crawl.parse.html.HTMLMetaTags
import ai.platon.pulsar.crawl.parse.html.ParseContext
import ai.platon.pulsar.persist.HypeLink
import ai.platon.pulsar.persist.ParseStatus
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.ParseStatusCodes
import org.apache.oro.text.regex.*
import org.slf4j.LoggerFactory
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.net.MalformedURLException
import java.net.URL
import java.util.*

/**
 * This class is a heuristic link extractor for JavaScript files and code
 * snippets. The general idea of a two-pass regex matching comes from Heritrix.
 * Parts of the code come from OutlinkExtractor.java by Stephan Strittmatter.
 *
 * @author Andrzej Bialecki &lt;ab@getopt.org&gt;
 */
class JSParseFilter(val conf: ImmutableConfig) : ParseFilter, Parser {

    /**
     * Scan the JavaScript looking for possible [HypeLink]'s
     *
     * @param parseContext Context of parse.
     */
    override fun filter(parseContext: ParseContext) {
        walk(parseContext.documentFragment, parseContext.metaTags, parseContext.url, parseContext.parseResult.hypeLinks)
    }

    private fun walk(n: Node, metaTags: HTMLMetaTags, base: String, hypeLinks: MutableList<HypeLink>) {
        if (n is Element) {
            val name = n.getNodeName()
            if (name.equals("script", ignoreCase = true)) {
                val script = StringBuilder()
                val nn = n.getChildNodes()
                for (i in 0 until nn.length) {
                    if (i > 0) {
                        script.append('\n')
                    }
                    script.append(nn.item(i).nodeValue)
                }
                // This logging makes the output very messy.
                // if (log.isInfoEnabled()) {
                // log.info("script: language=" + lang + ", text: " +
                // script.toString());
                // }
                hypeLinks.addAll(getJSLinks(script.toString(), "", base))
            } else { // process all HTML 4.0 events, if present...
                val attrs = n.getAttributes()
                val len = attrs.length
                for (i in 0 until len) { // Window: onload,onunload
                    // Form: onchange,onsubmit,onreset,onselect,onblur,onfocus
                    // Keyboard: onkeydown,onkeypress,onkeyup
                    // Mouse:
                    // onclick,ondbclick,onmousedown,onmouseout,onmousover,onmouseup
                    val anode = attrs.item(i)
                    var links = ArrayList<HypeLink>()
                    if (anode.nodeName.startsWith("on")) {
                        links = getJSLinks(anode.nodeValue, "", base)
                    } else if (anode.nodeName.equals("href", ignoreCase = true)) {
                        val `val` = anode.nodeValue
                        if (`val` != null && `val`.toLowerCase().contains("javascript:")) {
                            links = getJSLinks(`val`, "", base)
                        }
                    }
                    hypeLinks.addAll(links)
                }
            }
        }
        val nl = n.childNodes
        for (i in 0 until nl.length) {
            walk(nl.item(i), metaTags, base, hypeLinks)
        }
    }
    // Alternative pattern, which limits valid url characters.
    // private static final String URI_PATTERN =
    // "(^|\\s*?)[A-Za-z0-9/](([A-Za-z0-9$_.+!*,;/?:@&~=-])|%[A-Fa-f0-9]{2})+[/.](([A-Za-z0-9$_.+!*,;/?:@&~=-])|%[A-Fa-f0-9]{2})+(#([a-zA-Z0-9][a-zA-Z0-9$_.+!*,;/?:@&~=%-]*))?($|\\s*)";
    /**
     * Set the [Configuration] object
     *
     * @param page [WebPage] object relative to the URL
     * @return parse the actual [ParseResult] object
     */
    override fun parse(page: WebPage): ParseResult {
        val contentType = page.contentType
        if (!contentType.startsWith("application/x-javascript")) {
            return failed(ParseStatus.FAILED_INVALID_FORMAT, contentType)
        }
        val script = page.contentAsString
        val url = page.url
        val hypeLinks = getJSLinks(script, "", url)
        // Title? use the first line of the script...
        val title: String
        var idx = script.indexOf('\n')
        if (idx != -1) {
            if (idx > MAX_TITLE_LEN) idx = MAX_TITLE_LEN
            title = script.substring(0, idx)
        } else {
            idx = Math.min(MAX_TITLE_LEN, script.length)
            title = script.substring(0, idx)
        }
        page.pageTitle = title
        page.pageText = script
        return ParseResult(ParseStatusCodes.SUCCESS, ParseStatusCodes.SUCCESS_OK)
    }

    /**
     * This method extracts URLs from literals embedded in JavaScript.
     */
    private fun getJSLinks(plainText: String, anchor: String, base: String): ArrayList<HypeLink> {
        val hypeLinks = ArrayList<HypeLink>()
        var baseURL: URL? = null
        try {
            baseURL = URL(base)
        } catch (e: Exception) {
            if (LOG.isErrorEnabled) {
                LOG.error("error assigning base URL", e)
            }
        }
        try {
            val cp: PatternCompiler = Perl5Compiler()
            val pattern = cp.compile(STRING_PATTERN,
                    Perl5Compiler.CASE_INSENSITIVE_MASK or Perl5Compiler.READ_ONLY_MASK or Perl5Compiler.MULTILINE_MASK)
            val pattern1 = cp.compile(URI_PATTERN,
                    Perl5Compiler.CASE_INSENSITIVE_MASK or Perl5Compiler.READ_ONLY_MASK or Perl5Compiler.MULTILINE_MASK)
            val matcher: PatternMatcher = Perl5Matcher()
            val matcher1: PatternMatcher = Perl5Matcher()
            val input = PatternMatcherInput(plainText)
            var result: MatchResult
            var url: String
            // loop the matches
            while (matcher.contains(input, pattern)) {
                result = matcher.match
                url = result.group(2)
                val input1 = PatternMatcherInput(url)
                if (!matcher1.matches(input1, pattern1)) {
                    if (LOG.isTraceEnabled) {
                        LOG.trace(" - invalid '$url'")
                    }
                    continue
                }

                url = if (url.startsWith("www.")) {
                    "http://$url"
                } else {
                    // See if candidate URL is parseable. If not, pass and move on to the next match.
                    try {
                        URL(baseURL, url).toString()
                    } catch (ex: MalformedURLException) {
                        if (LOG.isTraceEnabled) {
                            LOG.trace(" - failed URL parse '$url' and baseURL '$baseURL'", ex)
                        }
                        continue
                    }
                }
                url = url.replace("&amp;".toRegex(), "&")
                if (LOG.isTraceEnabled) {
                    LOG.trace(" - outlink from JS: '$url'")
                }

                hypeLinks.add(HypeLink(url, anchor))
            }
        } catch (ex: Exception) {
            // if it is a malformed URL we just throw it away and continue with extraction.
            LOG.error(" - invalid or malformed URL", ex)
        }
        return hypeLinks
    }

    companion object {
        val LOG = LoggerFactory.getLogger(JSParseFilter::class.java)
        private const val MAX_TITLE_LEN = 80
        private const val STRING_PATTERN = "(\\\\*(?:\"|\'))([^\\s\"\']+?)(?:\\1)"
        // A simple pattern. This allows also invalid URL characters.
        private const val URI_PATTERN = "(^|\\s*?)/?\\S+?[/\\.]\\S+($|\\s*)"
    }
}
