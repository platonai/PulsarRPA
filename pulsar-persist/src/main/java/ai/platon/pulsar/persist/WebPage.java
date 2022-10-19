/*******************************************************************************
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
 ******************************************************************************/
package ai.platon.pulsar.persist;

import ai.platon.pulsar.common.DateTimes;
import ai.platon.pulsar.common.HtmlIntegrity;
import ai.platon.pulsar.common.Strings;
import ai.platon.pulsar.common.browser.BrowserType;
import ai.platon.pulsar.common.config.VolatileConfig;
import ai.platon.pulsar.common.urls.UrlUtils;
import ai.platon.pulsar.persist.experimental.WebAsset;
import ai.platon.pulsar.persist.gora.generated.*;
import ai.platon.pulsar.persist.metadata.*;
import ai.platon.pulsar.persist.model.*;
import org.apache.avro.util.Utf8;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.gora.util.ByteUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.InputSource;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static ai.platon.pulsar.common.config.AppConstants.*;

/**
 * The core web page structure
 */
public abstract class WebPage implements Comparable<WebPage>, WebAsset {

    private static final AtomicInteger SEQUENCER = new AtomicInteger();

    public static final WebPage NIL = newInternalPage(NIL_PAGE_URL, 0, "nil", "nil");

    @NotNull
    public static MutableWebPage newWebPage(@NotNull String url, @NotNull VolatileConfig conf) {
        return newWebPage(url, conf, null);
    }

    /**
     * @deprecated Use WebPageEx.newTestWebPage instead
     * */
    @Deprecated
    @NotNull
    public static MutableWebPage newTestWebPage(@NotNull String url) {
        return newWebPage(url, new VolatileConfig(), null);
    }

    @NotNull
    public static MutableWebPage newWebPage(@NotNull String url, @NotNull VolatileConfig conf, @Nullable String href) {
        return newWebPageInternal(url, conf, href);
    }

    @NotNull
    private static MutableWebPage newWebPageInternal(@NotNull String url, @NotNull VolatileConfig conf, @Nullable String href) {
        MutableWebPage page = new MutableWebPage(url, GWebPage.newBuilder().build(), false, conf);

        page.setLocation(url);
        page.setConf(conf);
        page.setHref(href);
        page.setCrawlStatus(CrawlStatus.STATUS_UNFETCHED);
        page.setCreateTime(Instant.now());
        page.setModifiedTime(Instant.now());
        page.setScore(0);
        page.setFetchCount(0);

        return page;
    }

    @NotNull
    public static MutableWebPage newInternalPage(@NotNull String url) {
        return newInternalPage(url, "internal", "internal");
    }

    @NotNull
    public static MutableWebPage newInternalPage(@NotNull String url, @NotNull String title) {
        return newInternalPage(url, title, "internal");
    }

    @NotNull
    public static MutableWebPage newInternalPage(@NotNull String url, @NotNull String title, @NotNull String content) {
        return newInternalPage(url, -1, title, content);
    }

    @NotNull
    public static MutableWebPage newInternalPage(@NotNull String url, int id, @NotNull String title, @NotNull String content) {
        VolatileConfig unsafe = VolatileConfig.Companion.getUNSAFE();
        MutableWebPage page = MutableWebPage.newWebPage(url, unsafe);
        if (id >= 0) {
            page.id = id;
        }

        page.setLocation(url);
        page.setModifiedTime(Instant.EPOCH);
        page.setPrevFetchTime(Instant.EPOCH);
        page.setFetchTime(Instant.EPOCH.plus(ChronoUnit.CENTURIES.getDuration()));
        page.setFetchInterval(ChronoUnit.CENTURIES.getDuration());
        page.setFetchPriority(FETCH_PRIORITY_MIN);
        page.setCrawlStatus(CrawlStatus.STATUS_UNFETCHED);

        page.setDistance(DISTANCE_INFINITE); // or -1?
        page.getMarks().put(Mark.INTERNAL, YES_STRING);
        page.getMarks().put(Mark.INACTIVE, YES_STRING);

        page.setPageTitle(title);
        page.setContent(content);

        return page;
    }

