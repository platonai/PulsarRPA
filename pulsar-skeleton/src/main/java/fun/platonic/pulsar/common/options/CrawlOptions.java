package fun.platonic.pulsar.common.options;

import com.beust.jcommander.Parameter;
import fun.platonic.pulsar.common.config.ImmutableConfig;
import fun.platonic.pulsar.common.config.Params;
import fun.platonic.pulsar.common.config.PulsarConstants;
import fun.platonic.pulsar.common.options.converters.DurationConverter;
import fun.platonic.pulsar.common.options.converters.WeightedKeywordsConverter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static fun.platonic.pulsar.common.config.CapabilityTypes.*;

/**
 * Created by vincent on 17-3-18.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
public class CrawlOptions extends CommonOptions {

    public static final CrawlOptions DEFAULT = new CrawlOptions();

    @Parameter(names = {"-log", "-verbose"}, description = "Log level for this crawl task")
    private int verbose = 0;
    @Parameter(names = {"-i", "--fetch-interval"}, converter = DurationConverter.class, description = "Fetch interval")
    private Duration fetchInterval = Duration.ofHours(1);
    @Parameter(names = {"-p", "--fetch-priority"}, description = "Fetch priority")
    private int fetchPriority = PulsarConstants.FETCH_PRIORITY_DEFAULT;
    @Parameter(names = {"-s", "--score"}, description = "Injected score")
    private int score = 0;
    @Parameter(names = {"-d", "--depth"}, description = "Max crawl depth. Do not crawl anything deeper")
    private int depth = 1;
    @Parameter(names = {"-z", "--zone-id"}, description = "The zone id of the website we crawl")
    private String zoneId = ZoneId.systemDefault().getId();

    @Parameter(names = {"-w", "--keywords"}, converter = WeightedKeywordsConverter.class, description = "Keywords with weight, ")
    private Map<String, Double> keywords = new HashMap<>();

    @Parameter(names = {"-idx", "--indexer-url"}, description = "Indexer url")
    private String indexerUrl;

    private LinkOptions linkOptions = new LinkOptions();

    public CrawlOptions() {
        super();
        addObjects(this, linkOptions);
    }

    public CrawlOptions(String args) {
        super(Objects.requireNonNull(args).replaceAll("=", " "));
        addObjects(this, linkOptions);
    }

    public CrawlOptions(String args, ImmutableConfig conf) {
        super(Objects.requireNonNull(args).replaceAll("=", " "));
        this.init(conf);
        addObjects(this, linkOptions);
    }

    public CrawlOptions(String[] argv) {
        super(Objects.requireNonNull(argv));
        addObjects(this, linkOptions);
    }

    public CrawlOptions(String[] argv, ImmutableConfig conf) {
        super(Objects.requireNonNull(argv));
        this.init(conf);
        addObjects(this, linkOptions);
    }

    public CrawlOptions(Map<String, String> argv) {
        super(Objects.requireNonNull(argv));
        addObjects(this, linkOptions);
    }

    public CrawlOptions(Map<String, String> argv, ImmutableConfig conf) {
        super(Objects.requireNonNull(argv));
        this.init(conf);
        addObjects(this, linkOptions);
    }

    @Nonnull
    public static CrawlOptions parse(String args, ImmutableConfig conf) {
        Objects.requireNonNull(args);
        Objects.requireNonNull(conf);

        if (StringUtils.isBlank(args)) {
            return new CrawlOptions(new String[0], conf);
        }

        CrawlOptions options = new CrawlOptions(args, conf);
        options.parse();

        return options;
    }

    private void init(ImmutableConfig conf) {
        this.fetchInterval = conf.getDuration(FETCH_INTERVAL, fetchInterval);
        this.score = conf.getInt(INJECT_SCORE, score);
        this.depth = conf.getUint(CRAWL_MAX_DISTANCE, depth);
        this.linkOptions = new LinkOptions("", conf);
    }

    public int getVerbose() {
        return verbose;
    }

    public Duration getFetchInterval() {
        return fetchInterval;
    }

    public int getFetchPriority() {
        return fetchPriority;
    }

    public int getScore() {
        return score;
    }

    public int getDepth() {
        return depth;
    }

    public String getZoneId() {
        return zoneId;
    }

    public Map<String, Double> getKeywords() {
        return keywords;
    }

    public String getIndexerUrl() {
        return indexerUrl;
    }

    public LinkOptions getLinkOptions() {
        return linkOptions;
    }

    public String formatKeywords() {
        final DecimalFormat df = new DecimalFormat("##.#");
        return keywords.entrySet()
                .stream().map(e -> e.getKey() + "^" + df.format(e.getValue())).collect(Collectors.joining(","));
    }

    @Override
    public Params getParams() {
        return Params.of(
                "-log", verbose,
                "-i", fetchInterval,
                "-p", fetchPriority,
                "-s", score,
                "-d", depth,
                "-z", zoneId,
                "-w", formatKeywords(),
                "-idx", indexerUrl
        )
                .filter(p -> StringUtils.isNotEmpty(p.getValue().toString()))
                .merge(linkOptions.getParams())
                ;
    }

    @Override
    public String toString() {
        return getParams().withKVDelimiter(" ").formatAsLine().replaceAll("\\s+", " ");
    }
}
