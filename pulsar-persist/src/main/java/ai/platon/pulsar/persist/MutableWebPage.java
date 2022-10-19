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

import ai.platon.pulsar.common.HtmlIntegrity;
import ai.platon.pulsar.common.browser.BrowserType;
import ai.platon.pulsar.common.config.VolatileConfig;
import ai.platon.pulsar.common.urls.UrlUtils;
import ai.platon.pulsar.persist.gora.generated.GActiveDOMStat;
import ai.platon.pulsar.persist.gora.generated.GActiveDOMStatus;
import ai.platon.pulsar.persist.gora.generated.GHypeLink;
import ai.platon.pulsar.persist.gora.generated.GWebPage;
import ai.platon.pulsar.persist.metadata.*;
import ai.platon.pulsar.persist.model.ActiveDOMStat;
import ai.platon.pulsar.persist.model.ActiveDOMStatus;
import ai.platon.pulsar.persist.model.Converters;
import org.apache.avro.util.Utf8;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ai.platon.pulsar.common.PulsarParams.VAR_LOAD_OPTIONS;
import static ai.platon.pulsar.common.config.AppConstants.YES_STRING;

/**
 * The core web page structure
 */
public class MutableWebPage extends WebPage {

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

    MutableWebPage(
            @NotNull String url, @NotNull GWebPage page, boolean urlReversed, @NotNull VolatileConfig conf
    ) {
        this.url = urlReversed ? UrlUtils.unreverseUrl(url) : url;
        this.reversedUrl = urlReversed ? url : UrlUtils.reverseUrlOrEmpty(url);
        this.conf = conf;
        this.page = page;

        // the url of a page might be normalized, but the baseUrl always keeps be the original
        if (page.getBaseUrl() == null) {
            // setLocation(url);
        }
    }