    /**
     * Initialize a WebPage with the underlying GWebPage instance.
     */
    @NotNull
    public static MutableWebPage box(
            @NotNull String url, @NotNull String reversedUrl, @NotNull GWebPage page, @NotNull VolatileConfig conf) {
        return new MutableWebPage(url, reversedUrl, page, conf);
    }

    /**
     * Initialize a WebPage with the underlying GWebPage instance.
     */
    @NotNull
    public static MutableWebPage box(@NotNull String url, @NotNull GWebPage page, @NotNull VolatileConfig conf) {
        return box(url, page, false, conf);
    }

    /**
     * Initialize a WebPage with the underlying GWebPage instance.
     */
    @NotNull
    public static MutableWebPage box(
            @NotNull String url, @NotNull GWebPage page, boolean urlReversed, @NotNull VolatileConfig conf
    ) {
        return new MutableWebPage(url, page, urlReversed, conf);
    }

    protected Integer id = SEQUENCER.incrementAndGet();
    /**
     * The url is the permanent internal address, and the location is the last working address
     */
    @NotNull
    protected String url = "";
    /**
     * The reversed url of the web page, it's also the key of the underlying storage of this object
     */
    @NotNull
    protected String reversedUrl = "";
    /**
     * Underlying persistent object
     */
    @NotNull
    protected GWebPage page;
    /**
     * Web page scope configuration
     */
    @NotNull
    protected VolatileConfig conf;
    /**
     * Web page scope variables
     */
    protected final Variables variables = new Variables();

    /**
     * If this page is fetched from Internet
     */
    protected boolean isCached = false;

    /**
     * If this page is loaded from database or is created and fetched from the web
     */
    protected boolean isLoaded = false;

    /**
     * If this page is fetched from Internet
     */
    protected boolean isFetched = false;

    /**
     * If this page is canceled
     */
    protected boolean isCanceled = false;

    /**
     * If this page is fetched and updated
     */
    protected volatile boolean isContentUpdated = false;

    /**
     * The cached content
     */
    protected volatile ByteBuffer tmpContent = null;

    /**
     * The delay time to retry if a retry is needed
     */
    protected Duration retryDelay = Duration.ZERO;

    @NotNull
    public static Utf8 wrapKey(@NotNull Mark mark) {
        return u8(mark.value());
    }

    @Nullable
    public static Utf8 u8(@Nullable String value) {
        if (value == null) {
            // TODO: return new Utf8.EMPTY?
            return null;
        }
        return new Utf8(value);
    }

    @NotNull
    public String getUrl() {
        return url;
    }

    @NotNull
    public String getKey() {
        return getReversedUrl();
    }

    @NotNull
    public String getReversedUrl() {
        return reversedUrl;
    }

    public int getId() {
        return id;
    }

    /**
     * Get The hypertext reference of this page.
     * It defines the address of the document, which this time is linked from
     * <p>
     * TODO: use a separate field for href
     *
     * @return The hypertext reference
     */
    @Nullable
    public String getHref() {
        return getMetadata().get(Name.HREF);
    }

    public boolean isNil() {
        return this == NIL;
    }

    public boolean isNotNil() {
        return !isNil();
    }

    public boolean isInternal() {
        return hasMark(Mark.INTERNAL);
    }

    public boolean isNotInternal() {
        return !isInternal();
    }

    @NotNull
    public GWebPage unbox() {
        return page;
    }

    public void unsafeSetGPage(@NotNull GWebPage page) {
        this.page = page;
    }

    public void unsafeCloneGPage(WebPage page) {
        unsafeSetGPage(GWebPage.newBuilder(page.unbox()).build());
    }

    /**
     * *****************************************************************************
     * Common fields
     * ******************************************************************************
     */

    @NotNull
    public Variables getVariables() {
        return variables;
    }

