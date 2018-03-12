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
package org.warps.pulsar.parse.js;

import org.apache.hadoop.conf.Configuration;
import org.apache.oro.text.regex.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.warps.pulsar.common.config.ImmutableConfig;
import org.warps.pulsar.crawl.parse.ParseFilter;
import org.warps.pulsar.crawl.parse.ParseResult;
import org.warps.pulsar.crawl.parse.Parser;
import org.warps.pulsar.crawl.parse.html.HTMLMetaTags;
import org.warps.pulsar.crawl.parse.html.ParseContext;
import org.warps.pulsar.persist.HypeLink;
import org.warps.pulsar.persist.ParseStatus;
import org.warps.pulsar.persist.WebPage;
import org.warps.pulsar.persist.metadata.ParseStatusCodes;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is a heuristic link extractor for JavaScript files and code
 * snippets. The general idea of a two-pass regex matching comes from Heritrix.
 * Parts of the code come from OutlinkExtractor.java by Stephan Strittmatter.
 *
 * @author Andrzej Bialecki &lt;ab@getopt.org&gt;
 */
public class JSParseFilter implements ParseFilter, Parser {
    public static final Logger LOG = LoggerFactory.getLogger(JSParseFilter.class);

    private static final int MAX_TITLE_LEN = 80;
    private static final String STRING_PATTERN = "(\\\\*(?:\"|\'))([^\\s\"\']+?)(?:\\1)";
    // A simple pattern. This allows also invalid URL characters.
    private static final String URI_PATTERN = "(^|\\s*?)/?\\S+?[/\\.]\\S+($|\\s*)";
    private ImmutableConfig conf;

    public JSParseFilter(ImmutableConfig conf) {
        reload(conf);
    }

    /**
     * Main method which can be run from command line with the plugin option. The
     * method takes two arguments e.g. o.a.n.parse.js.JSParseFilter file.js
     * baseURL
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println(JSParseFilter.class.getName() + " file.js baseURL");
            return;
        }

        InputStream in = new FileInputStream(args[0]);
        BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line).append("\n");
        }

        JSParseFilter parseFilter = new JSParseFilter(new ImmutableConfig());
        ArrayList<HypeLink> links = parseFilter.getJSLinks(sb.toString(), "", args[1]);
        System.out.println("Live links extracted: " + links.size());
        links.stream().map(l -> " - " + l).forEach(System.out::println);
    }

    public void reload(ImmutableConfig conf) {
        this.conf = conf;
    }

    /**
     * Scan the JavaScript looking for possible {@link HypeLink}'s
     *
     * @param parseContext Context of parse.
     */
    @Override
    public void filter(ParseContext parseContext) {
        walk(parseContext.getDocumentFragment(),
                parseContext.getMetaTags(), parseContext.getUrl(), parseContext.getParseResult().getHypeLinks());
    }

