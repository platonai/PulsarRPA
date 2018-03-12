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
package org.warps.pulsar.parse.tika;

import org.apache.commons.lang3.StringUtils;
import org.apache.html.dom.HTMLDocumentImpl;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.html.HtmlMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DocumentFragment;
import org.warps.pulsar.common.config.ImmutableConfig;
import org.warps.pulsar.crawl.filter.CrawlFilters;
import org.warps.pulsar.crawl.parse.ParseFilters;
import org.warps.pulsar.crawl.parse.ParseResult;
import org.warps.pulsar.crawl.parse.html.HTMLMetaTags;
import org.warps.pulsar.crawl.parse.html.ParseContext;
import org.warps.pulsar.crawl.parse.html.PrimerParser;
import org.warps.pulsar.persist.HypeLink;
import org.warps.pulsar.persist.ParseStatus;
import org.warps.pulsar.persist.WebPage;
import org.warps.pulsar.persist.metadata.ParseStatusCodes;

import java.io.ByteArrayInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import static org.warps.pulsar.common.PulsarConstants.CACHING_FORBIDDEN_CONTENT;
import static org.warps.pulsar.common.config.CapabilityTypes.CACHING_FORBIDDEN_KEY;
import static org.warps.pulsar.persist.ParseStatus.REFRESH_HREF;
import static org.warps.pulsar.persist.ParseStatus.REFRESH_TIME;

/**
 * Wrapper for Tika parsers. Mimics the HTMLParser but using the XHTML
 * representation returned by Tika as SAX events
 ***/
public class TikaParser implements org.warps.pulsar.crawl.parse.Parser {

    public static final Logger LOG = LoggerFactory.getLogger(TikaParser.class);

    private ImmutableConfig conf;
    private CrawlFilters crawlFilters;
    private ParseFilters parseFilters;

    private TikaConfig tikaConfig;
    private PrimerParser primerParser;
    private String cachingPolicy;

    private HtmlMapper HTMLMapper;

    public TikaParser() {
    }

    public TikaParser(ImmutableConfig conf) {
        this(new CrawlFilters(conf), new ParseFilters(conf), conf);
    }

    public TikaParser(ParseFilters parseFilters, ImmutableConfig conf) {
        this(new CrawlFilters(conf), parseFilters, conf);
    }

    public TikaParser(CrawlFilters crawlFilters, ParseFilters parseFilters, ImmutableConfig conf) {
        this.crawlFilters = crawlFilters;
        this.parseFilters = parseFilters;
        reload(conf);
    }

    public ParseFilters getParseFilters() {
        return parseFilters;
    }

    public void setParseFilters(ParseFilters parseFilters) {
        this.parseFilters = parseFilters;
    }

    public CrawlFilters getCrawlFilters() {
        return crawlFilters;
    }

    public void setCrawlFilters(CrawlFilters crawlFilters) {
        this.crawlFilters = crawlFilters;
    }

    @Override
    public void reload(ImmutableConfig conf) {
        this.conf = conf;
        this.primerParser = new PrimerParser(conf);

        try {
            tikaConfig = TikaConfig.getDefaultConfig();
        } catch (Exception e2) {
            String message = "Problem loading default Tika configuration";
            LOG.error(message, e2);
            throw new RuntimeException(e2);
        }

        // use a custom html mapper
        String htmlmapperClassName = conf.get("tika.htmlmapper.classname");
        if (StringUtils.isNotBlank(htmlmapperClassName)) {
            try {
                Class HTMLMapperClass = Class.forName(htmlmapperClassName);
                boolean interfaceOK = HtmlMapper.class.isAssignableFrom(HTMLMapperClass);
                if (!interfaceOK) {
                    throw new RuntimeException("Class " + htmlmapperClassName + " does not implement HtmlMapper");
                }
                HTMLMapper = (HtmlMapper) HTMLMapperClass.newInstance();
            } catch (Exception e) {
                LOG.error("Can't generate instance for class " + htmlmapperClassName);
                throw new RuntimeException("Can't generate instance for class " + htmlmapperClassName);
            }
        }

        this.cachingPolicy = getConf().get("parser.caching.forbidden.policy", CACHING_FORBIDDEN_CONTENT);
    }

