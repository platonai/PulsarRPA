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

import ai.platon.pulsar.common.*;
import ai.platon.pulsar.common.config.VolatileConfig;
import ai.platon.pulsar.common.urls.Urls;
import ai.platon.pulsar.persist.gora.generated.*;
import ai.platon.pulsar.persist.metadata.*;
import ai.platon.pulsar.persist.model.*;
import org.apache.avro.util.Utf8;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.gora.util.ByteUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static ai.platon.pulsar.common.config.AppConstants.*;

/**
 * The core data structure across the whole program execution
 *
 * <p>
 * Notice: Use a build-in java string or a Utf8 to serialize strings?
 * </p>
 *
 * <p>
 * see org.apache.gora.hbase.util.HBaseByteInterface#fromBytes
 * </p>
 * <p>
 * In serialization phrase, a byte array created by s.getBytes(UTF8_CHARSET) is serialized, and
 * in deserialization phrase, every string are wrapped to be a Utf8
 * </p>
 * So both build-in string and a Utf8 wrap is OK to serialize, and Utf8 is always returned
 *
 * @author vincent
 * @version $Id: $Id
 */
final public class WebPage implements Comparable<WebPage> {

    /**
     * Constant <code>LOG</code>
     */
    public static final Logger LOG = LoggerFactory.getLogger(WebPage.class);

    /**
     * Constant <code>sequencer</code>
     */
    public static AtomicInteger sequencer = new AtomicInteger();
    /**
     * Constant <code>NIL</code>
     */
    public static WebPage NIL = newInternalPage(NIL_PAGE_URL, 0, "nil", "nil");

    /**
     * The process scope WebPage instance sequence
     */
    private Integer id = sequencer.incrementAndGet();
    /**
     * The url is the permanent internal address, and the location is the last working address
     */
    @NotNull
    private String url = "";
    /**
     * The reversed url of the web page, it's also the key of the underlying storage of this object
     */
    @NotNull
    private String reversedUrl = "";
    /**
     * Underlying persistent object
     */
    @NotNull
    final private GWebPage page;
    /**
     * Web page scope configuration
     */
    @NotNull
    private VolatileConfig conf;
    /**
     * Web page scope variables
     * TODO : we may use it a PageDatum to track all context scope variables
     */
    private final Variables variables = new Variables();

    /**
     * If this page is loaded from database or is created and fetched from the web
     */
    private boolean isLoaded = false;

    /**
     * If we should keep the content in memory even if it's cleared for persistence
     */
    private boolean cachedContentEnabled = false;

    /**
     * If this page is fetched and updated
     */
    private volatile boolean isContentUpdated = false;

    /**
     * The cached content
     */
    private volatile ByteBuffer cachedContent = null;

    private WebPage(
            @NotNull String url, @NotNull GWebPage page, boolean urlReversed, @NotNull VolatileConfig conf
    ) {
        this.url = urlReversed ? Urls.unreverseUrl(url) : url;
        this.reversedUrl = urlReversed ? url : Urls.reverseUrlOrEmpty(url);
        this.conf = conf;
        this.page = page;

        // the url of a page might be normalized, but the baseUrl always keeps be the original
        if (page.getBaseUrl() == null) {
            setLocation(url);
        }
    }

    private WebPage(
            @NotNull String url, @NotNull String reversedUrl, @NotNull GWebPage page, @NotNull VolatileConfig conf
    ) {
        this.url = url;
        this.reversedUrl = reversedUrl;
        this.conf = conf;
        this.page = page;

        // BaseUrl is the last working address, it might redirect to url, or it might have random parameters
        if (page.getBaseUrl() == null) {
            setLocation(url);
        }
    }

    /**
     * <p>newWebPage.</p>
     *
     * @param url  a {@link java.lang.String} object.
     * @param conf a {@link ai.platon.pulsar.common.config.VolatileConfig} object.
     * @return a {@link ai.platon.pulsar.persist.WebPage} object.
     */
    @NotNull
    public static WebPage newWebPage(@NotNull String url, @NotNull VolatileConfig conf) {
        return newWebPage(url, conf, null);
    }

    /**
     * <p>newWebPage.</p>
     *
     * @param url a {@link java.lang.String} object.
     * @return a {@link ai.platon.pulsar.persist.WebPage} object.
     */
    @NotNull
    public static WebPage newTestWebPage(@NotNull String url) {
        return newWebPage(url, new VolatileConfig(), null);
    }

    /**
     * <p>newWebPage.</p>
     *
     * @param url  a {@link java.lang.String} object.
     * @param conf a {@link ai.platon.pulsar.common.config.VolatileConfig} object.
     * @return a {@link ai.platon.pulsar.persist.WebPage} object.
     */
    @NotNull
    public static WebPage newWebPage(@NotNull String url, @NotNull VolatileConfig conf, @Nullable String href) {
        return newWebPageInternal(url, conf, href);
    }

