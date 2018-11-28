package fun.platonic.pulsar.common;

import fun.platonic.pulsar.common.config.ImmutableConfig;
import fun.platonic.pulsar.common.config.Params;
import fun.platonic.pulsar.persist.*;
import fun.platonic.pulsar.persist.gora.db.WebDb;
import fun.platonic.pulsar.persist.metadata.PageCategory;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static fun.platonic.pulsar.common.config.CapabilityTypes.PULSAR_JOB_NAME;
import static fun.platonic.pulsar.common.config.CapabilityTypes.PULSAR_REPORT_DIR;

/**
 * Created by vincent on 16-10-12.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 * <p>
 * A very simple metrics system
 * <p>
 * TODO: Use com.codahale.metrics.MetricRegistry or Spark Metrics System
 */
public class MetricsSystem implements AutoCloseable {

    public static final Logger LOG = LoggerFactory.getLogger(MetricsSystem.class);
    public static final Logger REPORT_LOG = MetricsReporter.LOG_NON_ADDITIVITY;
    private final DecimalFormat df = new DecimalFormat("0.0");
    private ImmutableConfig conf;
    private Path reportDir;
    private String dayOfWeek;
    private String hostname;
    private String jobName;
    private String reportSuffix;
    private WebDb webDb;
    private WeakPageIndexer weakIndexer;
    private String urlPrefix;
    private int reportCount = 0;
    // We need predictable iteration order, LinkedHashSet is all right
    private Set<CharSequence> metricsPageUrls = new LinkedHashSet<>();
    private Map<String, WebPage> metricsPages = new HashMap<>();
    public MetricsSystem(WebDb webDb, ImmutableConfig conf) {
        this.conf = conf;
        this.hostname = NetUtil.getHostname();
        this.jobName = conf.get(PULSAR_JOB_NAME, "job-unknown-" + DateTimeUtil.now("MMdd.HHmm"));
        this.urlPrefix = PulsarConstants.CRAWL_LOG_INDEX_URL + "/" + DateTimeUtil.now("yyyy/MM/dd") + "/" + jobName + "/" + hostname;
        this.reportSuffix = jobName;
        this.dayOfWeek = String.valueOf(LocalDate.now().getDayOfWeek().getValue());

        try {
            reportDir = conf.getPath(PULSAR_REPORT_DIR, Paths.get(PulsarConstants.PATH_PULSAR_REPORT_DIR));
            reportDir = Paths.get(reportDir.toAbsolutePath().toString(), DateTimeUtil.format(System.currentTimeMillis(), "yyyyMMdd"));
            Files.createDirectories(reportDir);
        } catch (IOException e) {
            LOG.error(e.toString());
        }

        this.webDb = webDb;
        this.weakIndexer = new WeakPageIndexer(PulsarConstants.CRAWL_LOG_HOME_URL, webDb);
    }

    public WebDb getWebDb() {
        return webDb;
    }

    public void commit() {
        // TODO : save only if dirty
        metricsPages.forEach((key, value) -> webDb.put(key, value));
        webDb.flush();
    }

    @Override
    public void close() throws Exception {
        commit();

        if (!metricsPageUrls.isEmpty()) {
            weakIndexer.indexAll(metricsPageUrls);
            weakIndexer.commit();
        }
    }

    public void report(WebPage page) {
        String category = page.getPageCategory().name().toLowerCase();
        if (page.isSeed()) {
            category = "seed";
        }
        String fileSuffix = "urls-" + category + ".txt";
        report(fileSuffix, page);
    }

    /**
     * TODO : use WeakIndexer
     */
    public void report(String reportGroup, WebPage page) {
        String metricsPageUrl = urlPrefix + "/" + reportGroup;
        WebPage metricsPage = getOrCreateMetricsPage(metricsPageUrl);
        metricsPage.addLiveLink(new HypeLink(page.getUrl()));
        metricsPage.setContent(metricsPage.getContentAsString() + new PageReport(page) + "\n");

        metricsPageUrls.add(metricsPageUrl);
        metricsPages.put(metricsPageUrl, metricsPage);

        if (++reportCount > 40) {
            commit();
            reportCount = 0;
        }
    }