    @Override
    public ParseResult parse(WebPage page) {
        String baseUrl = page.getBaseUrl();
        URL base;
        try {
            base = new URL(baseUrl);
        } catch (MalformedURLException e) {
            return ParseResult.failed(e);
        }

        // get the right parser using the mime type as a clue
        String mimeType = page.getContentType();
        Parser parser = tikaConfig.getParser(mimeType);

        if (parser == null) {
            return ParseResult.failed(ParseStatus.FAILED_NO_PARSER, mimeType);
        }

        Metadata tikamd = new Metadata();

        HTMLDocumentImpl doc = new HTMLDocumentImpl();
        doc.setErrorChecking(false);
        DocumentFragment root = doc.createDocumentFragment();
        DOMBuilder domhandler = new DOMBuilder(doc, root);
        org.apache.tika.parser.ParseContext context = new org.apache.tika.parser.ParseContext();
        if (HTMLMapper != null) {
            context.set(HtmlMapper.class, HTMLMapper);
        }
        // to add once available in Tika
        // context.set(HtmlMapper.class, IdentityHtmlMapper.INSTANCE);
        tikamd.set(Metadata.CONTENT_TYPE, mimeType);
        try {
            ByteBuffer raw = page.getContent();
            parser.parse(new ByteArrayInputStream(raw.array(), raw.arrayOffset()
                    + raw.position(), raw.remaining()), domhandler, tikamd, context);
        } catch (Exception e) {
            LOG.error("Error parsing " + page.getUrl(), e);
            return ParseResult.failed(e);
        }

        String pageTitle = "";
        String pageText = "";
        ArrayList<HypeLink> hypeLinks = new ArrayList<>();

        // we have converted the sax events generated by Tika into a DOM object
        HTMLMetaTags metaTags = new HTMLMetaTags(root, base);

        // check meta directives
        if (!metaTags.getNoIndex()) { // okay to index
            pageText = primerParser.getPageText(root); // extract text
            pageTitle = primerParser.getPageTitle(root); // extract title
        }

        if (!metaTags.getNoFollow()) { // okay to follow links
            URL baseTag = primerParser.getBaseURL(root);
            primerParser.getLinks(baseTag != null ? baseTag : base, hypeLinks, root, null);
        }

        page.setPageTitle(pageTitle);
        page.setPageText(pageText);

        for (String name : tikamd.names()) {
            if (name.equalsIgnoreCase(TikaCoreProperties.TITLE.toString())) {
                continue;
            }
            page.getMetadata().set(name, tikamd.get(name));
        }

        // no hypeLinks? try OutlinkExtractor e.g works for mime types where no
        // explicit markup for anchors

        ParseResult parseResult = new ParseResult(ParseStatusCodes.SUCCESS, ParseStatusCodes.SUCCESS_OK);
//    if (hypeLinks.isEmpty()) {
//      hypeLinks = OutlinkExtractor.getLiveLinks(pageText, getConf());
//      parseResult.getLiveLinks().addAll(hypeLinks);
//    }

        if (metaTags.getRefresh()) {
            parseResult.setMinorCode(ParseStatusCodes.SUCCESS_REDIRECT);
            parseResult.getArgs().put(REFRESH_HREF, metaTags.getRefreshHref().toString());
            parseResult.getArgs().put(REFRESH_TIME, Integer.toString(metaTags.getRefreshTime()));
        }

        parseFilters.filter(new ParseContext(page, metaTags, root, parseResult));

        if (metaTags.getNoCache()) {
            // not okay to cache
            page.getMetadata().set(CACHING_FORBIDDEN_KEY, cachingPolicy);
        }

        return parseResult;
    }

    @Override
    public ImmutableConfig getConf() {
        return this.conf;
    }

    public TikaConfig getTikaConfig() {
        return this.tikaConfig;
    }
}