    @NotNull
    private static WebPage newWebPageInternal(@NotNull String url, @NotNull VolatileConfig conf, @Nullable String href) {
        WebPage page = new WebPage(url, GWebPage.newBuilder().build(), false, conf);

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

    /**
     * <p>newInternalPage.</p>
     *
     * @param url a {@link java.lang.String} object.
     * @return a {@link ai.platon.pulsar.persist.WebPage} object.
     */
    @NotNull
    public static WebPage newInternalPage(@NotNull String url) {
        return newInternalPage(url, "internal", "internal");
    }

    /**
     * <p>newInternalPage.</p>
     *
     * @param url   a {@link java.lang.String} object.
     * @param title a {@link java.lang.String} object.
     * @return a {@link ai.platon.pulsar.persist.WebPage} object.
     */
    @NotNull
    public static WebPage newInternalPage(@NotNull String url, @NotNull String title) {
        return newInternalPage(url, title, "internal");
    }

    /**
     * <p>newInternalPage.</p>
     *
     * @param url     a {@link java.lang.String} object.
     * @param title   a {@link java.lang.String} object.
     * @param content a {@link java.lang.String} object.
     * @return a {@link ai.platon.pulsar.persist.WebPage} object.
     */
    @NotNull
    public static WebPage newInternalPage(@NotNull String url, @NotNull String title, @NotNull String content) {
        return newInternalPage(url, -1, title, content);
    }

    /**
     * <p>newInternalPage.</p>
     *
     * @param url     a {@link java.lang.String} object.
     * @param id      a int.
     * @param title   a {@link java.lang.String} object.
     * @param content a {@link java.lang.String} object.
     * @return a {@link ai.platon.pulsar.persist.WebPage} object.
     */
    @NotNull
    public static WebPage newInternalPage(@NotNull String url, int id, @NotNull String title, @NotNull String content) {
        VolatileConfig unsafe = VolatileConfig.Companion.getUNSAFE();
        WebPage page = WebPage.newWebPage(url, unsafe);
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
     *
     * @param url         a {@link java.lang.String} object.
     * @param reversedUrl a {@link java.lang.String} object.
     * @param page        a {@link ai.platon.pulsar.persist.gora.generated.GWebPage} object.
     * @return a {@link ai.platon.pulsar.persist.WebPage} object.
     */
    @NotNull
    public static WebPage box(
            @NotNull String url, @NotNull String reversedUrl, @NotNull GWebPage page, @NotNull VolatileConfig conf) {
        return new WebPage(url, reversedUrl, page, conf);
    }

    /**
     * Initialize a WebPage with the underlying GWebPage instance.
     *
     * @param url  a {@link java.lang.String} object.
     * @param page a {@link ai.platon.pulsar.persist.gora.generated.GWebPage} object.
     * @return a {@link ai.platon.pulsar.persist.WebPage} object.
     */
    @NotNull
    public static WebPage box(@NotNull String url, @NotNull GWebPage page, @NotNull VolatileConfig conf) {
        return box(url, page, false, conf);
    }

    /**
     * Initialize a WebPage with the underlying GWebPage instance.
     *
     * @param url         a {@link java.lang.String} object.
     * @param page        a {@link ai.platon.pulsar.persist.gora.generated.GWebPage} object.
     * @param urlReversed a boolean.
     * @return a {@link ai.platon.pulsar.persist.WebPage} object.
     */
    @NotNull
    public static WebPage box(
            @NotNull String url, @NotNull GWebPage page, boolean urlReversed, @NotNull VolatileConfig conf
    ) {
        return new WebPage(url, page, urlReversed, conf);
    }

    /**
     * *****************************************************************************
     * Other
     * ******************************************************************************
     *
     * @param mark a {@link ai.platon.pulsar.persist.metadata.Mark} object.
     * @return a {@link org.apache.avro.util.Utf8} object.
     */

    @NotNull
    public static Utf8 wrapKey(@NotNull Mark mark) {
        return u8(mark.value());
    }

    /**
     * What's the difference between String and Utf8?
     *
     * @param value a {@link java.lang.String} object.
     * @return a {@link org.apache.avro.util.Utf8} object.
     */
    @Nullable
    public static Utf8 u8(@Nullable String value) {
        if (value == null) {
            // TODO: return new Utf8.EMPTY?
            return null;
        }
        return new Utf8(value);
    }

    /**
     * page.location is the last working address, and page.url is the permanent internal address
     *
     * @return a {@link java.lang.String} object.
     */
    @NotNull
    public String getUrl() {
        return url;
    }

    /**
     * <p>getKey.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @NotNull
    public String getKey() {
        return getReversedUrl();
    }

    /**
     * <p>Getter for the field <code>reversedUrl</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @NotNull
    public String getReversedUrl() {
        return reversedUrl != null ? reversedUrl : "";
    }

    /**
     * <p>Getter for the field <code>id</code>.</p>
     *
     * @return a int.
     */
    public int getId() {
        return id;
    }

    /**
     * Get The hypertext reference of this page.
     * It defines the address of the document, which this time is linked from
     * <p>
     * TODO: use a seperate field to hold href
     *
     * @return The hypertext reference
     */
    @Nullable
    public String getHref() {
        return getMetadata().get(Name.HREF);
    }

    /**
     * Set The hypertext reference of this page.
     * It defines the address of the document, which this time is linked from
     *
     * @param href The hypertext reference
     */
    public void setHref(@Nullable String href) {
        getMetadata().set(Name.HREF, href);
    }

    /**
     * <p>isNil.</p>
     *
     * @return a boolean.
     */
    public boolean isNil() {
        return this == NIL;
    }

    /**
     * <p>isNotNil.</p>
     *
     * @return a boolean.
     */
    public boolean isNotNil() {
        return !isNil();
    }

    /**
     * <p>isInternal.</p>
     *
     * @return a boolean.
     */
    public boolean isInternal() {
        return hasMark(Mark.INTERNAL);
    }

    /**
     * <p>isNotInternal.</p>
     *
     * @return a boolean.
     */
    public boolean isNotInternal() {
        return !isInternal();
    }

    /**
     * <p>unbox.</p>
     *
     * @return a {@link ai.platon.pulsar.persist.gora.generated.GWebPage} object.
     */
    @NotNull
    public GWebPage unbox() {
        return page;
    }

    /**
     * *****************************************************************************
     * Common fields
     * ******************************************************************************
     *
     * @return a {@link ai.platon.pulsar.persist.Variables} object.
     */

    @NotNull
    public Variables getVariables() {
        return variables;
    }

    /**
     * Check if the page scope temporary variable with name {@name} exist
     *
     * @param name The variable name to check
     * @return true if the variable exist
     */
    public boolean hasVar(@NotNull String name) {
        return variables.contains(name);
    }

    /**
     * Get a page scope temporary variable
     *
     * @param name a {@link String} object.
     * @return a Object or null.
     */
    public Object getVar(@NotNull String name) {
        return variables.get(name);
    }

    /**
     * <p>getAndRemoveVar.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean getAndRemoveVar(@NotNull String name) {
        boolean exist = variables.contains(name);
        if (exist) {
            variables.remove(name);
        }
        return exist;
    }

    /**
     * Get a page scope temporary variable
     *
     * @param name  The variable name.
     * @param value The variable value.
     */
    public void setVar(@NotNull String name, @NotNull Object value) {
        variables.set(name, value);
    }

    public boolean isLoaded() {
        return isLoaded;
    }

    public void setLoaded(boolean loaded) {
        isLoaded = loaded;
    }

    public boolean isContentUpdated() {
        return isContentUpdated;
    }

    public boolean isCachedContentEnabled() {
        return cachedContentEnabled;
    }

    public void setCachedContentEnabled(boolean cachedContentEnabled) {
        this.cachedContentEnabled = cachedContentEnabled;
    }

    @NotNull
    public VolatileConfig getConf() {
        return conf;
    }

    public void setConf(@NotNull VolatileConfig conf) {
        this.conf = conf;
    }

    public Metadata getMetadata() {
        return Metadata.box(page.getMetadata());
    }

    public CrawlMarks getMarks() {
        return CrawlMarks.box(page.getMarkers());
    }

    public boolean hasMark(Mark mark) {
        return page.getMarkers().get(wrapKey(mark)) != null;
    }

    /**
     * All options are saved here, including crawl options, link options, entity options and so on
     */
    @NotNull
    public String getArgs() {
        if (page.getOptions() == null) {
            return "";
        }

        return page.getOptions().toString().trim();
    }

    /**
     * <p>set load arguments.</p>
     */
    public void setArgs(@NotNull String args) {
        page.setOptions(args);
    }

    /**
     * <p>getConfiguredUrl.</p>
     */
    @NotNull
    public String getConfiguredUrl() {
        String configuredUrl = url;
        if (page.getOptions() != null) {
            configuredUrl += " " + page.getOptions().toString();
        }
        return configuredUrl;
    }

    /**
     * @deprecated
     */
    @Nullable
    public String getQuery() {
        return getMetadata().get(Name.QUERY);
    }

    /**
     *
     */
    public void setQuery(@Nullable String query) {
        getMetadata().set(Name.QUERY, query);
    }

    public int getFetchedLinkCount() {
        return getMetadata().getInt(Name.FETCHED_LINK_COUNT, 0);
    }

    public void setFetchedLinkCount(int count) {
        getMetadata().set(Name.FETCHED_LINK_COUNT, count);
    }

    @NotNull
    public ZoneId getZoneId() {
        return page.getZoneId() == null ? DateTimes.INSTANCE.getZoneId() : ZoneId.of(page.getZoneId().toString());
    }

    public void setZoneId(@NotNull ZoneId zoneId) {
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

    @NotNull
    public FetchMode getFetchMode() {
        return FetchMode.fromString(getMetadata().get(Name.FETCH_MODE));
    }

    /**
     * Fetch mode is used to determine the protocol before fetch, so it shall be set before fetch
     */
    public void setFetchMode(@NotNull FetchMode mode) {
        getMetadata().set(Name.FETCH_MODE, mode.name());
    }

    @NotNull
    public BrowserType getLastBrowser() {
        return BrowserType.fromString(getMetadata().get(Name.BROWSER));
    }

    public void setLastBrowser(@NotNull BrowserType browser) {
        getMetadata().set(Name.BROWSER, browser.name());
    }

    @NotNull
    public HtmlIntegrity getHtmlIntegrity() {
        return HtmlIntegrity.Companion.fromString(getMetadata().get(Name.HTML_INTEGRITY));
    }

    public void setHtmlIntegrity(@NotNull HtmlIntegrity integrity) {
        getMetadata().set(Name.HTML_INTEGRITY, integrity.name());
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

    @NotNull
    public Instant getCreateTime() {
        return Instant.ofEpochMilli(page.getCreateTime());
    }

    public void setCreateTime(@NotNull Instant createTime) {
        page.setCreateTime(createTime.toEpochMilli());
    }

    @NotNull
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

    public void setGenerateTime(@NotNull Instant generateTime) {
        getMetadata().set(Name.GENERATE_TIME, generateTime.toString());
    }

    @Nullable
    public Instant getModelSyncTime() {
        String modelSyncTime = getMetadata().get(Name.MODEL_SYNC_TIME);
        if (modelSyncTime == null) {
            return null;
        } else {
            return Instant.parse(modelSyncTime);
        }
    }

    public void setModelSyncTime(@Nullable Instant modelSyncTime) {
        if (modelSyncTime != null) {
            getMetadata().set(Name.MODEL_SYNC_TIME, modelSyncTime.toString());
        } else {
            getMetadata().set(Name.MODEL_SYNC_TIME, (Instant) null);
        }
    }

    public int getFetchCount() {
        return page.getFetchCount();
    }

    public void setFetchCount(int count) {
        page.setFetchCount(count);
    }

    public void updateFetchCount() {
        int count = getFetchCount();
        setFetchCount(count + 1);
    }

    @NotNull
    public CrawlStatus getCrawlStatus() {
        return new CrawlStatus(page.getCrawlStatus().byteValue());
    }

    public void setCrawlStatus(@NotNull CrawlStatus crawlStatus) {
        page.setCrawlStatus(crawlStatus.getCode());
    }

    public void setCrawlStatus(int value) {
        page.setCrawlStatus(value);
    }

    /**
     * The baseUrl is as the same as Location
     * <p>
     * A baseUrl has the same semantic with Jsoup.parse:
     *
     * @return a {@link java.lang.String} object.
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
     *
     * @return a {@link java.lang.String} object.
     */
    public String getLocation() {
        return getBaseUrl();
    }

    /**
     * The url is the permanent internal address, it might not still available to access the target.
     * <p>
     * Location is the last working address, it might redirect to url, or it might have additional random parameters.
     * <p>
     * Location may be different from url, it's generally normalized.
     *
     * @param value The location.
     */
    public void setLocation(@NotNull String value) {
        page.setBaseUrl(value);
    }

    /**
     * The next fetch time
     *
     * @return The next fetch time
     */
    @NotNull
    public Instant getFetchTime() {
        return Instant.ofEpochMilli(page.getFetchTime());
    }

    /**
     * The next fetch time
     *
     * @param time The next fetch time
     */
    public void setFetchTime(@NotNull Instant time) {
        page.setFetchTime(time.toEpochMilli());
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

    public void setPrevFetchTime(@NotNull Instant time) {
        page.setPrevFetchTime(time.toEpochMilli());
    }

    @NotNull
    public Instant getPrevCrawlTime1() {
        return Instant.ofEpochMilli(page.getPrevCrawlTime1());
    }

    public void setPrevCrawlTime1(@NotNull Instant time) {
        page.setPrevCrawlTime1(time.toEpochMilli());
    }

    @NotNull
    public Duration getFetchInterval() {
        return Duration.ofSeconds(page.getFetchInterval());
    }

    public void setFetchInterval(@NotNull Duration interval) {
        page.setFetchInterval((int) interval.getSeconds());
    }

    public void setFetchInterval(long interval) {
        page.setFetchInterval((int) interval);
    }

    public void setFetchInterval(float interval) {
        page.setFetchInterval(Math.round(interval));
    }

    @NotNull
    public ProtocolStatus getProtocolStatus() {
        GProtocolStatus protocolStatus = page.getProtocolStatus();
        if (protocolStatus == null) {
            protocolStatus = GProtocolStatus.newBuilder().build();
        }
        return ProtocolStatus.box(protocolStatus);
    }

    public void setProtocolStatus(@NotNull ProtocolStatus protocolStatus) {
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
     *
     * @return a {@link ai.platon.pulsar.persist.ProtocolHeaders} object.
     */
    @NotNull
    public ProtocolHeaders getHeaders() {
        return ProtocolHeaders.box(page.getHeaders());
    }

    @NotNull
    public String getReprUrl() {
        return page.getReprUrl() == null ? "" : page.getReprUrl().toString();
    }

    public void setReprUrl(@NotNull String value) {
        page.setReprUrl(value);
    }

    public int getFetchRetries() {
        return page.getFetchRetries();
    }

    public void setFetchRetries(int value) {
        page.setFetchRetries(value);
    }

    @NotNull
    public Duration getLastTimeout() {
        String s = getMetadata().get(Name.RESPONSE_TIME);
        return s == null ? Duration.ZERO : Duration.parse(s);
    }

    @NotNull
    public Instant getModifiedTime() {
        return Instant.ofEpochMilli(page.getModifiedTime());
    }

    public void setModifiedTime(@NotNull Instant value) {
        page.setModifiedTime(value.toEpochMilli());
    }

    @NotNull
    public Instant getPrevModifiedTime() {
        return Instant.ofEpochMilli(page.getPrevModifiedTime());
    }

    public void setPrevModifiedTime(@NotNull Instant value) {
        page.setPrevModifiedTime(value.toEpochMilli());
    }

    @NotNull
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

    @NotNull
    public String getFetchTimeHistory(@NotNull String defaultValue) {
        String s = getMetadata().get(Name.FETCH_TIME_HISTORY);
        return s == null ? defaultValue : s;
    }

    /**
     * *****************************************************************************
     * Parsing
     * ******************************************************************************
     */
    public void updateFetchTimeHistory(@NotNull Instant fetchTime) {
        String fetchTimeHistory = getMetadata().get(Name.FETCH_TIME_HISTORY);
        fetchTimeHistory = DateTimes.constructTimeHistory(fetchTimeHistory, fetchTime, 10);
        getMetadata().set(Name.FETCH_TIME_HISTORY, fetchTimeHistory);
    }

    /**
     * <p>getFirstCrawlTime.</p>
     *
     * @param defaultValue a {@link java.time.Instant} object.
     * @return a {@link java.time.Instant} object.
     */
    public @NotNull
    Instant getFirstCrawlTime(@NotNull Instant defaultValue) {
        Instant firstCrawlTime = null;

        String fetchTimeHistory = getFetchTimeHistory("");
        if (!fetchTimeHistory.isEmpty()) {
            String[] times = fetchTimeHistory.split(",");
            Instant time = DateTimes.parseInstant(times[0], Instant.EPOCH);
            if (time.isAfter(Instant.EPOCH)) {
                firstCrawlTime = time;
            }
        }

        return firstCrawlTime == null ? defaultValue : firstCrawlTime;
    }

    /**
     * namespace : metadata, seed, www
     * reserved
     *
     * @return a {@link java.lang.String} object.
     */
    @Nullable
    public String getNamespace() {
        return getMetadata().get("namespace");
    }

    /**
     * reserved
     *
     * @param ns a {@link java.lang.String} object.
     */
    public void setNamespace(String ns) {
        getMetadata().set("namespace", ns);
    }

    /**
     * <p>getPageCategory.</p>
     *
     * @return a {@link ai.platon.pulsar.persist.metadata.PageCategory} object.
     */
    @NotNull
    public PageCategory getPageCategory() {
        try {
            if (page.getPageCategory() != null) {
                return PageCategory.parse(page.getPageCategory().toString());
            }
        } catch (Throwable ignored) {
        }

        return PageCategory.UNKNOWN;
    }

    /**
     * category : index, detail, review, media, search, etc
     *
     * @param pageCategory a {@link ai.platon.pulsar.persist.metadata.PageCategory} object.
     */
    public void setPageCategory(@NotNull PageCategory pageCategory) {
        page.setPageCategory(pageCategory.toString());
    }

    public void setPageCategory(@NotNull OpenPageCategory pageCategory) {
        page.setPageCategory(pageCategory.toString());
    }

    /**
     * <p>getEncoding.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Nullable
    public String getEncoding() {
        return page.getEncoding() == null ? null : page.getEncoding().toString();
    }

    /**
     * <p>setEncoding.</p>
     *
     * @param encoding a {@link java.lang.String} object.
     */
    public void setEncoding(@Nullable String encoding) {
        page.setEncoding(encoding);
        getMetadata().set(Name.CHAR_ENCODING_FOR_CONVERSION, encoding);
    }

    /**
     * Get content encoding
     * Content encoding is detected just before it's parsed
     *
     * @param defaultEncoding a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    @NotNull
    public String getEncodingOrDefault(@NotNull String defaultEncoding) {
        return page.getEncoding() == null ? defaultEncoding : page.getEncoding().toString();
    }

    /**
     * <p>getEncodingClues.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @NotNull
    public String getEncodingClues() {
        return getMetadata().getOrDefault(Name.ENCODING_CLUES, "");
    }

    /**
     * <p>setEncodingClues.</p>
     *
     * @param clues a {@link java.lang.String} object.
     */
    public void setEncodingClues(@NotNull String clues) {
        getMetadata().set(Name.ENCODING_CLUES, clues);
    }

    /**
     * The entire raw document content e.g. raw XHTML
     *
     * @return a {@link java.nio.ByteBuffer} object.
     */
    @Nullable
    public ByteBuffer getContent() {
        if (cachedContent != null) {
            return cachedContent;
        }
        return page.getContent();
    }

    /**
     * Get the cached content
     */
    @Nullable
    public ByteBuffer getCachedContent() {
        return cachedContent;
    }

    /**
     * Set the cached content, keep the page content unmodified
     */
    public void setCachedContent(ByteBuffer cachedContent) {
        this.cachedContent = cachedContent;
    }

    /**
     * Get the uncached content
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
     * TODO: Encoding is always UTF-8?
     * <p>
     * Get the page content as a string
     */
    @NotNull
    public String getContentAsString() {
        return ByteUtils.toString(getContentAsBytes());
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
     * Set the page content
     */
    public void setContent(@Nullable String value) {
        if (value != null) {
            setContent(value.getBytes());
        } else {
            setContent((ByteBuffer) null);
        }
    }

    /**
     * Set the page content
     */
    public void setContent(@Nullable byte[] value) {
        if (value != null) {
            setContent(ByteBuffer.wrap(value));
        } else {
            setContent((ByteBuffer) null);
        }
    }

    /**
     * Set the page content
     *
     * @param value a ByteBuffer.
     */
    public void setContent(@Nullable ByteBuffer value) {
        if (value != null) {
            page.setContent(value);
            isContentUpdated = true;

            int length = value.array().length;
            setContentLength(length);
            setPersistContentLength(length);
        } else {
            clearPersistContent();
        }
    }

    public void clearPersistContent() {
        if (isCachedContentEnabled()) {
            // set cached content so other thread still can use it
            cachedContent = page.getContent();
        }

        page.setContent(null);
        setPersistContentLength(0);
    }

    /**
     * TODO: check consistency with HttpHeaders.CONTENT_LENGTH
     *
     * @return The content length
     */
    public long getContentLength() {
        return getMetadata().getLong(Name.CONTENT_BYTES, 0);
    }

    /**
     * TODO: use a field
     */
    private void setContentLength(long bytes) {
        long lastBytes = getContentLength();
        Metadata metadata = getMetadata();
        metadata.set(Name.LAST_CONTENT_BYTES, lastBytes);
        metadata.set(Name.CONTENT_BYTES, bytes);
        metadata.set(Name.AVE_CONTENT_BYTES, getAverageContentBytes(bytes));
    }

    private long getAverageContentBytes(long bytes) {
        Metadata metadata = getMetadata();

        int count = getFetchCount();
        long lastAveBytes = metadata.getLong(Name.AVE_CONTENT_BYTES, 0);

        long aveBytes;
        if (count > 0 && lastAveBytes == 0) {
            // old version, average bytes is not calculated
            aveBytes = bytes;
        } else {
            aveBytes = (lastAveBytes * count + bytes) / (count + 1);
        }

        return aveBytes;
    }

    public long getPersistContentLength() {
        return getMetadata().getLong(Name.PERSIST_CONTENT_BYTES, 0);
    }

    private void setPersistContentLength(long bytes) {
        getMetadata().set(Name.PERSIST_CONTENT_BYTES, bytes);
    }

    public long getLastContentBytes() {
        return getMetadata().getLong(Name.LAST_CONTENT_BYTES, 0);
    }

    /**
     * <p>getAveContentBytes.</p>
     *
     * @return a int.
     */
    public long getAveContentBytes() {
        return getMetadata().getLong(Name.AVE_CONTENT_BYTES, 0);
    }

    /**
     * <p>getContentType.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @NotNull
    public String getContentType() {
        return page.getContentType() == null ? "" : page.getContentType().toString();
    }

    /**
     * <p>setContentType.</p>
     *
     * @param value a {@link java.lang.String} object.
     */
    public void setContentType(String value) {
        page.setContentType(value.trim().toLowerCase());
    }

    /**
     * <p>getPrevSignature.</p>
     *
     * @return a {@link java.nio.ByteBuffer} object.
     */
    @Nullable
    public ByteBuffer getPrevSignature() {
        return page.getPrevSignature();
    }

    /**
     * <p>setPrevSignature.</p>
     *
     * @param value a {@link java.nio.ByteBuffer} object.
     */
    public void setPrevSignature(@Nullable ByteBuffer value) {
        page.setPrevSignature(value);
    }

    /**
     * <p>getPrevSignatureAsString.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @NotNull
    public String getPrevSignatureAsString() {
        ByteBuffer sig = getPrevSignature();
        if (sig == null) {
            sig = ByteBuffer.wrap("".getBytes());
        }
        return Strings.toHexString(sig);
    }

    /**
     * <p>getProxy.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Nullable
    public String getProxy() {
        return getMetadata().get(Name.PROXY);
    }

    /**
     * <p>setProxy.</p>
     *
     * @param proxy a {@link java.lang.String} object.
     */
    public void setProxy(@Nullable String proxy) {
        if (proxy != null) {
            getMetadata().set(Name.PROXY, proxy);
        }
    }

    @Nullable
    public ActiveDomStatus getActiveDomStatus() {
        GActiveDomStatus s = page.getActiveDomStatus();
        if (s == null) return null;

        return new ActiveDomStatus(
                s.getN(),
                s.getScroll(),
                s.getSt().toString(),
                s.getR().toString(),
                s.getIdl().toString(),
                s.getEc().toString()
        );
    }

    public void setActiveDomStatus(ActiveDomStatus s) {
        GActiveDomStatus s2 = page.getActiveDomStatus();
        if (s2 != null) {
            if (s == null) {
                page.setActiveDomStatus(null);
            }

            s2.setN(s.getN());
            s2.setScroll(s.getScroll());
            s2.setSt(s.getSt());
            s2.setR(s.getR());
            s2.setIdl(s.getIdl());
            s2.setEc(s.getEc());
        }
    }

    @NotNull
    public Map<String, ActiveDomStat> getActiveDomStats() {
        Map<CharSequence, GActiveDomStat> s = page.getActiveDomStats();
        return s.entrySet().stream().collect(Collectors.toMap(
                e -> e.getKey().toString(),
                e -> Converters.INSTANCE.convert(e.getValue())
        ));
    }

    public void setActiveDomStats(@NotNull Map<String, ActiveDomStat> stats) {
        Map<CharSequence, GActiveDomStat> stats2 = stats.entrySet().stream().collect(
                Collectors.toMap(Map.Entry::getKey, e -> Converters.INSTANCE.convert(e.getValue())));
        page.setActiveDomStats(stats2);
    }

    @NotNull
    public ActiveDomUrls getActiveDomUrls() {
        Map<CharSequence, CharSequence> urls = page.getActiveDomUrls();
        return new ActiveDomUrls(
                urls.getOrDefault("URL", "").toString(),
                urls.getOrDefault("baseURI", "").toString(),
                urls.getOrDefault("location", "").toString(),
                urls.getOrDefault("documentURI", "").toString()
        );
    }

    /**
     * <p>setActiveDomUrls.</p>
     *
     * @param urls a {@link ai.platon.pulsar.persist.model.ActiveDomUrls} object.
     */
    public void setActiveDomUrls(@NotNull ActiveDomUrls urls) {
        Map<CharSequence, CharSequence> domUrls = page.getActiveDomUrls();
        domUrls.put("URL", urls.getURL());
        domUrls.put("baseURI", urls.getBaseURI());
        domUrls.put("location", urls.getLocation());
        domUrls.put("documentURI", urls.getDocumentURI());
    }

    /**
     * An implementation of a WebPage's signature from which it can be identified and referenced at any point in time.
     * This is essentially the WebPage's fingerprint representing its state for any point in time.
     *
     * @return a {@link java.nio.ByteBuffer} object.
     */
    @Nullable
    public ByteBuffer getSignature() {
        return page.getSignature();
    }

    /**
     * <p>setSignature.</p>
     *
     * @param value an array of {@link byte} objects.
     */
    public void setSignature(byte[] value) {
        page.setSignature(ByteBuffer.wrap(value));
    }

    /**
     * <p>getSignatureAsString.</p>
     *
     * @return a {@link java.lang.String} object.
     */
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

    public void setPageTitle(String pageTitle) {
        page.setPageTitle(pageTitle);
    }

    @NotNull
    public String getContentTitle() {
        return page.getContentTitle() == null ? "" : page.getContentTitle().toString();
    }

    public void setContentTitle(String contentTitle) {
        if (contentTitle != null) {
            page.setContentTitle(contentTitle);
        }
    }

    @NotNull
    public String sniffTitle() {
        String title = getContentTitle();
        if (title.isEmpty()) {
            title = getAnchor().toString();
        }
        if (title.isEmpty()) {
            title = getPageTitle();
        }
        if (title.isEmpty()) {
            title = getLocation();
        }
        if (title.isEmpty()) {
            title = url;
        }
        return title;
    }

    @NotNull
    public String getPageText() {
        return page.getPageText() == null ? "" : page.getPageText().toString();
    }

    public void setPageText(String value) {
        if (value != null && !value.isEmpty()) page.setPageText(value);
    }

    @NotNull
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

    public void setTextCascaded(String text) {
        setContent(text);
        setContentText(text);
        setPageText(text);
    }

    @NotNull
    public ParseStatus getParseStatus() {
        GParseStatus parseStatus = page.getParseStatus();
        return ParseStatus.box(parseStatus == null ? GParseStatus.newBuilder().build() : parseStatus);
    }

    public void setParseStatus(ParseStatus parseStatus) {
        page.setParseStatus(parseStatus.unbox());
    }

    public Map<CharSequence, GHypeLink> getLiveLinks() {
        return page.getLiveLinks();
    }

    public Collection<String> getSimpleLiveLinks() {
        return CollectionUtils.collect(page.getLiveLinks().keySet(), CharSequence::toString);
    }

    /**
     * TODO: Remove redundant url to reduce space
     *
     * @param liveLinks a {@link java.lang.Iterable} object.
     */
    public void setLiveLinks(Iterable<HyperlinkPersistable> liveLinks) {
        page.getLiveLinks().clear();
        Map<CharSequence, GHypeLink> links = page.getLiveLinks();
        liveLinks.forEach(l -> links.put(l.getUrl(), l.unbox()));
    }

    public void setLiveLinks(Map<CharSequence, GHypeLink> links) {
        page.setLiveLinks(links);
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
     *
     * @param hypeLinks a {@link java.lang.Iterable} object.
     */
    public void addHyperlinks(Iterable<HyperlinkPersistable> hypeLinks) {
        List<CharSequence> links = page.getLinks();

        // If there are too many links, Drop the front 1/3 links
        if (links.size() > MAX_LINK_PER_PAGE) {
            links = links.subList(links.size() - MAX_LINK_PER_PAGE / 3, links.size());
        }

        for (HyperlinkPersistable l : hypeLinks) {
            Utf8 url = u8(l.getUrl());
            if (!links.contains(url)) {
                links.add(url);
            }
        }

        setLinks(links);
        setImpreciseLinkCount(links.size());
    }

    /**
     * <p>addLinks.</p>
     *
     * @param hypeLinks a {@link java.lang.Iterable} object.
     */
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

    /**
     * <p>getImpreciseLinkCount.</p>
     *
     * @return a int.
     */
    public int getImpreciseLinkCount() {
        String count = getMetadata().getOrDefault(Name.TOTAL_OUT_LINKS, "0");
        return NumberUtils.toInt(count, 0);
    }

    /**
     * <p>setImpreciseLinkCount.</p>
     *
     * @param count a int.
     */
    public void setImpreciseLinkCount(int count) {
        getMetadata().set(Name.TOTAL_OUT_LINKS, String.valueOf(count));
    }

    /**
     * <p>increaseImpreciseLinkCount.</p>
     *
     * @param count a int.
     */
    public void increaseImpreciseLinkCount(int count) {
        int oldCount = getImpreciseLinkCount();
        setImpreciseLinkCount(oldCount + count);
    }

    /**
     * <p>getInlinks.</p>
     *
     * @return a {@link java.util.Map} object.
     */
    public Map<CharSequence, CharSequence> getInlinks() {
        return page.getInlinks();
    }

    /**
     * <p>getAnchor.</p>
     *
     * @return a {@link java.lang.CharSequence} object.
     */
    @NotNull
    public CharSequence getAnchor() {
        return page.getAnchor() != null ? page.getAnchor() : "";
    }

    /**
     * Anchor can be used to sniff article title
     *
     * @param anchor a {@link java.lang.CharSequence} object.
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
        return publishTime.isAfter(MIN_ARTICLE_PUBLISH_TIME);
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

    /**
     * <p>getReferrer.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @NotNull
    public String getReferrer() {
        return page.getReferrer() == null ? "" : page.getReferrer().toString();
    }

    /**
     * <p>setReferrer.</p>
     *
     * @param referrer a {@link java.lang.String} object.
     */
    public void setReferrer(String referrer) {
        if (referrer != null && referrer.length() > SHORTEST_VALID_URL_LENGTH) {
            page.setReferrer(referrer);
        }
    }

    /**
     * *****************************************************************************
     * Page Model
     * ******************************************************************************
     *
     * @return a {@link ai.platon.pulsar.persist.model.PageModel} object.
     */

    @NotNull
    public PageModel getPageModel() {
        return PageModel.box(page.getPageModel());
    }

    /**
     * *****************************************************************************
     * Scoring
     * ******************************************************************************
     *
     * @return a float.
     */
    public float getScore() {
        return page.getScore();
    }

    /**
     * <p>setScore.</p>
     *
     * @param value a float.
     */
    public void setScore(float value) {
        page.setScore(value);
    }

    /**
     * <p>getContentScore.</p>
     *
     * @return a float.
     */
    public float getContentScore() {
        return page.getContentScore() == null ? 0.0f : page.getContentScore();
    }

    /**
     * <p>setContentScore.</p>
     *
     * @param score a float.
     */
    public void setContentScore(float score) {
        page.setContentScore(score);
    }

    /**
     * <p>getSortScore.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @NotNull
    public String getSortScore() {
        return page.getSortScore() == null ? "" : page.getSortScore().toString();
    }

    /**
     * <p>setSortScore.</p>
     *
     * @param score a {@link java.lang.String} object.
     */
    public void setSortScore(String score) {
        page.setSortScore(score);
    }

    /**
     * <p>getCash.</p>
     *
     * @return a float.
     */
    public float getCash() {
        return getMetadata().getFloat(Name.CASH_KEY, 0.0f);
    }

    /**
     * <p>setCash.</p>
     *
     * @param cash a float.
     */
    public void setCash(float cash) {
        getMetadata().set(Name.CASH_KEY, String.valueOf(cash));
    }

    /**
     * <p>getPageCounters.</p>
     *
     * @return a {@link ai.platon.pulsar.persist.PageCounters} object.
     */
    @NotNull
    public PageCounters getPageCounters() {
        return PageCounters.box(page.getPageCounters());
    }

    /**
     * *****************************************************************************
     * Index
     * ******************************************************************************
     *
     * @param defaultValue a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String getIndexTimeHistory(String defaultValue) {
        String s = getMetadata().get(Name.INDEX_TIME_HISTORY);
        return s == null ? defaultValue : s;
    }

    /**
     * <p>putIndexTimeHistory.</p>
     *
     * @param indexTime a {@link java.time.Instant} object.
     */
    public void putIndexTimeHistory(Instant indexTime) {
        String indexTimeHistory = getMetadata().get(Name.INDEX_TIME_HISTORY);
        indexTimeHistory = DateTimes.constructTimeHistory(indexTimeHistory, indexTime, 10);
        getMetadata().set(Name.INDEX_TIME_HISTORY, indexTimeHistory);
    }

    /**
     * <p>getFirstIndexTime.</p>
     *
     * @param defaultValue a {@link java.time.Instant} object.
     * @return a {@link java.time.Instant} object.
     */
    public Instant getFirstIndexTime(Instant defaultValue) {
        Instant firstIndexTime = null;

        String indexTimeHistory = getIndexTimeHistory("");
        if (!indexTimeHistory.isEmpty()) {
            String[] times = indexTimeHistory.split(",");
            Instant time = DateTimes.parseInstant(times[0], Instant.EPOCH);
            if (time.isAfter(Instant.EPOCH)) {
                firstIndexTime = time;
            }
        }

        return firstIndexTime == null ? defaultValue : firstIndexTime;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return url.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(@NotNull WebPage o) {
        return url.compareTo(o.url);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof WebPage && ((WebPage) other).url.equals(url);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return new WebPageFormatter(this).format();
    }
}