    MutableWebPage(
            @NotNull String url, @NotNull String reversedUrl, @NotNull GWebPage page, @NotNull VolatileConfig conf
    ) {
        this.url = url;
        this.reversedUrl = reversedUrl;
        this.conf = conf;
        this.page = page;

        // BaseUrl is the last working address, it might redirect to url, or it might have random parameters
        if (page.getBaseUrl() == null) {
            // setLocation(url);
        }
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
     * Set The hypertext reference of this page.
     * It defines the address of the document, which this time is linked from
     *
     * @param href The hypertext reference
     */
    public void setHref(@Nullable String href) {
        getMetadata().set(Name.HREF, href);
    }

    @NotNull
    public GWebPage unbox() {
        return page;
    }

    public void unsafeSetGPage(@NotNull GWebPage page) {
        this.page = page;
    }

    public void unsafeCloneGPage(MutableWebPage page) {
        unsafeSetGPage(GWebPage.newBuilder(page.unbox()).build());
    }

    /**
     * *****************************************************************************
     * Common fields
     * ******************************************************************************
     */

    /**
     * Retrieves and removes the local variable with the given name.
     */
    public Object removeVar(@NotNull String name) {
        return variables.remove(name);
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

    public void setCached(boolean cached) {
        isCached = cached;
    }

    public void setLoaded(boolean loaded) {
        isLoaded = loaded;
    }

    public void setFetched(boolean fetched) {
        isFetched = fetched;
    }

    /**
     * Check if the page is canceled.
     *
     * If a page is canceled, it should not be updated.
     * */
    public void setCanceled(boolean canceled) {
        isCanceled = canceled;
    }

    public void setConf(@NotNull VolatileConfig conf) {
        this.conf = conf;
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
     * Set the arguments and clear the LoadOptions object.
     * */
    public void setArgs(@NotNull String args) {
        variables.remove(VAR_LOAD_OPTIONS);
        page.setParams(args);
    }

    public void setRetryDelay(@NotNull Duration retryDelay) {
        this.retryDelay = retryDelay;
    }

    public void setMaxRetries(int maxRetries) {
        getMetadata().set(Name.FETCH_MAX_RETRY, maxRetries);
    }

    public void setFetchedLinkCount(int count) {
        getMetadata().set(Name.FETCHED_LINK_COUNT, count);
    }

    public void setZoneId(@NotNull ZoneId zoneId) {
        page.setZoneId(zoneId.getId());
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

    public void setDistance(int newDistance) {
        page.setDistance(newDistance);
    }

    /**
     * Fetch mode is used to determine the protocol before fetch, so it shall be set before fetch
     */
    public void setFetchMode(@NotNull FetchMode mode) {
        getMetadata().set(Name.FETCH_MODE, mode.name());
    }

    public void setLastBrowser(@NotNull BrowserType browser) {
        page.setBrowser(browser.name());
    }

    public void setResource(boolean resource) {
        if (resource) {
            page.setResource(1);
        }
    }

    public void setHtmlIntegrity(@NotNull HtmlIntegrity integrity) {
        page.setHtmlIntegrity(integrity.name());
    }

    public void setFetchPriority(int priority) {
        page.setFetchPriority(priority);
    }

    public void setCreateTime(@NotNull Instant createTime) {
        page.setCreateTime(createTime.toEpochMilli());
    }

    public void setGenerateTime(@NotNull Instant generateTime) {
        getMetadata().set(Name.GENERATE_TIME, generateTime.toString());
    }

    public void setFetchCount(int count) {
        page.setFetchCount(count);
    }

    public void updateFetchCount() {
        int count = getFetchCount();
        setFetchCount(count + 1);
    }

    public void setCrawlStatus(@NotNull CrawlStatus crawlStatus) {
        page.setCrawlStatus(crawlStatus.getCode());
    }

    public void setCrawlStatus(int value) {
        page.setCrawlStatus(value);
    }

    /**
     * The url is the permanent internal address, it might not still available to access the target.
     * <p>
     * Location is the last working address, it might redirect to url, or it might have additional random parameters.
     * <p>
     * Location may be different from url, it's generally normalized.
     *
     * @param location The location.
     */
    public void setLocation(@NotNull String location) {
        page.setBaseUrl(location);
    }

    /**
     * The latest fetch time
     *
     * @param time The latest fetch time
     */
    public void setFetchTime(@NotNull Instant time) {
        page.setFetchTime(time.toEpochMilli());
    }

    public void setPrevFetchTime(@NotNull Instant time) {
        page.setPrevFetchTime(time.toEpochMilli());
    }

    /**
     * The previous crawl time, used for fat link crawl, which means both the page itself and out pages are fetched
     * */
    public void setPrevCrawlTime1(@NotNull Instant time) {
        page.setPrevCrawlTime1(time.toEpochMilli());
    }

    /**
     * Set fetch interval
     * */
    public void setFetchInterval(@NotNull Duration duration) {
        page.setFetchInterval((int) duration.getSeconds());
    }

    /**
     * Set fetch interval in seconds
     * */
    public void setFetchInterval(long seconds) {
        page.setFetchInterval((int) seconds);
    }

    /**
     * Set fetch interval in seconds
     * */
    public void setFetchInterval(float seconds) {
        page.setFetchInterval(Math.round(seconds));
    }

    /**
     * Set protocol status
     * */
    public void setProtocolStatus(@NotNull ProtocolStatus protocolStatus) {
        page.setProtocolStatus(protocolStatus.unbox());
    }

    public void setReprUrl(@NotNull String value) {
        page.setReprUrl(value);
    }

    public void setFetchRetries(int value) {
        page.setFetchRetries(value);
    }

    public void setModifiedTime(@NotNull Instant value) {
        page.setModifiedTime(value.toEpochMilli());
    }

    public void setPrevModifiedTime(@NotNull Instant value) {
        page.setPrevModifiedTime(value.toEpochMilli());
    }

    /**
     * category : index, detail, review, media, search, etc
     *
     * @param pageCategory a {@link PageCategory} object.
     */
    public void setPageCategory(@NotNull PageCategory pageCategory) {
        page.setPageCategory(pageCategory.format());
    }

    public void setPageCategory(@NotNull OpenPageCategory pageCategory) {
        page.setPageCategory(pageCategory.format());
    }

    /**
     * Set the encoding of the content.
     * Content encoding is detected just before it's parsed.
     */
    public void setEncoding(@Nullable String encoding) {
        page.setEncoding(encoding);
    }

    /**
     * The clues are used to determine the encoding of the page content
     * */
    public void setEncodingClues(@NotNull String clues) {
        getMetadata().set(Name.ENCODING_CLUES, clues);
    }

    /**
     * Set the cached content, keep the persisted page content unmodified
     */
    public void setTmpContent(ByteBuffer tmpContent) {
        this.tmpContent = tmpContent;
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
            computeContentLength(length);
            setPersistedContentLength(length);
        } else {
            clearPersistContent();
        }
    }

    public void clearPersistContent() {
        tmpContent = page.getContent();
        page.setContent(null);
        setPersistedContentLength(0);
    }

    private void setPersistedContentLength(long bytes) {
        page.setPersistedContentLength(bytes);
    }

    public void setContentType(String value) {
        page.setContentType(value.trim().toLowerCase());
    }

    public void setPrevSignature(@Nullable ByteBuffer value) {
        page.setPrevSignature(value);
    }

    /**
     * The last proxy used to fetch the page
     */
    public void setProxy(@Nullable String proxy) {
        page.setProxy(proxy);
    }

    public void setActiveDOMStatTrace(@NotNull Map<String, ActiveDOMStat> trace) {
        Map<CharSequence, GActiveDOMStat> statTrace = trace.entrySet().stream().collect(
                Collectors.toMap(Map.Entry::getKey, e -> Converters.INSTANCE.convert(e.getValue())));
        page.setActiveDOMStatTrace(statTrace);
    }

    public void setActiveDOMStatus(@Nullable ActiveDOMStatus s) {
        if (s == null) {
            return;
        }

        GActiveDOMStatus s2 = page.getActiveDOMStatus();
        if (s2 != null) {
            s2.setN(s.getN());
            s2.setScroll(s.getScroll());
            s2.setSt(s.getSt());
            s2.setR(s.getR());
            s2.setIdl(s.getIdl());
            s2.setEc(s.getEc());
        }
    }

    public void setSignature(byte[] value) {
        page.setSignature(ByteBuffer.wrap(value));
    }

    public void setPageTitle(String pageTitle) {
        page.setPageTitle(pageTitle);
    }

    public void setContentTitle(String contentTitle) {
        if (contentTitle != null) {
            page.setContentTitle(contentTitle);
        }
    }

    public void setPageText(String value) {
        if (value != null && !value.isEmpty()) page.setPageText(value);
    }

    public void setContentText(String textContent) {
        if (textContent != null && !textContent.isEmpty()) {
            page.setContentText(textContent);
            page.setContentTextLen(textContent.length());
        }
    }

    public void setParseStatus(ParseStatus parseStatus) {
        page.setParseStatus(parseStatus.unbox());
    }

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

    public void setVividLinks(Map<CharSequence, CharSequence> links) {
        page.setVividLinks(links);
    }

    public void setDeadLinks(List<CharSequence> deadLinks) {
        page.setDeadLinks(deadLinks);
    }

    public void setLinks(List<CharSequence> links) {
        page.setLinks(links);
    }

    public void setImpreciseLinkCount(int count) {
        getMetadata().set(Name.TOTAL_OUT_LINKS, String.valueOf(count));
    }

    public void setAnchor(CharSequence anchor) {
        page.setAnchor(anchor);
    }

    public void setInlinkAnchors(Collection<CharSequence> anchors) {
        getMetadata().set(Name.ANCHORS, StringUtils.join(anchors, "\n"));
    }

    public void setAnchorOrder(int order) {
        page.setAnchorOrder(order);
    }

    public void setContentPublishTime(Instant publishTime) {
        page.setContentPublishTime(publishTime.toEpochMilli());
    }

    public void setPrevContentPublishTime(Instant publishTime) {
        page.setPrevContentPublishTime(publishTime.toEpochMilli());
    }

    public void setRefContentPublishTime(Instant publishTime) {
        page.setRefContentPublishTime(publishTime.toEpochMilli());
    }

    public void setContentModifiedTime(Instant modifiedTime) {
        page.setContentModifiedTime(modifiedTime.toEpochMilli());
    }

    public void setPrevContentModifiedTime(Instant modifiedTime) {
        page.setPrevContentModifiedTime(modifiedTime.toEpochMilli());
    }

    public void setPrevRefContentPublishTime(Instant publishTime) {
        page.setPrevRefContentPublishTime(publishTime.toEpochMilli());
    }

    public void setReferrer(@Nullable String referrer) {
        if (UrlUtils.isStandard(referrer)) {
            page.setReferrer(referrer);
        }
    }

    public void setPageModelUpdateTime(@Nullable Instant time) {
        page.setPageModelUpdateTime(time == null ? 0 : time.toEpochMilli());
    }

    /**
     * *****************************************************************************
     * Scoring
     * ******************************************************************************
     */
    public void setScore(float value) {
        page.setScore(value);
    }

    public void setContentScore(float score) {
        page.setContentScore(score);
    }

    public void setSortScore(String score) {
        page.setSortScore(score);
    }

    public void setCash(float cash) {
        getMetadata().set(Name.CASH_KEY, String.valueOf(cash));
    }

    /**
     * *****************************************************************************
     * Index
     * ******************************************************************************
     */
}