    /**
     * Check if the page scope temporary variable with {@code name} exists
     *
     * @param name The variable name to check
     * @return true if the variable exist
     */
    public boolean hasVar(@NotNull String name) {
        return variables.contains(name);
    }

    /**
     * Returns the local variable value to which the specified name is mapped,
     * or {@code null} if the local variable map contains no mapping for the name.
     *
     * @param name the name whose associated value is to be returned
     * @return the value to which the specified name is mapped, or
     *         {@code null} if the local variable map contains no mapping for the key
     */
    public Object getVar(@NotNull String name) {
        return variables.get(name);
    }

    public boolean isCached() {
        return isCached;
    }

    public boolean isLoaded() {
        return isLoaded;
    }

    public boolean isFetched() {
        return isFetched;
    }

    /**
     * Check if the page is canceled.
     *
     * If a page is canceled, it should not be updated.
     * */
    public boolean isCanceled() {
        return isCanceled;
    }

    public boolean isContentUpdated() {
        return isContentUpdated;
    }

    @NotNull
    public VolatileConfig getConf() {
        return conf;
    }

    public Metadata getMetadata() {
        return Metadata.box(page.getMetadata());
    }

    /**
     * CrawlMarks are used for nutch style crawling.
     * */
    public CrawlMarks getMarks() {
        return CrawlMarks.box(page.getMarkers());
    }

    /**
     * Check if a mark is marked.
     *
     * CrawlMarks are used for nutch style crawling.
     * */
    public boolean hasMark(Mark mark) {
        return page.getMarkers().get(wrapKey(mark)) != null;
    }

    /**
     * The load arguments is variant task by task, so the local version is the first choice,
     * while the persisted version is used for historical check only
     *
     * Underlying gora field should not use name 'args' which is already used,
     * see GProtocolStatus.args and GParseStatus.args
     */
    @NotNull
    public String getArgs() {
        // Underlying gora field should not use name 'args' which is already used.
        CharSequence args = page.getParams();
        return args != null ? args.toString() : "";
    }

    @NotNull
    public Duration getRetryDelay() {
        return retryDelay;
    }
    
    public int getMaxRetries() {
        return getMetadata().getInt(Name.FETCH_MAX_RETRY, 3);
    }

    @NotNull
    public String getConfiguredUrl() {
        return UrlUtils.mergeUrlArgs(url, getArgs());
    }

    public int getFetchedLinkCount() {
        return getMetadata().getInt(Name.FETCHED_LINK_COUNT, 0);
    }

    @NotNull
    public ZoneId getZoneId() {
        return page.getZoneId() == null ? DateTimes.INSTANCE.getZoneId() : ZoneId.of(page.getZoneId().toString());
    }

    public String getBatchId() {
        return page.getBatchId() == null ? "" : page.getBatchId().toString();
    }

    public void markSeed() {
        getMetadata().set(Name.IS_SEED, YES_STRING);
    }

    public void unmarkSeed() {
        getMetadata().remove(Name.IS_SEED);
    }

    public boolean isSeed() {
        return getMetadata().contains(Name.IS_SEED);
    }

    public int getDistance() {
        int distance = page.getDistance();
        return distance < 0 ? DISTANCE_INFINITE : distance;
    }

    @NotNull
    public FetchMode getFetchMode() {
        return FetchMode.fromString(getMetadata().get(Name.FETCH_MODE));
    }

    @NotNull
    public BrowserType getLastBrowser() {
        String browser = page.getBrowser() != null ? page.getBrowser().toString() : "";
        return BrowserType.fromString(browser);
    }

    public boolean isResource() {
        return page.getResource() != null;
    }

    @NotNull
    public HtmlIntegrity getHtmlIntegrity() {
        String integrity = page.getHtmlIntegrity() != null ? page.getHtmlIntegrity().toString() : "";
        return HtmlIntegrity.Companion.fromString(integrity);
    }

    public int getFetchPriority() {
        return page.getFetchPriority() > 0 ? page.getFetchPriority() : FETCH_PRIORITY_DEFAULT;
    }

