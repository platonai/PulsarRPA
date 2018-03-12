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
package org.warps.pulsar.persist;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.hbase.util.Bytes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.warps.pulsar.persist.gora.generated.GHypeLink;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

public class WebPageFormatter {

    private boolean withText = false;
    private boolean withContent = false;
    private boolean withLinks = false;
    private boolean withFields = false;
    private boolean withEntities = false;
    private WebPage page;
    private ZoneId zoneId;

    public WebPageFormatter(WebPage page) {
        Objects.requireNonNull(page);
        this.page = page;
        this.zoneId = page.getZoneId();
    }

    public WebPageFormatter withText(boolean withText) {
        this.withText = withText;
        return this;
    }

    public WebPageFormatter withText() {
        this.withText = true;
        return this;
    }

    public WebPageFormatter withContent(boolean withContent) {
        this.withContent = withContent;
        return this;
    }

    public WebPageFormatter withContent() {
        this.withContent = true;
        return this;
    }

    public WebPageFormatter withLinks(boolean withLinks) {
        this.withLinks = withLinks;
        return this;
    }

    public WebPageFormatter withLinks() {
        this.withLinks = true;
        return this;
    }

    public WebPageFormatter withFields(boolean withFields) {
        this.withFields = withFields;
        return this;
    }

    public WebPageFormatter withFields() {
        this.withFields = true;
        return this;
    }

    public WebPageFormatter withEntities(boolean withEntities) {
        this.withEntities = withEntities;
        return this;
    }

    public WebPageFormatter withEntities() {
        this.withEntities = true;
        return this;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> fields = new LinkedHashMap<>();

    /* General */
        fields.put("key", page.getKey());
        fields.put("url", page.getUrl());
        fields.put("options", page.getOptions());
        fields.put("isSeed", page.isSeed());
        fields.put("createTime", format(page.getCreateTime()));
        fields.put("distance", page.getDistance());

    /* Fetch */
        fields.put("crawlStatus", page.getCrawlStatus().toString());
        fields.put("protocolStatus", page.getProtocolStatus().getName());
        fields.put("protocolStatusMessage", page.getProtocolStatus().toString());
        fields.put("contentLength", page.getContent().array().length);
        fields.put("fetchCount", page.getFetchCount());
        fields.put("fetchPriority", page.getFetchPriority());
        fields.put("fetchInterval", page.getFetchInterval().toString());
        fields.put("retriesSinceFetch", page.getFetchRetries());
        fields.put("prevFetchTime", format(page.getPrevFetchTime()));
        fields.put("fetchTime", format(page.getFetchTime()));
        fields.put("prevModifiedTime", format(page.getPrevModifiedTime()));
        fields.put("modifiedTime", format(page.getModifiedTime()));
        fields.put("baseUrl", page.getBaseUrl());
        fields.put("reprUrl", page.getReprUrl());
        fields.put("batchId", page.getBatchId());

    /* Parse */
        fields.put("parseStatus", page.getParseStatus().getName());
        fields.put("parseStatusMessage", page.getParseStatus().toString());
        fields.put("encoding", page.getEncoding());
        fields.put("prevSignature", page.getPrevSignatureAsString());
        fields.put("signature", page.getSignatureAsString());
        fields.put("pageCategory", page.getPageCategory().name());
        fields.put("prevContentPublishTime", format(page.getPrevContentPublishTime()));
        fields.put("contentPublishTime", format(page.getContentPublishTime()));
        fields.put("prevContentModifiedTime", format(page.getPrevContentModifiedTime()));
        fields.put("contentModifiedTime", format(page.getContentModifiedTime()));

        fields.put("prevRefContentPublishTime", format(page.getPrevRefContentPublishTime()));
        fields.put("refContentPublishTime", format(page.getRefContentPublishTime()));

        // May be too long
        // fields.put("inlinkAnchors", page.getInlinkAnchors());
        fields.put("pageTitle", page.getPageTitle());
        fields.put("contentTitle", page.getContentTitle());
        fields.put("inlinkAnchor", page.getAnchor());
        fields.put("title", page.sniffTitle());

    /* Score */
        fields.put("contentScore", String.valueOf(page.getContentScore()));
        fields.put("score", String.valueOf(page.getScore()));
        fields.put("cash", String.valueOf(page.getCash()));

        fields.put("marks", page.getMarks().asStringMap());
        fields.put("pageCounters", page.getPageCounters().asStringMap());
        fields.put("metadata", page.getMetadata().asStringMap());
        fields.put("headers", page.getHeaders().asStringMap());

        fields.put("linkCount", page.getLinks().size());
        fields.put("vividLinkCount", page.getVividLinks().size());
        fields.put("liveLinkCount", page.getLiveLinks().size());
        fields.put("deadLinkCount", page.getDeadLinks().size());
        fields.put("inlinkCount", page.getInlinks().size());

        fields.put("linksMessage", "Total "
                + page.getLinks().size() + " links, "
                + page.getVividLinks().size() + " vivid links, "
                + page.getLiveLinks().size() + " live links, "
                + page.getDeadLinks().size() + " dead links, "
                + page.getInlinks().size() + " inlinks");

        if (withLinks) {
            fields.put("links", page.getLinks().stream().map(Object::toString).collect(Collectors.toList()));
            fields.put("vividLinks", page.getVividLinks().values().stream().map(CharSequence::toString).collect(Collectors.toList()));
            fields.put("liveLinks", page.getLiveLinks().values().stream().map(l -> HypeLink.box(l).toString()).collect(Collectors.toList()));
            fields.put("deadLinks", page.getDeadLinks().stream().map(CharSequence::toString).collect(Collectors.toList()));
            fields.put("inlinks", page.getInlinks().entrySet().stream()
                    .map(il -> il.getKey() + "\t" + il.getValue()).collect(Collectors.joining("\n")));
        }

        if (withText) {
            fields.put("contentText", page.getContentText());
            fields.put("pageText", page.getPageText());
        }

        if (withContent && page.getContent() != null) {
            fields.put("content", page.getContentAsString());
        }

        if (withEntities) {
            List<Map<String, Object>> pageEntities = page.getPageModel().unbox().stream()
                    .map(fg -> new FieldGroupFormatter(fg).getFields())
                    .collect(Collectors.toList());
            fields.put("pageEntities", pageEntities);
        }

        return fields;
    }