    public Path cache(WebPage page) {
        Objects.requireNonNull(page);

        Path path = Paths.get(PulsarConstants.PATH_PULSAR_CACHE_DIR, dayOfWeek, DigestUtils.md5Hex(page.getUrl()));

        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path.getParent());
            }

            Objects.requireNonNull(page.getContent());
            Files.write(path, page.getContent().array(), StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return path;
    }

    private WebPage getOrCreateMetricsPage(String url) {
        WebPage metricsPage = metricsPages.get(url);
        if (metricsPage == null) {
            metricsPage = WebPage.newInternalPage(url, "Pulsar Metrics Page");
            metricsPage.setContent("");
            metricsPage.getMetadata().set("JobName",
                    conf.get(PULSAR_JOB_NAME, "job-unknown-" + DateTimeUtil.now("MMdd.HHmm")));
        }

        return metricsPage;
    }

    public String getPageReport(WebPage page) {
        return new PageReport(page).toString();
    }

    public String getPageReport(WebPage page, boolean verbose) {
        PageReport pageReport = new PageReport(page);
        String report = pageReport.toString();
        if (verbose) {
            report = page.getReferrer() + " -> " + page.getUrl() + "\n" + report;
            report += "\n" + page + "\n\n\n";
        }

        return report;
    }

    public void reportRedirects(String redirectString) {
        // writeReport(redirectString, "fetch-redirects-" + reportSuffix + ".txt");
    }

    public void reportPageFromSeedersist(String report) {
        // String reportString = seedUrl + " -> " + url + "\n";
        writeReport(report + "\n", "fetch-urls-from-seed-persist-" + reportSuffix + ".txt");
    }

    public void reportPageFromSeed(String report) {
        // String reportString = seedUrl + " -> " + url + "\n";
        writeReport(report + "\n", "fetch-urls-from-seed-" + reportSuffix + ".txt");
    }

    public void reportFetchTimeHistory(String fetchTimeHistory) {
        // writeReport(fetchTimeHistory, "fetch-time-history-" + reportSuffix + ".txt");
    }

    public void debugFetchHistory(WebPage page, CrawlStatus crawlStatus) {
        // Debug fetch time history
        String fetchTimeHistory = page.getFetchTimeHistory("");
        if (fetchTimeHistory.contains(",")) {
            String report = String.format("%60s", page.getUrl())
                    + "\tfetchTimeHistory : " + fetchTimeHistory
                    + "\tcrawlStatus : " + page.getCrawlStatus()
                    + "\n";
            reportFetchTimeHistory(report);
        }
    }

    public void reportGeneratedHosts(Set<String> hostNames) {
        String report = "# Total " + hostNames.size() + " hosts generated : \n"
                + hostNames.stream().map(UrlUtil::reverseHost).sorted().map(UrlUtil::unreverseHost)
                .map(host -> String.format("%40s", host))
                .collect(Collectors.joining("\n"));

        writeReport(report, "generate-hosts-" + reportSuffix + ".txt", true);
    }

    public void debugSortScore(WebPage page) {
        String report = page.getSortScore() + "\t\t" + page.getUrl() + "\n";
        writeReport(report, "generate-sort-score-" + reportSuffix + ".txt");
    }

    public void debugFetchLaterSeeds(WebPage page) {
        PageReport pageReport = new PageReport(page);
        writeReport(pageReport.toString() + "\n", "seeds-fetch-later-" + reportSuffix + ".txt");
    }

    public void debugDepthUpdated(String report) {
        writeReport(report + "\n", "depth-updated-" + reportSuffix + ".txt");
    }

    public void reportFlawyParsedPage(WebPage page, boolean verbose) {
        String report = getFlawyParsedPageReport(page, verbose);
        writeReport(report + "\n", "parse-flaw-" + reportSuffix + ".txt");
    }

    public String getFlawyParsedPageReport(WebPage page, boolean verbose) {
        PageCounters pageCounters = page.getPageCounters();
        ParseStatus parseStatus = page.getParseStatus();
        Params params = Params.of(
                "parseStatus", parseStatus.toString(),
                "parseErr", pageCounters.get(PageCounters.Self.parseErr),
                "extractErr", pageCounters.get(PageCounters.Self.extractErr),
                "U", StringUtils.substring(page.getUrl(), 0, 80)
        ).withKVDelimiter(":");

        String report = params.formatAsLine();
        if (verbose) {
            report = page.getReferrer() + " -> " + page.getUrl() + "\n" + report;
            report += "\n" + page + "\n\n\n";
        }

        return report;
    }

    public void reportBrokenEntity(String url, String message) {
        writeReport(LocalDateTime.now() + "\t" + message + "\t" + url + "\n", "broken-entity-" + reportSuffix + ".txt");
    }

    public void reportPerformance(String url, String elapsed) {
        writeReport(elapsed + " -> url" + "\n", "performance-" + reportSuffix + ".txt");
    }

    public void debugLongUrls(String report) {
        writeReport(report + "\n", "urls-long-" + reportSuffix + ".txt");
    }

    public void debugIndexDocTime(String timeStrings) {
        writeReport(timeStrings + "\n", "index-doc-time-" + reportSuffix + ".txt");
    }

    public void reportFetchSchedule(WebPage page, boolean verbose) {
        String report = getPageReport(page, verbose);
        String prefix = verbose ? "verbose-" : "";
        writeReport(report + "\n", prefix + "fetch-schedule-" + page.getPageCategory().name().toLowerCase() + "-" + reportSuffix + ".txt");
    }

    public void reportForceRefetchSeeds(String report) {
        writeReport(report + "\n", "force-refetch-urls-" + reportSuffix + ".txt");
    }

    public void reportBadModifiedTime(String report) {
        writeReport(report + "\n", "bad-modified-urls-" + reportSuffix + ".txt");
    }

    public void writeReport(String report, String fileSuffix) {
        writeReport(report, fileSuffix, false);
    }

    public void writeReport(String report, String fileSuffix, boolean printPath) {
        Path reportFile = Paths.get(reportDir.toAbsolutePath().toString(), fileSuffix);
        writeReport(reportFile, report, printPath);
    }

    public synchronized void writeReport(Path reportFile, String report, boolean printPath) {
        try {
            if (!Files.exists(reportFile)) {
                Files.createDirectories(reportFile.getParent());
                Files.createFile(reportFile);
            }

            BufferedWriter writer = Files.newBufferedWriter(reportFile, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
            writer.write(report);

            writer.flush();
        } catch (IOException e) {
            LOG.error("Failed to write report : " + e.toString());
        }

        if (printPath) {
            LOG.info("Report written to " + reportFile.toAbsolutePath());
        }
    }

    private class PageReport {
        private Instant prevFetchTime;
        private Instant fetchTime;
        private Duration fetchInterval;
        private int distance;
        private int fetchCount;
        private Instant contentPublishTime;
        private Instant refContentPublishTime;
        private PageCategory pageCategory;
        private int refArticles;
        private int refChars;
        private double contentScore;
        private double score;
        private double cash;
        private String url;

        public PageReport(WebPage page) {
            this.prevFetchTime = page.getPrevFetchTime();
            this.fetchTime = page.getFetchTime();
            this.fetchInterval = page.getFetchInterval();
            this.distance = page.getDistance();
            this.fetchCount = page.getFetchCount();
            this.contentPublishTime = page.getContentPublishTime();
            this.refContentPublishTime = page.getRefContentPublishTime();
            this.pageCategory = page.getPageCategory();
            this.refArticles = page.getPageCounters().get(PageCounters.Ref.article);
            this.refChars = page.getPageCounters().get(PageCounters.Ref.ch);
            this.contentScore = page.getContentScore();
            this.score = page.getScore();
            this.cash = page.getCash();
            this.url = page.getUrl();
        }

        @Override
        public String toString() {
            final String pattern = "yyyy-MM-dd HH:mm:ss";
            String fetchTimeString =
                    DateTimeUtil.format(prevFetchTime, pattern) + "->" + DateTimeUtil.format(fetchTime, pattern)
                            + "," + DurationFormatUtils.formatDuration(fetchInterval.toMillis(), "DdTH:mm:ss");

            Params params = Params.of(
                    "T", fetchTimeString,
                    "DC", distance + "," + fetchCount,
                    "PT", DateTimeUtil.isoInstantFormat(contentPublishTime.truncatedTo(ChronoUnit.SECONDS))
                            + "," + DateTimeUtil.isoInstantFormat(refContentPublishTime.truncatedTo(ChronoUnit.SECONDS)),
                    "C", refArticles + "," + refChars,
                    "S", df.format(contentScore) + "," + df.format(score) + "," + df.format(cash),
                    "U", StringUtils.substring(url, 0, 80)
            ).withKVDelimiter(":");

            return params.formatAsLine();
        }
    }
}
