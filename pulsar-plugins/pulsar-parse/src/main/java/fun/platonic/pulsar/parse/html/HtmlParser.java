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

package fun.platonic.pulsar.parse.html;

import fun.platonic.pulsar.common.EncodingDetector;
import fun.platonic.pulsar.common.MetricsSystem;
import fun.platonic.pulsar.common.config.ImmutableConfig;
import fun.platonic.pulsar.common.config.Params;
import fun.platonic.pulsar.crawl.filter.CrawlFilters;
import fun.platonic.pulsar.crawl.parse.ParseFilters;
import fun.platonic.pulsar.crawl.parse.ParseResult;
import fun.platonic.pulsar.crawl.parse.Parser;
import fun.platonic.pulsar.crawl.parse.html.HTMLMetaTags;
import fun.platonic.pulsar.crawl.parse.html.ParseContext;
import fun.platonic.pulsar.crawl.parse.html.PrimerParser;
import fun.platonic.pulsar.persist.Metadata;
import fun.platonic.pulsar.persist.ParseStatus;
import fun.platonic.pulsar.persist.WebDb;
import fun.platonic.pulsar.persist.WebPage;
import fun.platonic.pulsar.persist.metadata.MultiMetadata;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.w3c.dom.DocumentFragment;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import static fun.platonic.pulsar.common.PulsarParams.VAR_LINKS_COUNT;
import static fun.platonic.pulsar.common.config.CapabilityTypes.*;
import static fun.platonic.pulsar.common.config.PulsarConstants.CACHING_FORBIDDEN_CONTENT;
import static fun.platonic.pulsar.persist.ParseStatus.REFRESH_HREF;
import static fun.platonic.pulsar.persist.ParseStatus.REFRESH_TIME;
import static fun.platonic.pulsar.persist.metadata.ParseStatusCodes.FAILED_INVALID_FORMAT;
import static fun.platonic.pulsar.persist.metadata.ParseStatusCodes.FAILED_MALFORMED_URL;

/**
 * Html parser
 */
public class HtmlParser implements Parser {

    private ImmutableConfig conf;
    private CrawlFilters crawlFilters;
    private ParseFilters parseFilters;

    private String parserImpl;
    private String defaultCharEncoding;
    private EncodingDetector encodingDetector;

    private PrimerParser primerParser;
    private String cachingPolicy;

    private WebDb webDb;
    private MetricsSystem metricsSystem;

    public HtmlParser(WebDb webDb, ImmutableConfig conf) {
        this(webDb, new CrawlFilters(conf), new ParseFilters(conf), conf);
    }

    public HtmlParser(WebDb webDb, ParseFilters parseFilters, ImmutableConfig conf) {
        this(webDb, new CrawlFilters(conf), parseFilters, conf);
    }

    public HtmlParser(WebDb webDb, CrawlFilters crawlFilters, ParseFilters parseFilters, ImmutableConfig conf) {
        this.webDb = webDb;
        this.crawlFilters = crawlFilters;
        this.parseFilters = parseFilters;

        reload(conf);
    }

    @Override
    public void reload(ImmutableConfig conf) {
        this.conf = conf;

        this.parserImpl = conf.get(PARSE_HTML_IMPL, "neko");
        this.defaultCharEncoding = conf.get(PARSE_DEFAULT_ENCODING, "utf-8");
        this.cachingPolicy = conf.get(PARSE_CACHING_FORBIDDEN_POLICY, CACHING_FORBIDDEN_CONTENT);

        this.primerParser = new PrimerParser(conf);
        this.encodingDetector = new EncodingDetector(conf);
        this.metricsSystem = new MetricsSystem(webDb, conf);

        LOG.info(getParams().formatAsLine());
        LOG.info("Active parse filters : " + parseFilters);
    }

    @Override
    public ImmutableConfig getConf() {
        return this.conf;
    }

    @Override
    public Params getParams() {
        return Params.of(
                "className", this.getClass().getSimpleName(),
                "parserImpl", parserImpl,
                "defaultCharEncoding", defaultCharEncoding,
                "cachingPolicy", cachingPolicy
        );
    }

    @Override
    public ParseResult parse(WebPage page) {
        try {
            // The base url is set by protocol. Might be different from url if the request redirected.
            String url = page.getUrl();
            String baseUrl = page.getBaseUrl();
            URL baseURL = new URL(baseUrl);

            if (page.getEncoding() == null) {
                primerParser.detectEncoding(page);
            }
            InputSource inputSource = page.getContentAsSaxInputSource();
            DocumentFragment documentFragment = parse(baseUrl, inputSource);

            HTMLMetaTags metaTags = parseMetaTags(baseURL, documentFragment, page);
            ParseResult parseResult = initParseResult(metaTags);
            if (parseResult.isFailed()) {
                return parseResult;
            }

            ParseContext parseContext = new ParseContext(page, metaTags, documentFragment, parseResult);
            parseLinks(baseURL, parseContext);

            page.setPageTitle(primerParser.getPageTitle(documentFragment));
            page.setPageText(primerParser.getPageText(documentFragment));

            page.getPageModel().clear();
            parseFilters.filter(parseContext);

            return parseContext.getParseResult();
        } catch (MalformedURLException e) {
            return ParseResult.failed(FAILED_MALFORMED_URL, e.getMessage());
        } catch (Exception e) {
            return ParseResult.failed(FAILED_INVALID_FORMAT, e.getMessage());
        }
    }