    /**
     * TODO: Optimization
     */
    public Map<String, Object> toMap(Set<String> fields) {
        return toMap().entrySet().stream().filter(e -> fields.contains(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Nonnull
    public String format() {
        StringBuilder sb = new StringBuilder();

        sb.append("url:\t" + page.getUrl() + "\n")
                .append("baseUrl:\t" + page.getBaseUrl() + "\n")
                .append("batchId:\t" + page.getBatchId() + "\n")
                .append("crawlStatus:\t" + page.getCrawlStatus() + "\n")
                .append("protocolStatus:\t" + page.getProtocolStatus() + "\n")
                .append("depth:\t" + page.getDistance() + "\n")
                .append("pageCategory:\t" + page.getPageCategory() + "\n")
                .append("fetchCount:\t" + page.getFetchCount() + "\n")
                .append("fetchPriority:\t" + page.getFetchPriority() + "\n")
                .append("fetchInterval:\t" + page.getFetchInterval() + "\n")
                .append("retriesSinceFetch:\t" + page.getFetchRetries() + "\n");

        sb.append("\n")
                .append("options:\t" + page.getOptions() + "\n");

        sb.append("\n")
                .append("createTime:\t" + format(page.getCreateTime()) + "\n")
                .append("prevFetchTime:\t" + format(page.getPrevFetchTime()) + "\n")
                .append("fetchTime:\t" + format(page.getFetchTime()) + "\n")
                .append("prevModifiedTime:\t" + format(page.getPrevModifiedTime()) + "\n")
                .append("modifiedTime:\t" + format(page.getModifiedTime()) + "\n")
                .append("prevContentModifiedTime:\t" + format(page.getPrevContentModifiedTime()) + "\n")
                .append("contentModifiedTime:\t" + format(page.getContentModifiedTime()) + "\n")
                .append("prevContentPublishTime:\t" + format(page.getPrevContentPublishTime()) + "\n")
                .append("contentPublishTime:\t" + format(page.getContentPublishTime()) + "\n");

        sb.append("\n")
                .append("prevRefContentPublishTime:\t" + format(page.getPrevRefContentPublishTime()) + "\n")
                .append("refContentPublishTime:\t" + format(page.getRefContentPublishTime()) + "\n");

        sb.append("\n")
                .append("pageTitle:\t" + page.getPageTitle() + "\n")
                .append("contentTitle:\t" + page.getContentTitle() + "\n")
                .append("anchor:\t" + page.getAnchor() + "\n")
                .append("title:\t" + page.sniffTitle() + "\n");

        sb.append("\n")
                .append("parseStatus:\t" + page.getParseStatus().toString() + "\n")
                .append("prevSignature:\t" + page.getPrevSignatureAsString() + "\n")
                .append("signature:\t" + page.getSignatureAsString() + "\n")
                .append("contentScore:\t" + page.getContentScore() + "\n")
                .append("score:\t" + page.getScore() + "\n")
                .append("cash:\t" + page.getCash() + "\n");

        if (page.getReprUrl() != null) {
            sb.append("\n\n").append("reprUrl:\t" + page.getReprUrl() + "\n");
        }

        CrawlMarks crawlMarks = page.getMarks();
        if (!crawlMarks.unbox().isEmpty()) {
            sb.append("\n");
            crawlMarks.unbox().forEach((key, value) -> sb.append("mark " + key.toString() + ":\t" + value + "\n"));
        }

        if (!page.getPageCounters().unbox().isEmpty()) {
            sb.append("\n");
            page.getPageCounters().unbox().forEach((key, value) -> sb.append("counter " + key.toString() + " : " + value + "\n"));
        }

        Map<String, String> metadata = page.getMetadata().asStringMap();
        if (!metadata.isEmpty()) {
            sb.append("\n");
            metadata.entrySet().stream().filter(e -> !e.getValue().startsWith("meta_"))
                    .forEach(e -> sb.append("metadata " + e.getKey() + ":\t" + e.getValue() + "\n"));

            metadata.entrySet().stream().filter(e -> e.getValue().startsWith("meta_"))
                    .forEach(e -> sb.append("metadata " + e.getKey() + ":\t" + e.getValue() + "\n"));
        }

        Map<CharSequence, CharSequence> headers = page.getHeaders().unbox();
        if (headers != null && !headers.isEmpty()) {
            sb.append("\n");
            headers.forEach((key, value) -> sb.append("header " + key + ":\t" + value + "\n"));
        }

        sb.append("\n");
        sb.append("Total " + page.getLinks().size() + " links, ")
                .append(page.getVividLinks().size() + " vivid links, ")
                .append(page.getLiveLinks().size() + " live links, ")
                .append(page.getDeadLinks().size() + " dead links, ")
                .append(page.getInlinks().size() + " inlinks\n");

        if (withLinks) {
            sb.append("\n");
            sb.append("links:\n");
            page.getLinks().forEach(l -> sb.append("links:\t" + l + "\n"));
            sb.append("vividLinks:\n");
            page.getVividLinks().forEach((k, v) -> sb.append("liveLinks:\t" + k + "\t" + v + "\n"));
            sb.append("liveLinks:\n");
            page.getLiveLinks().values().forEach(e -> sb.append("liveLinks:\t" + e.getUrl() + "\t" + e.getAnchor() + "\n"));
            sb.append("deadLinks:\n");
            page.getDeadLinks().forEach(l -> sb.append("deadLinks:\t" + l + "\n"));
            sb.append("inlinks:\n");
            page.getInlinks().entrySet().forEach(e -> sb.append("inlink:\t" + e.getKey() + "\t" + e.getValue() + "\n"));
        }

        if (withContent) {
            ByteBuffer content = page.getContent();
            if (content != null) {
                sb.append("\n");
                sb.append("contentType:\t" + page.getContentType() + "\n")
                        .append("content:START>>>\n")
                        .append(Bytes.toString(content.array()))
                        .append("\n<<<END:content\n");
            }
        }

        if (withText) {
            if (page.getContentText().length() > 0) {
                sb.append("\n");
                sb.append("contentText:START>>>\n")
                        .append(page.getContentText())
                        .append("\n<<<END:contentText\n");
            }

            if (page.getPageText().length() > 0) {
                sb.append("pageText:START>>>\n")
                        .append(page.getPageText())
                        .append("\n<<<END:pageText\n");
            }
        }

        if (withEntities) {
            sb.append("\n")
                    .append("entityField:START>>>\n");
            page.getPageModel().unbox().stream()
                    .map(fg -> new FieldGroupFormatter(fg).getFields())
                    .flatMap(m -> m.entrySet().stream())
                    .forEach(e -> sb.append(e.getKey() + ": " + e.getValue()));
            sb.append("\n<<<END:pageText\n");
        }

        sb.append("\n");
        return sb.toString();
    }

    public Document createDocument() {
        Document doc = Document.createShell(page.getBaseUrl());

        doc.head().appendElement("title").appendText(page.getPageTitle());
        doc.body().appendElement("h1").appendText(page.getContentTitle());
        doc.body().appendElement("div")
                .attr("class", "content")
                .append(page.getContentText());
        createLinksElement(doc.body());

        return doc;
    }

    public void createLinksElement(Element parent) {
        Element links = parent.appendElement("div")
                .attr("class", "links")
                .appendElement("ul");

        int i = 0;
        for (GHypeLink l : page.getLiveLinks().values()) {
            ++i;

            String text = StringUtils.isBlank(l.getAnchor()) ? String.valueOf(i) : l.getAnchor().toString();
            links.appendElement("li")
                    .appendElement("a").attr("href", l.getUrl().toString()).appendText(text);
        }
    }

    public String toJson() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(toMap());
    }

    @Nonnull
    private String format(@Nonnull Instant instant) {
        return LocalDateTime.ofInstant(instant, zoneId).toString();
    }

    @Override
    public String toString() {
        return format();
    }
}
