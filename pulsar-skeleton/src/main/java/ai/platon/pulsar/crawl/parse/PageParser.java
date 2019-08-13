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
package ai.platon.pulsar.crawl.parse;

import ai.platon.pulsar.common.MetricsCounters;
import ai.platon.pulsar.common.StringUtil;
import ai.platon.pulsar.common.URLUtil;
import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.common.config.Params;
import ai.platon.pulsar.common.config.ReloadableParameterized;
import ai.platon.pulsar.crawl.filter.CrawlFilters;
import ai.platon.pulsar.crawl.filter.UrlNormalizers;
import ai.platon.pulsar.crawl.signature.Signature;
import ai.platon.pulsar.crawl.signature.TextMD5Signature;
import ai.platon.pulsar.persist.HypeLink;
import ai.platon.pulsar.persist.ParseStatus;
import ai.platon.pulsar.persist.WebPage;
import ai.platon.pulsar.persist.metadata.Mark;
import ai.platon.pulsar.persist.metadata.Name;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static ai.platon.pulsar.common.config.CapabilityTypes.PARSE_MAX_LINKS_PER_PAGE;
import static ai.platon.pulsar.common.config.CapabilityTypes.PARSE_TIMEOUT;
import static ai.platon.pulsar.common.config.PulsarConstants.*;
import static ai.platon.pulsar.persist.metadata.Name.NO_FOLLOW;
import static ai.platon.pulsar.persist.metadata.Name.REDIRECT_DISCOVERED;
import static ai.platon.pulsar.persist.metadata.ParseStatusCodes.FAILED;

public final class PageParser implements ReloadableParameterized {

    public static final Logger LOG = LoggerFactory.getLogger(PageParser.class);

    static {
        MetricsCounters.register(Counter.class);
    }

    private final ImmutableConfig conf;
    private MetricsCounters metricsCounters;
    private Set<CharSequence> unparsableTypes = Collections.synchronizedSet(new HashSet<>());
    private CrawlFilters crawlFilters;
    private ParserFactory parserFactory;
    private Signature signature;
    private LinkFilter linkFilter;
    private int maxParsedLinks;
    /**
     * Parser timeout set to 30 sec by default. Set -1 (or any negative int) to deactivate
     **/
    private Duration maxParseTime;
    private ExecutorService executorService;

    /**
     * @param conf The configuration
     */
    public PageParser(ImmutableConfig conf) {
        this(new CrawlFilters(conf), new ParserFactory(conf), new TextMD5Signature(), conf);
    }

    public PageParser(ParserFactory parserFactory, ImmutableConfig conf) {
        this(new CrawlFilters(conf), parserFactory, new TextMD5Signature(), conf);
    }

    public PageParser(CrawlFilters crawlFilters, ParserFactory parserFactory, Signature signature, ImmutableConfig conf) {
        this.conf = conf;
        this.crawlFilters = crawlFilters;
        this.parserFactory = parserFactory;
        this.signature = signature;

        reload(conf);
    }

    @Override
    public ImmutableConfig getConf() {
        return conf;
    }

    @Override
    public Params getParams() {
        return Params.of(
                "maxParseTime", maxParseTime,
                "maxParsedLinks", maxParsedLinks
        );
    }

    @Override
    public void reload(ImmutableConfig conf) {
        maxParseTime = conf.getDuration(PARSE_TIMEOUT, DEFAULT_MAX_PARSE_TIME);
        maxParsedLinks = conf.getUint(PARSE_MAX_LINKS_PER_PAGE, 200);
        linkFilter = new LinkFilter(crawlFilters, conf);

        if (maxParseTime.getSeconds() > 0) {
            ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("parse-%d").setDaemon(true).build();
            this.executorService = Executors.newCachedThreadPool(threadFactory);
        }

        getParams().merge(linkFilter.getParams()).withLogger(LOG).info(true);
    }

    public MetricsCounters getPulsarCounters() {
        return metricsCounters;
    }

    public void setPulsarCounters(MetricsCounters metricsCounters) {
        this.metricsCounters = metricsCounters;
    }

    public ParserFactory getParserFactory() {
        return parserFactory;
    }

    public void setParserFactory(ParserFactory parserFactory) {
        this.parserFactory = parserFactory;
    }

    public Signature getSignature() {
        return signature;
    }

    public void setSignature(Signature signature) {
        this.signature = signature;
    }

    public CrawlFilters getCrawlFilters() {
        return crawlFilters;
    }

    public void setCrawlFilters(CrawlFilters crawlFilters) {
        this.crawlFilters = crawlFilters;
    }

    public LinkFilter getLinkFilter() {
        return linkFilter;
    }

    public void setLinkFilter(LinkFilter linkFilter) {
        this.linkFilter = linkFilter;
    }

    public Set<CharSequence> getUnparsableTypes() {
        return unparsableTypes;
    }

    /**
     * Parses given web page and stores parsed content within page. Puts a
     * meta-redirect to outlinks.
     * <p>
     *
     * @param page The web page
     * @return ParseResult
     * A non-null ParseResult contains the main result and status of this parse
     */
    @Nonnull
    public ParseResult parse(WebPage page) {
        try {
            ParseResult parseResult = doParse(page);

            if (parseResult.isParsed()) {
                page.setParseStatus(parseResult);

                if (parseResult.isRedirect()) {
                    processRedirect(page, parseResult);
                } else if (parseResult.isSuccess()) {
                    processSuccess(page, parseResult);
                }

                updateCounters(parseResult);

                if (parseResult.isSuccess()) {
                    page.getMarks().putIfNonNull(Mark.PARSE, page.getMarks().get(Mark.FETCH));
                }
            }

            return parseResult;
        } catch (Throwable e) {
            LOG.error(StringUtil.stringifyException(e));
        }

        return new ParseResult();
    }

