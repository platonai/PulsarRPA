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
package fun.platonic.pulsar.persist;

import fun.platonic.pulsar.common.DateTimeUtil;
import fun.platonic.pulsar.common.StringUtil;
import fun.platonic.pulsar.common.UrlUtil;
import fun.platonic.pulsar.common.config.MutableConfig;
import fun.platonic.pulsar.persist.gora.generated.GHypeLink;
import fun.platonic.pulsar.persist.gora.generated.GParseStatus;
import fun.platonic.pulsar.persist.gora.generated.GProtocolStatus;
import fun.platonic.pulsar.persist.gora.generated.GWebPage;
import fun.platonic.pulsar.persist.metadata.*;
import org.apache.avro.util.Utf8;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.gora.util.ByteUtils;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static fun.platonic.pulsar.common.config.PulsarConstants.*;
import static java.time.temporal.ChronoUnit.DAYS;

/**
 * The core data structure across the whole program execution
 * <p>
 * Notice: Use a build-in java string or a Utf8 to serialize strings?
 *
 * @see org.apache.gora.hbase.util.HBaseByteInterface#fromBytes
 * <p>
 * In serializetion phrase, a byte array created by s.getBytes(UTF8_CHARSET) is serialized, and
 * in deserialization phrase, every string are wrapped to be a Utf8
 * <p>
 * So both build-in string and a Utf8 wrap is OK to serialize, and Utf8 is always returned
 */
public class WebPage {

    public static final Logger LOG = LoggerFactory.getLogger(WebPage.class);

    public static Instant impreciseNow = Instant.now();
    public static Instant impreciseTomorrow = impreciseNow.plus(1, ChronoUnit.DAYS);
    public static Instant imprecise2DaysAhead = impreciseNow.plus(2, ChronoUnit.DAYS);
    public static LocalDateTime middleNight = LocalDateTime.now().truncatedTo(DAYS);
    public static Instant middleNightInstant = Instant.now().truncatedTo(DAYS);
    public static ZoneId defaultZoneId = ZoneId.systemDefault();

    public static WebPage NIL = newInternalPage(NIL_PAGE_URL, "nil", "nil");
    /**
     * The url of the web page
     */
    private String url = "";
    /**
     * The reversed url of the web page, it's also the key of the underlying storage of this object
     */
    private String reversedUrl = "";
    /**
     * Underlying persistent object
     */
    private GWebPage page;
    /**
     * Object scope configuration
     */
    private MutableConfig mutableConfig;
    /**
     * Object scope variables
     */
    private Variables variables = new Variables();

    private WebPage(String url, GWebPage page, boolean urlReversed) {
        Objects.requireNonNull(url);
        Objects.requireNonNull(page);
        this.url = urlReversed ? UrlUtil.unreverseUrl(url) : url;
        this.reversedUrl = urlReversed ? url : UrlUtil.reverseUrlOrEmpty(url);
        this.page = page;

        if (page.getBaseUrl() == null) {
            setBaseUrl(url);
        }
    }

    private WebPage(String url, String reversedUrl, GWebPage page) {
        this.url = Objects.requireNonNull(url);
        this.reversedUrl = Objects.requireNonNull(reversedUrl);
        this.page = Objects.requireNonNull(page);

        if (page.getBaseUrl() == null) {
            setBaseUrl(url);
        }
    }

    @Nonnull
    public static WebPage newWebPage(String url) {
        Objects.requireNonNull(url);
        return newWebPageInternal(url, null);
    }

    @Nonnull
    public static WebPage newWebPage(String url, MutableConfig mutableConfig) {
        Objects.requireNonNull(url);
        Objects.requireNonNull(mutableConfig);
        return newWebPageInternal(url, mutableConfig);
    }

    private static WebPage newWebPageInternal(String url, @Nullable MutableConfig mutableConfig) {
        Objects.requireNonNull(url);

        WebPage page = new WebPage(url, GWebPage.newBuilder().build(), false);

        page.setBaseUrl(url);
        page.setMutableConfig(mutableConfig);
        page.setCrawlStatus(CrawlStatus.STATUS_UNFETCHED);
        page.setCreateTime(impreciseNow);
        page.setScore(0);
        page.setFetchCount(0);

        return page;
    }

    @Nonnull
    public static WebPage newInternalPage(@Nonnull String url) {
        return newInternalPage(url, "internal", "internal");
    }