    public int sniffFetchPriority() {
        int priority = getFetchPriority();

        int depth = getDistance();
        if (depth < FETCH_PRIORITY_DEPTH_BASE) {
            priority = Math.max(priority, FETCH_PRIORITY_DEPTH_BASE - depth);
        }

        return priority;
    }

    @NotNull
    public Instant getCreateTime() {
        return Instant.ofEpochMilli(page.getCreateTime());
    }

    @NotNull
    public Instant getGenerateTime() {
        String generateTime = getMetadata().get(Name.GENERATE_TIME);
        if (generateTime == null) {
            return Instant.EPOCH;
        } else {
            return Instant.parse(generateTime);
        }
    }

    public int getFetchCount() {
        return page.getFetchCount();
    }

    @NotNull
    public CrawlStatus getCrawlStatus() {
        return new CrawlStatus(page.getCrawlStatus().byteValue());
    }

    /**
     * The baseUrl is as the same as Location
     * <p>
     * A baseUrl has the same semantic with Jsoup.parse:
     *
     * @return a {@link String} object.
     * @link {https://jsoup.org/apidocs/org/jsoup/Jsoup.html#parse-java.io.File-java.lang.String-java.lang.String-}
     * @see WebPage#getLocation
     */
    public String getBaseUrl() {
        return page.getBaseUrl() == null ? "" : page.getBaseUrl().toString();
    }

    /**
     * WebPage.url is the permanent internal address, it might not still available to access the target.
     * And WebPage.location or WebPage.baseUrl is the last working address, it might redirect to url,
     * or it might have additional random parameters.
     * WebPage.location may be different from url, it's generally normalized.
     */
    public String getLocation() {
        return getBaseUrl();
    }

    /**
     * The latest fetch time
     *
     * @return The latest fetch time
     */
    @NotNull
    public Instant getFetchTime() {
        return Instant.ofEpochMilli(page.getFetchTime());
    }

    /**
     * The previous fetch time, updated at the fetch stage
     *
     * @return The previous fetch time.
     */
    @NotNull
    public Instant getPrevFetchTime() {
        return Instant.ofEpochMilli(page.getPrevFetchTime());
    }

    /**
     * The previous crawl time, used for fat link crawl, which means both the page itself and out pages are fetched
     * */
    @NotNull
    public Instant getPrevCrawlTime1() {
        return Instant.ofEpochMilli(page.getPrevCrawlTime1());
    }

    /**
     * Get fetch interval
     * */
    @NotNull
    public Duration getFetchInterval() {
        long seconds = page.getFetchInterval();
        if (seconds < 0) {
            seconds = ChronoUnit.CENTURIES.getDuration().getSeconds();
        }
        return Duration.ofSeconds(seconds);
    }

    /**
     * Get protocol status
     * */
    @NotNull
    public ProtocolStatus getProtocolStatus() {
        GProtocolStatus protocolStatus = page.getProtocolStatus();
        if (protocolStatus == null) {
            protocolStatus = GProtocolStatus.newBuilder().build();
        }
        return ProtocolStatus.box(protocolStatus);
    }

    /**
     * Header information returned from the web server used to server the content which is subsequently fetched from.
     * This includes keys such as
     * TRANSFER_ENCODING,
     * CONTENT_ENCODING,
     * CONTENT_LANGUAGE,
     * CONTENT_LENGTH,
     * CONTENT_LOCATION,
     * CONTENT_DISPOSITION,
     * CONTENT_MD5,
     * CONTENT_TYPE,
     * LAST_MODIFIED
     * and LOCATION.
     */
    @NotNull
    public ProtocolHeaders getHeaders() {
        return ProtocolHeaders.box(page.getHeaders());
    }

    @NotNull
    public String getReprUrl() {
        return page.getReprUrl() == null ? "" : page.getReprUrl().toString();
    }

    public int getFetchRetries() {
        return page.getFetchRetries();
    }