    private void walk(Node n, HTMLMetaTags metaTags, String base, List<HypeLink> hypeLinks) {
        if (n instanceof Element) {
            String name = n.getNodeName();
            if (name.equalsIgnoreCase("script")) {
                StringBuilder script = new StringBuilder();

                NodeList nn = n.getChildNodes();
                for (int i = 0; i < nn.getLength(); i++) {
                    if (i > 0) {
                        script.append('\n');
                    }
                    script.append(nn.item(i).getNodeValue());
                }
                // This logging makes the output very messy.
                // if (LOG.isInfoEnabled()) {
                // LOG.info("script: language=" + lang + ", text: " +
                // script.toString());
                // }
                hypeLinks.addAll(getJSLinks(script.toString(), "", base));
            } else {
                // process all HTML 4.0 events, if present...
                NamedNodeMap attrs = n.getAttributes();
                int len = attrs.getLength();
                for (int i = 0; i < len; i++) {
                    // Window: onload,onunload
                    // Form: onchange,onsubmit,onreset,onselect,onblur,onfocus
                    // Keyboard: onkeydown,onkeypress,onkeyup
                    // Mouse:
                    // onclick,ondbclick,onmousedown,onmouseout,onmousover,onmouseup
                    Node anode = attrs.item(i);
                    ArrayList<HypeLink> links = new ArrayList<>();
                    if (anode.getNodeName().startsWith("on")) {
                        links = getJSLinks(anode.getNodeValue(), "", base);
                    } else if (anode.getNodeName().equalsIgnoreCase("href")) {
                        String val = anode.getNodeValue();
                        if (val != null && val.toLowerCase().contains("javascript:")) {
                            links = getJSLinks(val, "", base);
                        }
                    }

                    hypeLinks.addAll(links);
                }
            }
        }

        NodeList nl = n.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            walk(nl.item(i), metaTags, base, hypeLinks);
        }
    }

    // Alternative pattern, which limits valid url characters.
    // private static final String URI_PATTERN =
    // "(^|\\s*?)[A-Za-z0-9/](([A-Za-z0-9$_.+!*,;/?:@&~=-])|%[A-Fa-f0-9]{2})+[/.](([A-Za-z0-9$_.+!*,;/?:@&~=-])|%[A-Fa-f0-9]{2})+(#([a-zA-Z0-9][a-zA-Z0-9$_.+!*,;/?:@&~=%-]*))?($|\\s*)";

    /**
     * Set the {@link Configuration} object
     *
     * @param page {@link WebPage} object relative to the URL
     * @return parse the actual {@link ParseResult} object
     */
    @Override
    public ParseResult parse(WebPage page) {
        String contentType = page.getContentType();
        if (!contentType.startsWith("application/x-javascript")) {
            return ParseResult.failed(ParseStatus.FAILED_INVALID_FORMAT, contentType);
        }
        String script = page.getContentAsString();

        String url = page.getUrl();
        ArrayList<HypeLink> hypeLinks = getJSLinks(script, "", url);
        // Title? use the first line of the script...
        String title;
        int idx = script.indexOf('\n');
        if (idx != -1) {
            if (idx > MAX_TITLE_LEN) idx = MAX_TITLE_LEN;
            title = script.substring(0, idx);
        } else {
            idx = Math.min(MAX_TITLE_LEN, script.length());
            title = script.substring(0, idx);
        }

        page.setPageTitle(title);
        page.setPageText(script);

        return new ParseResult(ParseStatusCodes.SUCCESS, ParseStatusCodes.SUCCESS_OK);
    }

    /**
     * This method extracts URLs from literals embedded in JavaScript.
     */
    private ArrayList<HypeLink> getJSLinks(String plainText, String anchor, String base) {
        final ArrayList<HypeLink> hypeLinks = new ArrayList<>();
        URL baseURL = null;

        try {
            baseURL = new URL(base);
        } catch (Exception e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("error assigning base URL", e);
            }
        }

        try {
            final PatternCompiler cp = new Perl5Compiler();
            final Pattern pattern = cp.compile(STRING_PATTERN,
                    Perl5Compiler.CASE_INSENSITIVE_MASK | Perl5Compiler.READ_ONLY_MASK
                            | Perl5Compiler.MULTILINE_MASK);
            final Pattern pattern1 = cp.compile(URI_PATTERN,
                    Perl5Compiler.CASE_INSENSITIVE_MASK | Perl5Compiler.READ_ONLY_MASK
                            | Perl5Compiler.MULTILINE_MASK);
            final PatternMatcher matcher = new Perl5Matcher();

            final PatternMatcher matcher1 = new Perl5Matcher();
            final PatternMatcherInput input = new PatternMatcherInput(plainText);

            MatchResult result;
            String url;

            // loop the matches
            while (matcher.contains(input, pattern)) {
                result = matcher.getMatch();
                url = result.group(2);
                PatternMatcherInput input1 = new PatternMatcherInput(url);
                if (!matcher1.matches(input1, pattern1)) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace(" - invalid '" + url + "'");
                    }
                    continue;
                }
                if (url.startsWith("www.")) {
                    url = "http://" + url;
                } else {
                    // See if candidate URL is parseable. If not, pass and move on to
                    // the next match.
                    try {
                        url = new URL(baseURL, url).toString();
                    } catch (MalformedURLException ex) {
                        if (LOG.isTraceEnabled()) {
                            LOG.trace(" - failed URL parse '" + url + "' and baseURL '"
                                    + baseURL + "'", ex);
                        }
                        continue;
                    }
                }
                url = url.replaceAll("&amp;", "&");
                if (LOG.isTraceEnabled()) {
                    LOG.trace(" - outlink from JS: '" + url + "'");
                }
                hypeLinks.add(new HypeLink(url, anchor));
            }
        } catch (Exception ex) {
            // if it is a malformed URL we just throw it away and continue with
            // extraction.
            if (LOG.isErrorEnabled()) {
                LOG.error(" - invalid or malformed URL", ex);
            }
        }

        return hypeLinks;
    }

    /**
     * Get the {@link Configuration} object
     */
    public ImmutableConfig getConf() {
        return this.conf;
    }
}