    private HTMLMetaTags parseMetaTags(URL baseURL, DocumentFragment docRoot, WebPage page) {
        HTMLMetaTags metaTags = new HTMLMetaTags(docRoot, baseURL);

        MultiMetadata tags = metaTags.getGeneralTags();
        Metadata metadata = page.getMetadata();

        tags.names().forEach(name -> metadata.set("meta_" + name, tags.get(name)));
        if (metaTags.getNoCache()) {
            metadata.set(CACHING_FORBIDDEN_KEY, cachingPolicy);
        }

        return metaTags;
    }

    private void parseLinks(URL baseURL, ParseContext parseContext) {
        String url = parseContext.getUrl();
        url = crawlFilters.normalizeToEmpty(url);
        if (url.isEmpty()) {
            return;
        }

        WebPage page = parseContext.getPage();
        HTMLMetaTags metaTags = parseContext.getMetaTags();
        DocumentFragment docRoot = parseContext.getDocumentFragment();
        ParseResult parseResult = parseContext.getParseResult();

        if (!metaTags.getNoFollow()) { // okay to follow links
            URL baseURLFromTag = primerParser.getBaseURL(docRoot);
            baseURL = baseURLFromTag != null ? baseURLFromTag : baseURL;
            primerParser.getLinks(baseURL, parseResult.getHypeLinks(), docRoot, crawlFilters);
        }

        page.increaseImpreciseLinkCount(parseResult.getHypeLinks().size());
        page.getVariables().set(VAR_LINKS_COUNT, parseResult.getHypeLinks().size());
    }

    private ParseResult initParseResult(HTMLMetaTags metaTags) {
        if (metaTags.getNoIndex()) {
            return new ParseResult(ParseStatus.SUCCESS, ParseStatus.SUCCESS_NO_INDEX);
        }

        ParseResult parseResult = new ParseResult(ParseStatus.SUCCESS, ParseStatus.SUCCESS_OK);
        if (metaTags.getRefresh()) {
            parseResult.setMinorCode(ParseStatus.SUCCESS_REDIRECT);
            parseResult.getArgs().put(REFRESH_HREF, metaTags.getRefreshHref().toString());
            parseResult.getArgs().put(REFRESH_TIME, Integer.toString(metaTags.getRefreshTime()));
        }

        return parseResult;
    }

    private DocumentFragment parse(String baseUri, InputSource input) throws Exception {
        if (parserImpl.equalsIgnoreCase("neko")) {
            return parseNeko(baseUri, input);
        } else {
            return parseJsoup(baseUri, input);
        }
    }

    private DocumentFragment parseJsoup(String baseUri, InputSource input) throws IOException {
        Document doc = Jsoup.parse(input.getByteStream(), input.getEncoding(), baseUri);
        org.jsoup.helper.W3CDom dom = new org.jsoup.helper.W3CDom();
        return dom.fromJsoup(doc).createDocumentFragment();
    }

    private DocumentFragment parseNeko(String baseUri, InputSource input) throws Exception {
        org.cyberneko.html.parsers.DOMFragmentParser parser = new org.cyberneko.html.parsers.DOMFragmentParser();
        try {
            parser.setFeature("http://cyberneko.org/html/features/scanner/allow-selfclosing-iframe", true);
            parser.setFeature("http://cyberneko.org/html/features/augmentations", true);
            parser.setProperty("http://cyberneko.org/html/properties/default-encoding", defaultCharEncoding);
            parser.setFeature("http://cyberneko.org/html/features/scanner/ignore-specified-charset", true);
            parser.setFeature("http://cyberneko.org/html/features/balance-tags/ignore-outside-content", false);
            parser.setFeature("http://cyberneko.org/html/features/balance-tags/document-fragment", true);
            parser.setFeature("http://cyberneko.org/html/features/report-errors", LOG.isTraceEnabled());
        } catch (SAXException ignored) {
        }

        // convert Document to DocumentFragment
        org.apache.html.dom.HTMLDocumentImpl doc = new org.apache.html.dom.HTMLDocumentImpl();
        doc.setErrorChecking(false);
        DocumentFragment res = doc.createDocumentFragment();
        DocumentFragment frag = doc.createDocumentFragment();
        parser.parse(input, frag);
        res.appendChild(frag);

        try {
            while (true) {
                frag = doc.createDocumentFragment();
                parser.parse(input, frag);
                if (!frag.hasChildNodes())
                    break;
                if (LOG.isInfoEnabled()) {
                    LOG.info(" - new frag, " + frag.getChildNodes().getLength() + " nodes.");
                }
                res.appendChild(frag);
            }
        } catch (Exception x) {
            LOG.error("Failed with the following Exception: ", x);
        }

        return res;
    }
}