    public List<HypeLink> filterLinks(WebPage page, List<HypeLink> unfilteredLinks) {
        final int[] sequence = {0};

        return unfilteredLinks.stream()
                .filter(linkFilter.asPredicate(page)) // filter out invalid urls
                .peek(l -> l.setOrder(++sequence[0])) // links added later, crawls earlier
                .sorted((l1, l2) -> l2.getAnchor().length() - l1.getAnchor().length()) // longer anchor comes first
                .distinct()
                .limit(maxParsedLinks)
                .collect(Collectors.toList());
    }

    private ParseResult doParse(WebPage page) {
        String url = page.getUrl();
        if (isTruncated(page)) {
            return ParseResult.failed(ParseStatus.FAILED_TRUNCATED, url);
        }

        ParseResult parseResult;
        try {
            parseResult = applyParsers(page);
        } catch (ParserNotFound e) {
            String contentType = page.getContentType();
            if (!unparsableTypes.contains(contentType)) {
                unparsableTypes.add(contentType);
                LOG.warn("No suitable parser for <" + contentType + ">." + e.getMessage());
            }
            return ParseResult.failed(ParseStatus.FAILED_NO_PARSER, contentType);
        } catch (Throwable e) {
            return ParseResult.failed(e);
        }

        return parseResult;
    }

    /**
     * @throws ParseException If there is an error parsing.
     */
    @Nonnull
    private ParseResult applyParsers(WebPage page) throws ParseException {
        ParseResult parseResult = new ParseResult();

        List<Parser> parsers = parserFactory.getParsers(page.getContentType(), page.getUrl());
        for (Parser parser : parsers) {
            if (executorService != null) {
                assert (maxParseTime.getSeconds() > 0);
                parseResult = runParser(parser, page);
            } else {
                parseResult = parser.parse(page);
            }

            // Found a suitable parser and successfully parsed
            if (parseResult.isSuccess()) {
                break;
            }
        }

        return parseResult;
    }

    private ParseResult runParser(Parser p, WebPage page) {
        ParseResult parseResult = new ParseResult();
        Future<ParseResult> task = executorService.submit(() -> p.parse(page));
        try {
            parseResult = task.get(maxParseTime.getSeconds(), TimeUnit.SECONDS);
        } catch (Exception e) {
            LOG.warn("Failed to parsing " + page.getUrl(), e);
            task.cancel(true);
        }

        return parseResult;
    }

    private void processSuccess(WebPage page, ParseResult parseResult) {
        ByteBuffer prevSig = page.getSignature();
        if (prevSig != null) {
            page.setPrevSignature(prevSig);
        }
        page.setSignature(signature.calculate(page));

        // Collect links
        // TODO : check no-follow html tag directive
        boolean follow = !page.getMetadata().contains(NO_FOLLOW)
                || page.isSeed()
                || page.hasMark(Mark.INJECT)
                || page.getMetadata().contains(Name.FORCE_FOLLOW)
                || page.getVariables().contains(Name.FORCE_FOLLOW.name());
        if (follow) {
            processLinks(page, parseResult.getHypeLinks());
        }
    }

    private void processRedirect(WebPage page, ParseStatus parseStatus) {
        String refreshHref = parseStatus.getArgOrDefault(ParseStatus.REFRESH_HREF, "");
        String newUrl = crawlFilters.normalizeToEmpty(refreshHref, UrlNormalizers.SCOPE_FETCHER);
        if (newUrl.isEmpty()) {
            return;
        }

        page.addLiveLink(new HypeLink(newUrl));
        page.getMetadata().set(REDIRECT_DISCOVERED, YES_STRING);

        if (newUrl.equals(page.getUrl())) {
            int refreshTime = Integer.parseInt(parseStatus.getArgOrDefault(ParseStatus.REFRESH_TIME, "0"));
            String reprUrl = URLUtil.chooseRepr(page.getUrl(), newUrl, refreshTime < PERM_REFRESH_TIME);
            page.setReprUrl(reprUrl);
        }
    }

    private void processLinks(WebPage page, ArrayList<HypeLink> unfilteredLinks) {
        List<HypeLink> hypeLinks = filterLinks(page, unfilteredLinks);

        page.setLiveLinks(hypeLinks);
        page.addHyperLinks(hypeLinks);
    }

    // 0 : "notparsed", 1 : "success", 2 : "failed"
    private void updateCounters(ParseStatus parseStatus) {
        if (parseStatus == null) {
            return;
        }

        Counter counter = null;
        switch (parseStatus.getMajorCode()) {
            case FAILED:
                counter = Counter.parseFailed;
                break;
            default:
                break;
        }

        if (counter != null && metricsCounters != null) {
            metricsCounters.increase(counter);
        }
    }

    /**
     * Checks if the page's content is truncated.
     *
     * @param page The web page
     * @return If the page is truncated <code>true</code>. When it is not, or when
     * it could be determined, <code>false</code>.
     */
    public boolean isTruncated(WebPage page) {
        String url = page.getUrl();
        int inHeaderSize = page.getHeaders().getContentLength();
        if (inHeaderSize < 0) {
            LOG.debug("HttpHeaders.CONTENT_LENGTH is not available | " + url);
            return false;
        }

        ByteBuffer content = page.getContent();
        if (content == null) {
            LOG.debug("Page content is null, url: " + url);
            return false;
        }
        int actualSize = content.limit();

        return inHeaderSize > actualSize;
    }

    public enum Counter {notFetched, alreadyParsed, truncated, notParsed, parseSuccess, parseFailed}
}