    @NotNull
    public Instant getModifiedTime() {
        return Instant.ofEpochMilli(page.getModifiedTime());
    }

    @NotNull
    public Instant getPrevModifiedTime() {
        return Instant.ofEpochMilli(page.getPrevModifiedTime());
    }

    @NotNull
    public String getFetchTimeHistory(@NotNull String defaultValue) {
        String s = getMetadata().get(Name.FETCH_TIME_HISTORY);
        return s == null ? defaultValue : s;
    }

    @NotNull
    public PageCategory getPageCategory() {
        try {
            CharSequence pageCategory = page.getPageCategory();
            if (pageCategory != null) {
                return PageCategory.parse(pageCategory.toString());
            }
        } catch (Throwable ignored) {
        }

        return PageCategory.UNKNOWN;
    }

    @NotNull
    public OpenPageCategory getOpenPageCategory() {
        try {
            CharSequence pageCategory = page.getPageCategory();
            if (pageCategory != null) {
                return OpenPageCategory.Companion.parse(pageCategory.toString());
            }
        } catch (Throwable ignored) {
        }

        return new OpenPageCategory("", "");
    }

    /**
     * Get the encoding of the content.
     * Content encoding is detected just before it's parsed.
     */
    @Nullable
    public String getEncoding() {
        return page.getEncoding() == null ? null : page.getEncoding().toString();
    }

    /**
     * The clues are used to determine the encoding of the page content
     * */
    @NotNull
    public String getEncodingClues() {
        return getMetadata().getOrDefault(Name.ENCODING_CLUES, "");
    }

    /**
     * The entire raw document content e.g. raw XHTML
     *
     * @return The raw document content in {@link ByteBuffer}.
     */
    @Nullable
    public ByteBuffer getContent() {
        if (tmpContent != null) {
            return tmpContent;
        }
        return page.getContent();
    }

    /**
     * Get the cached content
     */
    @Nullable
    public ByteBuffer getTmpContent() {
        return tmpContent;
    }

    /**
     * Get the persisted page content
     */
    @Nullable
    public ByteBuffer getPersistContent() {
        return page.getContent();
    }

    /**
     * Get content as bytes, the underling buffer is duplicated
     *
     * @return a duplication of the underling buffer.
     */
    @NotNull
    public byte[] getContentAsBytes() {
        ByteBuffer content = getContent();
        if (content == null) {
            return ByteUtils.toBytes('\0');
        }
        return ByteUtils.toBytes(content);
    }

    /**
     * Get the page content as a string, if the underlying page content is null, return an empty string
     */
    @NotNull
    public String getContentAsString() {
        ByteBuffer buffer = getContent();
        if (buffer == null || buffer.remaining() == 0) {
            return "";
        }

        return new String(buffer.array(), buffer.arrayOffset(), buffer.limit());
    }

    /**
     * Get the page content as input stream
     */
    @NotNull
    public ByteArrayInputStream getContentAsInputStream() {
        ByteBuffer contentInOctets = getContent();
        if (contentInOctets == null) {
            return new ByteArrayInputStream(ByteUtils.toBytes('\0'));
        }

        return new ByteArrayInputStream(getContent().array(),
                contentInOctets.arrayOffset() + contentInOctets.position(),
                contentInOctets.remaining());
    }

    /**
     * Get the page content as sax input source
     */
    @NotNull
    public InputSource getContentAsSaxInputSource() {
        InputSource inputSource = new InputSource(getContentAsInputStream());
        String encoding = getEncoding();
        if (encoding != null) {
            inputSource.setEncoding(encoding);
        }
        return inputSource;
    }

    /**
     * Get the length of content in bytes.
     *
     * TODO: check consistency with HttpHeaders.CONTENT_LENGTH
     *
     * @return The length of the content in bytes.
     */
    public long getContentLength() {
        return page.getContentLength() != null ? page.getContentLength() : 0;
    }

