package ai.platon.pulsar.persist;

import ai.platon.pulsar.common.DateTimes;
import ai.platon.pulsar.persist.metadata.Name;
import org.apache.avro.util.Utf8;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static ai.platon.pulsar.common.config.AppConstants.MAX_LINK_PER_PAGE;

public class WebPageExt {
    private final WebPage page;

    public WebPageExt(WebPage page) {
        this.page = page;
    }

    @NotNull
    public String sniffTitle() {
        String title = page.getContentTitle();
        if (title.isEmpty()) {
            title = page.getAnchor().toString();
        }
        if (title.isEmpty()) {
            title = page.getPageTitle();
        }
        if (title.isEmpty()) {
            title = page.getLocation();
        }
        if (title.isEmpty()) {
            title = page.getUrl();
        }
        return title;
    }

    public void setTextCascaded(String text) {
        page.setContent(text);
        page.setContentText(text);
        page.setPageText(text);
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
            Utf8 url = page.u8(l.getUrl());
            if (!links.contains(url)) {
                links.add(url);
            }
        }

        page.setLinks(links);
        page.setImpreciseLinkCount(links.size());
    }

    public void addLinks(Iterable<CharSequence> hypeLinks) {
        List<CharSequence> links = page.getLinks();

        // If there are too many links, Drop the front 1/3 links
        if (links.size() > MAX_LINK_PER_PAGE) {
            links = links.subList(links.size() - MAX_LINK_PER_PAGE / 3, links.size());
        }

        for (CharSequence link : hypeLinks) {
            Utf8 url = page.u8(link.toString());
            // Use a set?
            if (!links.contains(url)) {
                links.add(url);
            }
        }

        page.setLinks(links);
        page.setImpreciseLinkCount(links.size());
    }

    public boolean updateContentPublishTime(Instant newPublishTime) {
        if (!page.isValidContentModifyTime(newPublishTime)) {
            return false;
        }

        Instant lastPublishTime = page.getContentPublishTime();
        if (newPublishTime.isAfter(lastPublishTime)) {
            page.setPrevContentPublishTime(lastPublishTime);
            page.setContentPublishTime(newPublishTime);
        }

        return true;
    }

    public boolean updateContentModifiedTime(Instant newModifiedTime) {
        if (!page.isValidContentModifyTime(newModifiedTime)) {
            return false;
        }

        Instant lastModifyTime = page.getContentModifiedTime();
        if (newModifiedTime.isAfter(lastModifyTime)) {
            page.setPrevContentModifiedTime(lastModifyTime);
            page.setContentModifiedTime(newModifiedTime);
        }

        return true;
    }

    public boolean updateRefContentPublishTime(Instant newRefPublishTime) {
        if (!page.isValidContentModifyTime(newRefPublishTime)) {
            return false;
        }

        Instant latestRefPublishTime = page.getRefContentPublishTime();

        if (newRefPublishTime.isAfter(latestRefPublishTime)) {
            page.setPrevRefContentPublishTime(latestRefPublishTime);
            page.setRefContentPublishTime(newRefPublishTime);

            return true;
        }

        return false;
    }

    public Instant getFirstIndexTime(Instant defaultValue) {
        Instant firstIndexTime = null;

        String indexTimeHistory = page.getIndexTimeHistory("");
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
     * *****************************************************************************
     * Parsing
     * ******************************************************************************
     */
    public void updateFetchTimeHistory(@NotNull Instant fetchTime) {
        String fetchTimeHistory = page.getMetadata().get(Name.FETCH_TIME_HISTORY);
        fetchTimeHistory = DateTimes.constructTimeHistory(fetchTimeHistory, fetchTime, 10);
        page.getMetadata().set(Name.FETCH_TIME_HISTORY, fetchTimeHistory);
    }

    public void updateFetchTime(Instant prevFetchTime, Instant fetchTime) {
        page.setPrevFetchTime(prevFetchTime);
        // the next time supposed to fetch
        page.setFetchTime(fetchTime);
        updateFetchTimeHistory(fetchTime);
    }

    /**
     * Get the first fetch time
     */
    public @Nullable Instant getFirstFetchTime() {
        Instant firstFetchTime = null;

        String history = page.getFetchTimeHistory("");
        if (!history.isEmpty()) {
            String[] times = history.split(",");
            Instant time = DateTimes.parseInstant(times[0], Instant.EPOCH);
            if (time.isAfter(Instant.EPOCH)) {
                firstFetchTime = time;
            }
        }

        return firstFetchTime;
    }

    @NotNull
    public Instant sniffModifiedTime() {
        Instant modifiedTime = page.getModifiedTime();
        Instant headerModifiedTime = page.getHeaders().getLastModified();
        Instant contentModifiedTime = page.getContentModifiedTime();

        if (page.isValidContentModifyTime(headerModifiedTime) && headerModifiedTime.isAfter(modifiedTime)) {
            modifiedTime = headerModifiedTime;
        }

        if (page.isValidContentModifyTime(contentModifiedTime) && contentModifiedTime.isAfter(modifiedTime)) {
            modifiedTime = contentModifiedTime;
        }

        Instant contentPublishTime = page.getContentPublishTime();
        if (page.isValidContentModifyTime(contentPublishTime) && contentPublishTime.isAfter(modifiedTime)) {
            modifiedTime = contentPublishTime;
        }

        // A fix
        if (modifiedTime.isAfter(Instant.now().plus(1, ChronoUnit.DAYS))) {
            // LOG.warn("Invalid modified time " + DateTimeUtil.isoInstantFormat(modifiedTime) + ", url : " + page.url());
            modifiedTime = Instant.now();
        }

        return modifiedTime;
    }
}
