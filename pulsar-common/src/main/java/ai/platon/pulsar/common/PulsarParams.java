package ai.platon.pulsar.common;

import ai.platon.pulsar.common.config.Params;

import java.util.Map;

/**
 * Created by vincent on 16-9-24.
 *
 * @author vincent
 * @version $Id: $Id
 */
public class PulsarParams extends Params {

    /**
     * Crawl id to use.
     */
    public static final String ARG_CRAWL_ID = "-crawlId";
    /**
     * Batch id to select.
     */
    public static final String ARG_BATCH_ID = "-batchId";
    /**
     * Batch id to select.
     */
    public static final String ARG_SEEDS = "-seeds";
    /**
     * Re-generate.
     */
    public static final String ARG_REGENERATE = "-reGen";
    /**
     * Re-generate seeds.
     */
    public static final String ARG_REGENERATE_SEEDS = "-reGenSeeds";
    /**
     * Resume previously aborted op.
     */
    public static final String ARG_RESUME = "-resume";
    /**
     * Parse page after it's fetched
     */
    public static final String ARG_PARSE = "-parse";
    /**
     * Force processing even if there are locks or inconsistencies.
     */
    public static final String ARG_FORCE = "-force";
    /**
     * Sort statistics.
     */
    public static final String ARG_SORT = "-sort";
    /**
     * Number of fetcher threads (per map task).
     */
    public static final String ARG_THREADS = "-threads";
    /**
     * Number of fetcher threads per queue.
     */
    public static final String ARG_POOL_THREADS = "-queueThreads";
    /**
     * Number of crawl rounds.
     */
    public static final String ARG_ROUND = "-round";
    /**
     * Number of privacy contexts.
     */
    public static final String ARG_PRIVACY_CONTEXTS = "-privacyContexts";
    /**
     * Number of max open tabs in each browser instance.
     */
    public static final String ARG_BROWSER_MAX_TABS = "-maxTabs";
    /**
     * Number of fetcher tasks.
     */
    public static final String ARG_REDUCER_TASKS = "-reducerTasks";
    /**
     * The notion of current time.
     */
    public static final String ARG_CURTIME = "-curTime";
    /**
     * Apply URLFilters.
     */
    public static final String ARG_NO_FILTER = "-noFilter";
    /**
     * Apply URLNormalizers.
     */
    public static final String ARG_NO_NORMALIZER = "-noNorm";
    /**
     * Add days.
     */
    public static final String ARG_ADDDAYS = "-adddays";
    /**
     * Class to run.
     */
    public static final String ARG_CLASS = "-class";
    /**
     * Depth (number of cycles) of a crawl.
     */
    public static final String ARG_DEPTH = "-depth";
    /** Constant <code>ARG_START_KEY="-startKey"</code> */
    public static final String ARG_START_KEY = "-startKey";
    /** Constant <code>ARG_END_KEY="-endKey"</code> */
    public static final String ARG_END_KEY = "-endKey";
    /** Constant <code>ARG_LIMIT="-limit"</code> */
    public static final String ARG_LIMIT = "-limit";
    /** Constant <code>ARG_VERBOSE="-verbose"</code> */
    public static final String ARG_VERBOSE = "-verbose";
    /**
     * Injector Relative
     */
    public static final String ARG_SEED_PATH = "-seedDir";
    /**
     * \n-separated list of seed URLs.
     */
    public static final String ARG_SEED_URLS = "-seedUrls";
    /**
     * Generate Relative
     */
    public static final String ARG_PRIORITY = "-priority";
    /** Constant <code>ARG_SCORE="-score"</code> */
    public static final String ARG_SCORE = "-score";
    /**
     * Fetcher Relative
     */
  /* Fetch interval. */
    public static final String ARG_FETCH_INTERVAL = "-fetchInterval";
    /* Fetch mode. */
    /** Constant <code>ARG_FETCH_MODE="-fetchMode"</code> */
    public static final String ARG_FETCH_MODE = "-fetchMode";
    /**
     * Generator Relative
     */
  /* Generate topN scoring URLs. */
    public static final String ARG_TOPN = "-topN";
    /**
     * Fetch
     */
    public static final String ARG_STRICT_DF = "-strictDf";
    /**
     * Reparse
     */
    public static final String ARG_REPARSE = "-reparse";
    /**
     * Index immediately once the content is fetched.
     */
    public static final String ARG_INDEX = "-index";
    /**
     * Index immediately once the content is fetched.
     */
    public static final String ARG_REINDEX = "-reindex";
    /**
     * Update immediately once the content is fetched.
     */
    public static final String ARG_UPDATE = "-update";
    /**
     * Indexer.
     */
    public static final String ARG_INDEXER = "-indexer";
    /**
     * Indexer.
     */
    public static final String ARG_INDEXER_URL = "-indexerUrl";
    /**
     * ZooKeeper.
     */
    public static final String ARG_ZK = "-zk";
    /**
     * Solr Collection.
     */
    public static final String ARG_COLLECTION = "-collection";
    /**
     * Index document fields
     */
    public static final String DOC_FIELD_PUBLISH_TIME = "publish_time";
    public static final String DOC_FIELD_MODIFIED_TIME = "modified_time";
    public static final String DOC_FIELD_ARTICLE_TILE = "article_title";
    public static final String DOC_FIELD_PAGE_TITLE = "page_title";
    public static final String DOC_FIELD_CONTENT_TILE = "article_title";
    public static final String DOC_FIELD_PAGE_CATEGORY = "page_category";
    public static final String DOC_FIELD_LINKS_COUNT = "links_count";
    public static final String DOC_FIELD_HTML_CONTENT = "html_content";
    public static final String DOC_FIELD_TEXT_CONTENT = "text_content";
    public static final String DOC_FIELD_TEXT_CONTENT_LENGTH = "text_content_length";
    public static final String DOC_FIELD_HTML_CONTENT_LENGTH = "html_content_length";
    /**
     * Temporary variable holders
     */
    public static final String VAR_LINKS_COUNT = "links_count";
    public static final String VAR_DROPPED_LINKS_COUNT = "dropped_links_count";
    public static final String VAR_PAGE_EXISTENCE = "page_existence";
    public static final String VAR_PAGE_CONTENT_LENGTH = "page_content_length";
    public static final String VAR_GENERATE_FORCE_FIRST = "generate_force_first";
    public static final String VAR_FETCH_REASON = "fetch_reason";
    public static final String VAR_PREV_FETCH_TIME_BEFORE_UPDATE = "prev_fetch_time_before_update";
    public static final String VAR_PRIVACY_CONTEXT_NAME = "privacy_context_name";

    /**
     * If this task is a scrape task
     * TODO: this is a temporary solution
     * */
    public static final String VAR_IS_SCRAPE = "IS_SCRAPE";

    public static final String VAR_LOAD_OPTIONS = "LOAD_OPTIONS";

    /**
     * <p>Constructor for PulsarParams.</p>
     */
    public PulsarParams() {
    }

    /**
     * <p>Constructor for PulsarParams.</p>
     *
     * @param key a {@link java.lang.String} object.
     * @param value a {@link java.lang.Object} object.
     * @param others a {@link java.lang.Object} object.
     */
    public PulsarParams(String key, Object value, Object... others) {
        super(key, value, others);
    }

    /**
     * <p>Constructor for PulsarParams.</p>
     *
     * @param args a {@link java.util.Map} object.
     */
    public PulsarParams(Map<String, Object> args) {
        super(args);
    }
}