    /**
     * Compute the length of content in bytes.
     */
    protected void computeContentLength(long bytes) {
        long lastBytes = getContentLength();
        page.setLastContentLength(lastBytes);
        page.setContentLength(bytes);
        computeAveContentLength(bytes);
    }

    protected void computeAveContentLength(long bytes) {
        int count = getFetchCount();
        long lastAveBytes = page.getAveContentLength();

        long aveBytes;
        if (count > 0 && lastAveBytes == 0) {
            // old version, average bytes is not calculated
            aveBytes = bytes;
        } else {
            aveBytes = (lastAveBytes * count + bytes) / (count + 1);
        }

        page.setAveContentLength(aveBytes);
    }

    public long getPersistedContentLength() {
        return page.getPersistedContentLength() != null ? page.getPersistedContentLength() : 0;
    }

    public long getLastContentLength() {
        return page.getLastContentLength() != null ? page.getLastContentLength() : 0;
    }

    public long getAveContentLength() {
        return page.getAveContentLength() != null ? page.getAveContentLength() : 0;
    }

    @NotNull
    public String getContentType() {
        return page.getContentType() == null ? "" : page.getContentType().toString();
    }

    @Nullable
    public ByteBuffer getPrevSignature() {
        return page.getPrevSignature();
    }

    @NotNull
    public String getPrevSignatureAsString() {
        ByteBuffer sig = getPrevSignature();
        if (sig == null) {
            sig = ByteBuffer.wrap("".getBytes());
        }
        return Strings.toHexString(sig);
    }

    /**
     * The last proxy used to fetch the page
     */
    public String getProxy() {
        return page.getProxy() == null ? null : page.getProxy().toString();
    }

    @Nullable
    public ActiveDOMStatus getActiveDOMStatus() {
        GActiveDOMStatus s = page.getActiveDOMStatus();
        if (s == null) return null;

        return new ActiveDOMStatus(
                s.getN(),
                s.getScroll(),
                s.getSt().toString(),
                s.getR().toString(),
                s.getIdl().toString(),
                s.getEc().toString()
        );
    }

    @NotNull
    public Map<String, ActiveDOMStat> getActiveDOMStatTrace() {
        Map<CharSequence, GActiveDOMStat> s = page.getActiveDOMStatTrace();
        return s.entrySet().stream().collect(Collectors.toMap(
                e -> e.getKey().toString(),
                e -> Converters.INSTANCE.convert(e.getValue())
        ));
    }

    /**
     * An implementation of a WebPage's signature from which it can be identified and referenced at any point in time.
     * This is essentially the WebPage's fingerprint representing its state for any point in time.
     */
    @Nullable
    public ByteBuffer getSignature() {
        return page.getSignature();
    }

    @NotNull
    public String getSignatureAsString() {
        ByteBuffer sig = getSignature();
        if (sig == null) {
            sig = ByteBuffer.wrap("".getBytes());
        }
        return Strings.toHexString(sig);
    }

    @NotNull
    public String getPageTitle() {
        return page.getPageTitle() == null ? "" : page.getPageTitle().toString();
    }

    @NotNull
    public String getContentTitle() {
        return page.getContentTitle() == null ? "" : page.getContentTitle().toString();
    }

    @NotNull
    public String getPageText() {
        return page.getPageText() == null ? "" : page.getPageText().toString();
    }

    @NotNull
    public String getContentText() {
        return page.getContentText() == null ? "" : page.getContentText().toString();
    }

    public int getContentTextLen() {
        return page.getContentTextLen();
    }

    @NotNull
    public ParseStatus getParseStatus() {
        GParseStatus parseStatus = page.getParseStatus();
        return ParseStatus.box(parseStatus == null ? GParseStatus.newBuilder().build() : parseStatus);
    }

    public Map<CharSequence, GHypeLink> getLiveLinks() {
        return page.getLiveLinks();
    }

    public Collection<String> getSimpleLiveLinks() {
        return CollectionUtils.collect(page.getLiveLinks().keySet(), CharSequence::toString);
    }
    