    @Nonnull
    public static WebPage newInternalPage(@Nonnull String url, @Nonnull String title) {
        return newInternalPage(url, title, "internal");
    }

    @Nonnull
    public static WebPage newInternalPage(@Nonnull String url, @Nonnull String title, @Nonnull String content) {
        Objects.requireNonNull(url);
        Objects.requireNonNull(title);
        Objects.requireNonNull(content);

        WebPage page = WebPage.newWebPage(url);

        page.setBaseUrl(url);
        page.setModifiedTime(impreciseNow);
        page.setFetchTime(Instant.parse("3000-01-01T00:00:00Z"));
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
    @Nonnull
    public static WebPage box(@Nonnull String url, @Nonnull String reversedUrl, @Nonnull GWebPage page) {
        Objects.requireNonNull(url);
        Objects.requireNonNull(reversedUrl);
        Objects.requireNonNull(page);

        return new WebPage(url, reversedUrl, page);
    }

    /**
     * Initialize a WebPage with the underlying GWebPage instance.
     */
    @Nonnull
    public static WebPage box(@Nonnull String url, @Nonnull GWebPage page) {
        Objects.requireNonNull(url);
        Objects.requireNonNull(page);

        return new WebPage(url, page, false);
    }

    /**
     * Initialize a WebPage with the underlying GWebPage instance.
     */
    @Nonnull
    public static WebPage box(@Nonnull String url, @Nonnull GWebPage page, boolean urlReversed) {
        Objects.requireNonNull(url);
        Objects.requireNonNull(page);

        return new WebPage(url, page, urlReversed);
    }

    /********************************************************************************
     * Other
     ********************************************************************************/

    public static Utf8 wrapKey(Mark mark) {
        return u8(mark.value());
    }

    /**
     * What's the difference between String and Utf8?
     */
    public static Utf8 u8(String value) {
        if (value == null) {
            // TODO: return new Utf8.EMPTY?
            return null;
        }
        return new Utf8(value);
    }

    public String getUrl() {
        return url != null ? url : "";
    }

    public String getKey() {
        return getReversedUrl();
    }

    public String getReversedUrl() {
        return reversedUrl != null ? reversedUrl : "";
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

    public GWebPage unbox() {
        return page;
    }

    /********************************************************************************
     * Common fields
     ********************************************************************************/

    public Variables getVariables() {
        return variables;
    }

    @Nullable
    public MutableConfig getMutableConfig() {
        return mutableConfig;
    }

    public void setMutableConfig(MutableConfig mutableConfig) {
        this.mutableConfig = mutableConfig;
    }

    @Nonnull
    public MutableConfig getMutableConfigOrElse(MutableConfig fallbackConfig) {
        Objects.requireNonNull(fallbackConfig);
        return mutableConfig != null ? mutableConfig : fallbackConfig;
    }

    public Metadata getMetadata() {
        return Metadata.box(page.getMetadata());
    }

    /********************************************************************************
     * Creation fields
     ********************************************************************************/

    public CrawlMarks getMarks() {
        return CrawlMarks.box(page.getMarkers());
    }

    public boolean hasMark(Mark mark) {
        return page.getMarkers().get(wrapKey(mark)) != null;
    }

    /**
     * All options are saved here, including crawl options, link options, entity options and so on
     */
    public CharSequence getOptions() {
        return page.getOptions() == null ? "" : page.getOptions().toString();
    }

    public void setOptions(CharSequence options) {
        page.setOptions(options);
    }

    public String getConfiguredUrl() {
        String configuredUrl = url;
        if (page.getOptions() != null) {
            configuredUrl += " " + page.getOptions().toString();
        }
        return configuredUrl;
    }

    public String getQuery() {
        return getMetadata().get(Name.QUERY);
    }

    public void setQuery(String query) {
        Objects.requireNonNull(query);
        getMetadata().set(Name.QUERY, query);
    }

    public ZoneId getZoneId() {
        return page.getZoneId() == null ? defaultZoneId : ZoneId.of(page.getZoneId().toString());
    }

    public void setZoneId(ZoneId zoneId) {
        page.setZoneId(zoneId.getId());
    }

    public String getBatchId() {
        return page.getBatchId() == null ? "" : page.getBatchId().toString();
    }

    public void setBatchId(String value) {
        page.setBatchId(value);
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

    public void setDistance(int newDistance) {
        page.setDistance(newDistance);
    }

    public void updateDistance(int newDistance) {
        int oldDistance = getDistance();
        if (newDistance < oldDistance) {
            setDistance(newDistance);
        }
    }

    /**
     * Fetch mode is used to determine the protocol before fetch
     */
    public FetchMode getFetchMode() {
        return FetchMode.fromString(getMetadata().get(Name.FETCH_MODE));
    }

    /**
     * Fetch mode is used to determine the protocol before fetch, so it shall be set before fetch
     */
    public void setFetchMode(FetchMode mode) {
        getMetadata().set(Name.FETCH_MODE, mode.name());
    }

    public BrowserType getLastBrowser() {
        return BrowserType.fromString(getMetadata().get(Name.BROWSER));
    }

    public void setLastBrowser(BrowserType browser) {
        getMetadata().set(Name.BROWSER, browser.name());
    }

    public int getFetchPriority() {
        return page.getFetchPriority() > 0 ? page.getFetchPriority() : FETCH_PRIORITY_DEFAULT;
    }

    public void setFetchPriority(int priority) {
        page.setFetchPriority(priority);
    }

    public int sniffFetchPriority() {
        int priority = getFetchPriority();

        int depth = getDistance();
        if (depth < FETCH_PRIORITY_DEPTH_BASE) {
            priority = Math.max(priority, FETCH_PRIORITY_DEPTH_BASE - depth);
        }

        return priority;
    }

    public Instant getCreateTime() {
        return Instant.ofEpochMilli(page.getCreateTime());
    }

    public void setCreateTime(Instant createTime) {
        page.setCreateTime(createTime.toEpochMilli());
    }

    public Instant getGenerateTime() {
        String generateTime = getMetadata().get(Name.GENERATE_TIME);
        if (generateTime == null) {
            return Instant.EPOCH;
        } else if (NumberUtils.isDigits(generateTime)) {
            // Old version of generate time, created by String.valueOf(epochMillis)
            return Instant.ofEpochMilli(NumberUtils.toLong(generateTime, 0));
        } else {
            return Instant.parse(generateTime);
        }
    }

    public void setGenerateTime(Instant generateTime) {
        getMetadata().set(Name.GENERATE_TIME, generateTime.toString());
    }

    /********************************************************************************
     * Fetch fields
     ********************************************************************************/

    public int getFetchCount() {
        return page.getFetchCount();
    }

    public void setFetchCount(int count) {
        page.setFetchCount(count);
    }

    public void increaseFetchCount() {
        int count = getFetchCount();
        setFetchCount(count + 1);
    }

    public CrawlStatus getCrawlStatus() {
        return new CrawlStatus(page.getCrawlStatus().byteValue());
    }

    public void setCrawlStatus(CrawlStatus crawlStatus) {
        page.setCrawlStatus(crawlStatus.getCode());
    }

    /**
     * Set crawl status
     *
     * @see CrawlStatus
     */
    public void setCrawlStatus(int value) {
        page.setCrawlStatus(value);
    }

    public String getBaseUrl() {
        return page.getBaseUrl() == null ? "" : page.getBaseUrl().toString();
    }

    /**
     * BaseUrl comes from Content#getBaseUrl which comes from ProtocolOutput
     * Maybe be different from url if the request redirected.
     */
    public void setBaseUrl(String value) {
        page.setBaseUrl(value);
    }

    public Instant getFetchTime() {
        return Instant.ofEpochMilli(page.getFetchTime());
    }

    public void setFetchTime(Instant time) {
        page.setFetchTime(time.toEpochMilli());
    }

    public Instant getPrevFetchTime() {
        return Instant.ofEpochMilli(page.getPrevFetchTime());
    }

    public void setPrevFetchTime(Instant time) {
        page.setPrevFetchTime(time.toEpochMilli());
    }

    /**
     * Get last fetch time
     * <p>
     * If fetchTime is before now, the result is the fetchTime
     * If fetchTime is after now, it means that schedule has modified it for the next fetch, the result is prevFetchTime
     */
    public Instant getLastFetchTime(Instant now) {
        Instant lastFetchTime = getFetchTime();
        // Minus 1 seconds to protect from inaccuracy
        if (lastFetchTime.isAfter(now.plusSeconds(1))) {
            // updated by schedule
            lastFetchTime = getPrevFetchTime();
        }
        return lastFetchTime;
    }

    public Duration getFetchInterval() {
        return Duration.ofSeconds(page.getFetchInterval());
    }

    public void setFetchInterval(Duration interval) {
        page.setFetchInterval((int) interval.getSeconds());
    }

    public long getFetchInterval(TimeUnit destUnit) {
        return destUnit.convert(page.getFetchInterval(), TimeUnit.SECONDS);
    }

    public void setFetchInterval(long interval) {
        page.setFetchInterval((int) interval);
    }

    public void setFetchInterval(float interval) {
        page.setFetchInterval(Math.round(interval));
    }

    public ProtocolStatus getProtocolStatus() {
        GProtocolStatus protocolStatus = page.getProtocolStatus();
        if (protocolStatus == null) {
            protocolStatus = GProtocolStatus.newBuilder().build();
        }
        return ProtocolStatus.box(protocolStatus);
    }

    public void setProtocolStatus(ProtocolStatus protocolStatus) {
        page.setProtocolStatus(protocolStatus.unbox());
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
    public ProtocolHeaders getHeaders() {
        return ProtocolHeaders.box(page.getHeaders());
    }

    @Nonnull
    public String getReprUrl() {
        return page.getReprUrl() == null ? "" : page.getReprUrl().toString();
    }

    public void setReprUrl(String value) {
        page.setReprUrl(value);
    }

    public int getFetchRetries() {
        return page.getFetchRetries();
    }

    public void setFetchRetries(int value) {
        page.setFetchRetries(value);
    }

    public Duration getLastTimeout() {
        String s = getMetadata().get(Name.RESPONSE_TIME);
        return s == null ? Duration.ZERO : Duration.parse(s);
    }

    public Instant getModifiedTime() {
        return Instant.ofEpochMilli(page.getModifiedTime());
    }

    public void setModifiedTime(Instant value) {
        page.setModifiedTime(value.toEpochMilli());
    }

    public Instant getPrevModifiedTime() {
        return Instant.ofEpochMilli(page.getPrevModifiedTime());
    }

    public void setPrevModifiedTime(Instant value) {
        page.setPrevModifiedTime(value.toEpochMilli());
    }

    public Instant sniffModifiedTime() {
        Instant modifiedTime = getModifiedTime();
        Instant headerModifiedTime = getHeaders().getLastModified();
        Instant contentModifiedTime = getContentModifiedTime();

        if (isValidContentModifyTime(headerModifiedTime) && headerModifiedTime.isAfter(modifiedTime)) {
            modifiedTime = headerModifiedTime;
        }

        if (isValidContentModifyTime(contentModifiedTime) && contentModifiedTime.isAfter(modifiedTime)) {
            modifiedTime = contentModifiedTime;
        }

        Instant contentPublishTime = getContentPublishTime();
        if (isValidContentModifyTime(contentPublishTime) && contentPublishTime.isAfter(modifiedTime)) {
            modifiedTime = contentPublishTime;
        }

        // A fix
        if (modifiedTime.isAfter(Instant.now().plus(1, ChronoUnit.DAYS))) {
            // LOG.warn("Invalid modified time " + DateTimeUtil.isoInstantFormat(modifiedTime) + ", url : " + page.url());
            modifiedTime = Instant.now();
        }

        return modifiedTime;
    }

    public String getFetchTimeHistory(String defaultValue) {
        String s = getMetadata().get(Name.FETCH_TIME_HISTORY);
        return s == null ? defaultValue : s;
    }

    /********************************************************************************
     * Parsing
     ********************************************************************************/

    public void putFetchTimeHistory(Instant fetchTime) {
        String fetchTimeHistory = getMetadata().get(Name.FETCH_TIME_HISTORY);
        fetchTimeHistory = DateTimeUtil.constructTimeHistory(fetchTimeHistory, fetchTime, 10);
        getMetadata().set(Name.FETCH_TIME_HISTORY, fetchTimeHistory);
    }

    public Instant getFirstCrawlTime(Instant defaultValue) {
        Instant firstCrawlTime = null;

        String fetchTimeHistory = getFetchTimeHistory("");
        if (!fetchTimeHistory.isEmpty()) {
            String[] times = fetchTimeHistory.split(",");
            Instant time = DateTimeUtil.parseInstant(times[0], Instant.EPOCH);
            if (time.isAfter(Instant.EPOCH)) {
                firstCrawlTime = time;
            }
        }

        return firstCrawlTime == null ? defaultValue : firstCrawlTime;
    }

    /**
     * namespace : metadata, seed, www
     * reserved
     */
    public String getNamespace() {
        return getMetadata().get("namespace");
    }

    /**
     * reserved
     * */
    public void setNamespace(String ns) {
        getMetadata().set("namespace", ns);
    }

    public PageCategory getPageCategory() {
        try {
            if (page.getPageCategory() != null) {
                return PageCategory.valueOf(page.getPageCategory().toString());
            }
        } catch (Throwable ignored) {
        }

        return PageCategory.UNKNOWN;
    }

    /**
     * category : index, detail, media, search
     */
    public void setPageCategory(PageCategory pageCategory) {
        page.setPageCategory(pageCategory.name());
    }

    @Nullable
    public String getEncoding() {
        return page.getEncoding() == null ? null : page.getEncoding().toString();
    }

    public void setEncoding(String encoding) {
        page.setEncoding(encoding);
        getMetadata().set(Name.CHAR_ENCODING_FOR_CONVERSION, encoding);
    }

    /**
     * Get content encoding
     * Content encoding is detected just before it's parsed
     */
    public String getEncodingOrDefault(String defaultEncoding) {
        return page.getEncoding() == null ? defaultEncoding : page.getEncoding().toString();
    }

    public String getEncodingClues() {
        return getMetadata().getOrDefault(Name.ENCODING_CLUES, "");
    }

    public void setEncodingClues(String clues) {
        getMetadata().set(Name.ENCODING_CLUES, clues);
    }

    public boolean hasContent() {
        return page.getContent() != null;
    }

    /**
     * The entire raw document content e.g. raw XHTML
     */
    @Nullable
    public ByteBuffer getContent() {
        return page.getContent();
    }

    public void setContent(String value) {
        setContent(value.getBytes());
    }

    @Nonnull
    public byte[] getContentAsBytes() {
        ByteBuffer content = getContent();
        if (content == null) {
            return ByteUtils.toBytes('\0');
        }
        return Bytes.getBytes(content);
    }

    /**
     * TODO: Encoding is always UTF-8?
     */
    @Nonnull
    public String getContentAsString() {
        return Bytes.toString(getContentAsBytes());
    }

    @Nonnull
    public ByteArrayInputStream getContentAsInputStream() {
        ByteBuffer contentInOctets = getContent();
        if (contentInOctets == null) {
            return new ByteArrayInputStream(ByteUtils.toBytes('\0'));
        }

//        if (LOG.isDebugEnabled()) {
//            LOG.debug(String.format("WebPage content as ByteBuffer: %d - %d",
//                    contentInOctets.arrayOffset(), contentInOctets.position()));
//        }

        return new ByteArrayInputStream(getContent().array(),
                contentInOctets.arrayOffset() + contentInOctets.position(),
                contentInOctets.remaining());
    }

    @Nonnull
    public InputSource getContentAsSaxInputSource() {
        InputSource inputSource = new InputSource(getContentAsInputStream());
        String encoding = getEncoding();
        if (encoding != null) {
            inputSource.setEncoding(encoding);
        }
        return inputSource;
    }

    public void setContent(ByteBuffer value) {
        page.setContent(value);
        setContentBytes(value.array().length);
    }

    public void setContent(@Nullable byte[] value) {
        if (value != null) {
            page.setContent(ByteBuffer.wrap(value));
            setContentBytes(value.length);
        } else {
            page.setContent(null);
        }
    }

    public int getContentBytes() {
        return getMetadata().getInt(Name.CONTENT_BYTES, 0);
    }

    public void setContentBytes(int bytes) {
        getMetadata().set(Name.CONTENT_BYTES, String.valueOf(bytes));
    }

    public String getContentType() {
        return page.getContentType() == null ? "" : page.getContentType().toString();
    }

    public void setContentType(String value) {
        page.setContentType(value.trim().toLowerCase());
    }

    public ByteBuffer getPrevSignature() {
        return page.getPrevSignature();
    }

    public void setPrevSignature(ByteBuffer value) {
        page.setPrevSignature(value);
    }

    public String getPrevSignatureAsString() {
        ByteBuffer sig = getPrevSignature();
        if (sig == null) {
            sig = ByteBuffer.wrap("".getBytes());
        }
        return StringUtil.toHexString(sig);
    }

    /**
     * An implementation of a WebPage's signature from which it can be identified and referenced at any point in time.
     * This is essentially the WebPage's fingerprint representing its state for any point in time.
     */
    @Nullable
    public ByteBuffer getSignature() {
        return page.getSignature();
    }

    public void setSignature(byte[] value) {
        page.setSignature(ByteBuffer.wrap(value));
    }

    @Nonnull
    public String getSignatureAsString() {
        ByteBuffer sig = getSignature();
        if (sig == null) {
            sig = ByteBuffer.wrap("".getBytes());
        }
        return StringUtil.toHexString(sig);
    }

    @Nonnull
    public String getPageTitle() {
        return page.getPageTitle() == null ? "" : page.getPageTitle().toString();
    }

    public void setPageTitle(String pageTitle) {
        page.setPageTitle(pageTitle);
    }

    @Nonnull
    public String getContentTitle() {
        return page.getContentTitle() == null ? "" : page.getContentTitle().toString();
    }

    public void setContentTitle(String contentTitle) {
        if (contentTitle != null) {
            page.setContentTitle(contentTitle);
        }
    }

    @Nonnull
    public String sniffTitle() {
        String title = getContentTitle();
        if (title.isEmpty()) {
            title = getAnchor().toString();
        }
        if (title.isEmpty()) {
            title = getPageTitle();
        }
        if (title.isEmpty()) {
            title = getBaseUrl();
        }
        if (title.isEmpty()) {
            title = url;
        }
        return title;
    }

    @Nonnull
    public String getPageText() {
        return page.getPageText() == null ? "" : page.getPageText().toString();
    }

    public void setPageText(String value) {
        if (value != null && !value.isEmpty()) page.setPageText(value);
    }

    @Nonnull
    public String getContentText() {
        return page.getContentText() == null ? "" : page.getContentText().toString();
    }

    public void setContentText(String textContent) {
        if (textContent != null && !textContent.isEmpty()) {
            page.setContentText(textContent);
            page.setContentTextLen(textContent.length());
        }
    }

    public int getContentTextLen() {
        return page.getContentTextLen();
    }

    /**
     * Set all text fields cascaded, including content, content text and page text.
     */
    public void setTextCascaded(String text) {
        setContent(text);
        setContentText(text);
        setPageText(text);
    }

    /**
     * {WebPage#setParseStatus} must be called later if the status is empty
     */
    @Nonnull
    public ParseStatus getParseStatus() {
        GParseStatus parseStatus = page.getParseStatus();
        return ParseStatus.box(parseStatus == null ? GParseStatus.newBuilder().build() : parseStatus);
    }

    public void setParseStatus(ParseStatus parseStatus) {
        page.setParseStatus(parseStatus.unbox());
    }

    /**
     * Embedded hyperlinks which direct outside of the current domain.
     */
    public Map<CharSequence, GHypeLink> getLiveLinks() {
        return page.getLiveLinks();
    }

    public Collection<String> getSimpleLiveLinks() {
        return CollectionUtils.collect(page.getLiveLinks().keySet(), CharSequence::toString);
    }

    /**
     * TODO: Remove redundant url to reduce space
     */
    public void setLiveLinks(Iterable<HypeLink> liveLinks) {
        page.getLiveLinks().clear();
        Map<CharSequence, GHypeLink> links = page.getLiveLinks();
        liveLinks.forEach(l -> links.put(l.getUrl(), l.unbox()));
    }

    /**
     * @param links the value to set.
     */
    public void setLiveLinks(Map<CharSequence, GHypeLink> links) {
        page.setLiveLinks(links);
    }

    public void addLiveLink(HypeLink hypeLink) {
        page.getLiveLinks().put(hypeLink.getUrl(), hypeLink.unbox());
    }

    public Map<CharSequence, CharSequence> getVividLinks() {
        return page.getVividLinks();
    }

    public Collection<String> getSimpleVividLinks() {
        return CollectionUtils.collect(page.getVividLinks().keySet(), CharSequence::toString);
    }

    public void setVividLinks(Map<CharSequence, CharSequence> links) {
        page.setVividLinks(links);
    }

    public List<CharSequence> getDeadLinks() {
        return page.getDeadLinks();
    }

    public void setDeadLinks(List<CharSequence> deadLinks) {
        page.setDeadLinks(deadLinks);
    }

    public List<CharSequence> getLinks() {
        return page.getLinks();
    }

    public void setLinks(List<CharSequence> links) {
        page.setLinks(links);
    }

    /**
     * Record all links appeared in a page
     * The links are in FIFO order, for each time we fetch and parse a page,
     * we push newly discovered links to the queue, if the queue is full, we drop out some old ones,
     * usually they do not appears in the page any more.
     * <p>
     * TODO: compress links
     * TODO: HBase seems not modify any nested array
     */
    public void addHyperLinks(Iterable<HypeLink> hypeLinks) {
        List<CharSequence> links = page.getLinks();

        // If there are too many links, Drop the front 1/3 links
        if (links.size() > MAX_LINK_PER_PAGE) {
            links = links.subList(links.size() - MAX_LINK_PER_PAGE / 3, links.size());
        }

        for (HypeLink l : hypeLinks) {
            Utf8 url = u8(l.getUrl());
            if (!links.contains(url)) {
                links.add(url);
            }
        }

        setLinks(links);
        setImpreciseLinkCount(links.size());
    }

    public void addLinks(Iterable<CharSequence> hypeLinks) {
        List<CharSequence> links = page.getLinks();

        // If there are too many links, Drop the front 1/3 links
        if (links.size() > MAX_LINK_PER_PAGE) {
            links = links.subList(links.size() - MAX_LINK_PER_PAGE / 3, links.size());
        }

        for (CharSequence link : hypeLinks) {
            Utf8 url = u8(link.toString());
            // Use a set?
            if (!links.contains(url)) {
                links.add(url);
            }
        }

        setLinks(links);
        setImpreciseLinkCount(links.size());
    }

    public int getImpreciseLinkCount() {
        String count = getMetadata().getOrDefault(Name.TOTAL_OUT_LINKS, "0");
        return NumberUtils.toInt(count, 0);
    }

    public void setImpreciseLinkCount(int count) {
        getMetadata().set(Name.TOTAL_OUT_LINKS, String.valueOf(count));
    }

    public void increaseImpreciseLinkCount(int count) {
        int oldCount = getImpreciseLinkCount();
        setImpreciseLinkCount(oldCount + count);
    }

    public Map<CharSequence, CharSequence> getInlinks() {
        return page.getInlinks();
    }

    @Nonnull
    public CharSequence getAnchor() {
        return page.getAnchor() != null ? page.getAnchor() : "";
    }

    /**
     * Anchor can be used to sniff article title
     */
    public void setAnchor(CharSequence anchor) {
        page.setAnchor(anchor);
    }

    public String[] getInlinkAnchors() {
        return StringUtils.split(getMetadata().getOrDefault(Name.ANCHORS, ""), "\n");
    }

    public void setInlinkAnchors(Collection<CharSequence> anchors) {
        getMetadata().set(Name.ANCHORS, StringUtils.join(anchors, "\n"));
    }

    public int getAnchorOrder() {
        int order = page.getAnchorOrder();
        return order < 0 ? MAX_LIVE_LINK_PER_PAGE : order;
    }

    public void setAnchorOrder(int order) {
        page.setAnchorOrder(order);
    }

    public Instant getContentPublishTime() {
        return Instant.ofEpochMilli(page.getContentPublishTime());
    }

    public void setContentPublishTime(Instant publishTime) {
        page.setContentPublishTime(publishTime.toEpochMilli());
    }

    private boolean isValidContentModifyTime(Instant publishTime) {
        return publishTime.isAfter(MIN_ARTICLE_PUBLISH_TIME) && publishTime.isBefore(imprecise2DaysAhead);
    }

    public boolean updateContentPublishTime(Instant newPublishTime) {
        if (!isValidContentModifyTime(newPublishTime)) {
            return false;
        }

        Instant lastPublishTime = getContentPublishTime();
        if (newPublishTime.isAfter(lastPublishTime)) {
            setPrevContentPublishTime(lastPublishTime);
            setContentPublishTime(newPublishTime);
        }

        return true;
    }

    public Instant getPrevContentPublishTime() {
        return Instant.ofEpochMilli(page.getPrevContentPublishTime());
    }

    public void setPrevContentPublishTime(Instant publishTime) {
        page.setPrevContentPublishTime(publishTime.toEpochMilli());
    }

    public Instant getRefContentPublishTime() {
        return Instant.ofEpochMilli(page.getRefContentPublishTime());
    }

    public void setRefContentPublishTime(Instant publishTime) {
        page.setRefContentPublishTime(publishTime.toEpochMilli());
    }

    public Instant getContentModifiedTime() {
        return Instant.ofEpochMilli(page.getContentModifiedTime());
    }

    public void setContentModifiedTime(Instant modifiedTime) {
        page.setContentModifiedTime(modifiedTime.toEpochMilli());
    }

    public Instant getPrevContentModifiedTime() {
        return Instant.ofEpochMilli(page.getPrevContentModifiedTime());
    }

    public void setPrevContentModifiedTime(Instant modifiedTime) {
        page.setPrevContentModifiedTime(modifiedTime.toEpochMilli());
    }

    public boolean updateContentModifiedTime(Instant newModifiedTime) {
        if (!isValidContentModifyTime(newModifiedTime)) {
            return false;
        }

        Instant lastModifyTime = getContentModifiedTime();
        if (newModifiedTime.isAfter(lastModifyTime)) {
            setPrevContentModifiedTime(lastModifyTime);
            setContentModifiedTime(newModifiedTime);
        }

        return true;
    }

    public Instant getPrevRefContentPublishTime() {
        return Instant.ofEpochMilli(page.getPrevRefContentPublishTime());
    }

    public void setPrevRefContentPublishTime(Instant publishTime) {
        page.setPrevRefContentPublishTime(publishTime.toEpochMilli());
    }

    public boolean updateRefContentPublishTime(Instant newRefPublishTime) {
        if (!isValidContentModifyTime(newRefPublishTime)) {
            return false;
        }

        Instant latestRefPublishTime = getRefContentPublishTime();

        // LOG.debug("Ref Content Publish Time: " + latestRefPublishTime + " -> " + newRefPublishTime + ", Url: " + getUrl());

        if (newRefPublishTime.isAfter(latestRefPublishTime)) {
            setPrevRefContentPublishTime(latestRefPublishTime);
            setRefContentPublishTime(newRefPublishTime);

            // LOG.debug("[Updated] " + latestRefPublishTime + " -> " + newRefPublishTime);

            return true;
        }

        return false;
    }

    @Nonnull
    public String getReferrer() {
        return page.getReferrer() == null ? "" : page.getReferrer().toString();
    }

    public void setReferrer(String referrer) {
        if (referrer != null && referrer.length() > SHORTEST_VALID_URL_LENGTH) {
            page.setReferrer(referrer);
        }
    }

    /********************************************************************************
     * Page Model
     ********************************************************************************/

    @Nonnull
    public PageModel getPageModel() {
        return PageModel.box(page.getPageModel());
    }

    /********************************************************************************
     * Scoring
     ********************************************************************************/

    public float getScore() {
        return page.getScore();
    }

    public void setScore(float value) {
        page.setScore(value);
    }

    public float getContentScore() {
        return page.getContentScore() == null ? 0.0f : page.getContentScore();
    }

    public void setContentScore(float score) {
        page.setContentScore(score);
    }

    @Nonnull
    public String getSortScore() {
        return page.getSortScore() == null ? "" : page.getSortScore().toString();
    }

    public void setSortScore(String score) {
        page.setSortScore(score);
    }

    public float getCash() {
        return getMetadata().getFloat(Name.CASH_KEY, 0.0f);
    }

    public void setCash(float cash) {
        getMetadata().set(Name.CASH_KEY, String.valueOf(cash));
    }

    @Nonnull
    public PageCounters getPageCounters() {
        return PageCounters.box(page.getPageCounters());
    }

    /********************************************************************************
     * Index
     ********************************************************************************/

    public String getIndexTimeHistory(String defaultValue) {
        String s = getMetadata().get(Name.INDEX_TIME_HISTORY);
        return s == null ? defaultValue : s;
    }

    public void putIndexTimeHistory(Instant indexTime) {
        String indexTimeHistory = getMetadata().get(Name.INDEX_TIME_HISTORY);
        indexTimeHistory = DateTimeUtil.constructTimeHistory(indexTimeHistory, indexTime, 10);
        getMetadata().set(Name.INDEX_TIME_HISTORY, indexTimeHistory);
    }

    public Instant getFirstIndexTime(Instant defaultValue) {
        Instant firstIndexTime = null;

        String indexTimeHistory = getIndexTimeHistory("");
        if (!indexTimeHistory.isEmpty()) {
            String[] times = indexTimeHistory.split(",");
            Instant time = DateTimeUtil.parseInstant(times[0], Instant.EPOCH);
            if (time.isAfter(Instant.EPOCH)) {
                firstIndexTime = time;
            }
        }

        return firstIndexTime == null ? defaultValue : firstIndexTime;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof WebPage && ((WebPage) other).getUrl().equals(getUrl());
    }

    @Override
    public String toString() {
        return new WebPageFormatter(this).format();
    }
}