    public void addLiveLink(HyperlinkPersistable hyperLink) {
        page.getLiveLinks().put(hyperLink.getUrl(), hyperLink.unbox());
    }

    public Map<CharSequence, CharSequence> getVividLinks() {
        return page.getVividLinks();
    }

    public Collection<String> getSimpleVividLinks() {
        return CollectionUtils.collect(page.getVividLinks().keySet(), CharSequence::toString);
    }

    public List<CharSequence> getDeadLinks() {
        return page.getDeadLinks();
    }

    public List<CharSequence> getLinks() {
        return page.getLinks();
    }

    public int getImpreciseLinkCount() {
        return getMetadata().getInt(Name.TOTAL_OUT_LINKS, 0);
    }

    public Map<CharSequence, CharSequence> getInlinks() {
        return page.getInlinks();
    }

    @NotNull
    public CharSequence getAnchor() {
        return page.getAnchor() != null ? page.getAnchor() : "";
    }

    public String[] getInlinkAnchors() {
        return StringUtils.split(getMetadata().getOrDefault(Name.ANCHORS, ""), "\n");
    }

    public int getAnchorOrder() {
        return page.getAnchorOrder() < 0 ? MAX_LIVE_LINK_PER_PAGE : page.getAnchorOrder();
    }

    public Instant getContentPublishTime() {
        return Instant.ofEpochMilli(page.getContentPublishTime());
    }

    public boolean isValidContentModifyTime(Instant publishTime) {
        return publishTime.isAfter(MIN_ARTICLE_PUBLISH_TIME);
    }

    public Instant getPrevContentPublishTime() {
        return Instant.ofEpochMilli(page.getPrevContentPublishTime());
    }

    public Instant getRefContentPublishTime() {
        return Instant.ofEpochMilli(page.getRefContentPublishTime());
    }

    public Instant getContentModifiedTime() {
        return Instant.ofEpochMilli(page.getContentModifiedTime());
    }

    public Instant getPrevContentModifiedTime() {
        return Instant.ofEpochMilli(page.getPrevContentModifiedTime());
    }

    public Instant getPrevRefContentPublishTime() {
        return Instant.ofEpochMilli(page.getPrevRefContentPublishTime());
    }

    @Nullable
    public String getReferrer() {
        return page.getReferrer() == null ? null : page.getReferrer().toString();
    }

    /**
     * *****************************************************************************
     * Page Model
     * ******************************************************************************
     */
    @Nullable
    public Instant getPageModelUpdateTime() {
        return Instant.ofEpochMilli(page.getPageModelUpdateTime());
    }

    @Nullable
    public PageModel getPageModel() {
        return page.getPageModel() == null ? null : PageModel.box(page.getPageModel());
    }

    @NotNull
    public PageModel ensurePageModel() {
        if (page.getPageModel() == null) {
            page.setPageModel(GPageModel.newBuilder().build());
        }

        return Objects.requireNonNull(getPageModel());
    }

    /**
     * *****************************************************************************
     * Scoring
     * ******************************************************************************
     */
    public float getScore() {
        return page.getScore();
    }

    public float getContentScore() {
        return page.getContentScore() == null ? 0.0f : page.getContentScore();
    }

    @NotNull
    public String getSortScore() {
        return page.getSortScore() == null ? "" : page.getSortScore().toString();
    }

    public float getCash() {
        return getMetadata().getFloat(Name.CASH_KEY, 0.0f);
    }

    @NotNull
    public PageCounters getPageCounters() {
        return PageCounters.box(page.getPageCounters());
    }

    /**
     * *****************************************************************************
     * Index
     * ******************************************************************************
     */

    @Override
    public int hashCode() {
        return url.hashCode();
    }

    @Override
    public int compareTo(@NotNull WebPage o) {
        return url.compareTo(o.url);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        return other instanceof WebPage && ((WebPage) other).url.equals(url);
    }

    @Override
    public String toString() {
        return new WebPageFormatter(this).format();
    }
}
