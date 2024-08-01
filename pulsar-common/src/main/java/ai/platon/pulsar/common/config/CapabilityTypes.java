package ai.platon.pulsar.common.config;

/**
 * Created by vincent on 17-1-17.
 * Copyright @ 2013-2023 Platon AI. All rights reserved
 *
 * @author vincent
 * @version $Id: $Id
 */
public interface CapabilityTypes {

    /**
     * Common Parameters
     */

    String APP_ID_KEY = "app.ident";
    String APP_NAME_KEY = "app.name";

    String APP_TMP_DIR_KEY = "app.tmp.dir";

    String LEGACY_CONFIG_PROFILE = "legacy.config.profile";

    /**
     * @deprecated legacy configurations are deprecated
     * */
    String SYSTEM_PROPERTY_SPECIFIED_RESOURCES = "system.property.specified.resources";

    String DRY_RUN = "pulsar.dry.run";

    /**
     * Main loop
     * */
    String MAIN_LOOP_CONCURRENCY_OVERRIDE = "main.loop.concurrency.override";

    /**
     * Args
     */
    String TENANT_ID = "tenantId";

    String START_KEY = "startKey";

    String END_KEY = "endKey";

    String START = "start";

    String LIMIT = "limit";

    String CRAWL_MAX_DISTANCE = "crawl.max.distance";

    String BATCH_ID = "pulsar.batch.name";

    String PARAM_JOB_NAME = "pulsar.job.name";

    String MAPREDUCE_JOB_REDUCES = "mapreduce.job.reduces";

    String FORCE = "force";

    String RECENT_DAYS_WINDOW = "recent.days.window";

    /**
     * Application metrics
     * */
    String METRICS_ENABLED = "metrics.enabled";

    /**
     * Distribution
     */
    String PULSAR_MASTER_HOST = "pulsar.master.host";

    String PULSAR_MASTER_PORT = "pulsar.master.port";

    ///////////////////////////////////////////////////////////////////////////
    // Crawl section

    /**
     * The class name of the page event handler.
     * <p>
     * <code>System.setProperty(CapabilityTypes.PAGE_EVENT_CLASS, "ai.platon.pulsar.skeleton.crawl.event.impl.DefaultPageEvent")</code>
     * */
    String PAGE_EVENT_CLASS = "page.event.class";

    ///////////////////////////////////////////////////////////////////////////
    // Storage

    String STORAGE_CRAWL_ID = "storage.crawl.id";

    String STORAGE_SCHEMA_WEBPAGE = "storage.schema.webpage";

    String STORAGE_PREFERRED_SCHEMA_NAME = "preferred.schema.name";

    String STORAGE_DATA_STORE_CLASS = "storage.data.store.class";

    String STORAGE_DATUM_EXPIRES = "storage.datum.expires";

    ///////////////////////////////////////////////////////////////////////////
    // Spring

    String APPLICATION_CONTEXT_CONFIG_LOCATION = "application.context.config.location";

    ///////////////////////////////////////////////////////////////////////////
    // Inject phrase

    /**
     * Inject parameters
     */
    String INJECT_SCORE = "inject.score";

    String GLOBAL_CACHE_CLASS = "global.cache.class";


    ///////////////////////////////////////////////////////////////////////////
    // Load phrase

    /**
     * Load parameters
     */
    String LOAD_STRATEGY = "load.strategy";
    /**
     * Deactivate the fetch component, ensuring that all pages are loaded exclusively from storage
     * and never fetched from the Internet.
     * <p>
     * If a page is not found in the local storage, return WebPage.NIL.
     * */
    String LOAD_DEACTIVATE_FETCH_COMPONENT = "load.deactivate.fetch.component";
    /**
     * @deprecated use {@link #LOAD_DEACTIVATE_FETCH_COMPONENT} instead
     * */
    @Deprecated
    String LOAD_DISABLE_FETCH = "load.disable.fetch";

    ///////////////////////////////////////////////////////////////////////////
    // Fetch phrase

    /**
     * Fetch parameters
     */
    String FETCH_MODE = "fetch.fetch.mode";

    String FETCH_CONCURRENCY = "fetch.concurrency";

    String FETCH_JOB_TIMEOUT = "fetch.job.timeout";

    String FETCH_TASK_TIMEOUT = "fetch.task.timeout";

    String FETCH_PENDING_TIMEOUT = "fetch.pending.timeout";

    String FETCH_MAX_HOST_FAILURES = "fetch.max.host.failures";

    String FETCH_QUEUE_MODE = "fetch.queue.mode";

    String FETCH_QUEUE_RETUNE_INTERVAL = "fetch.pending.queue.check.time";

    String FETCH_FEEDER_INIT_BATCH_SIZE = "fetch.feeder.init.batch.size";

    String FETCH_THREADS_PER_POOL = "fetch.threads.per.pool";

    String FETCH_THROUGHPUT_PAGES_PER_SECOND = "fetch.throughput.threshold.pages";

    String FETCH_THROUGHPUT_THRESHOLD_SEQENCE = "fetch.throughput.threshold.sequence";

    String FETCH_THROUGHPUT_CHECK_INTERVAL = "fetch.throughput.check.interval";

    String FETCH_CHECK_INTERVAL = "fetch.check.interval";

    String FETCH_QUEUE_DELAY = "fetch.queue.delay";

    String FETCH_QUEUE_MIN_DELAY = "fetch.queue.min.delay";

    String FETCH_MIN_INTERVAL = "fetch.interval.min";

    String FETCH_MAX_INTERVAL = "fetch.interval.max";

    String FETCH_INTERVAL = "fetch.fetch.interval";

    String FETCH_DEFAULT_INTERVAL = "fetch.default.interval";

    String FETCH_MAX_RETRY = "fetch.retry.max";

    String FETCH_STORE_CONTENT = "fetch.store.content";

    String FETCH_NET_BANDWIDTH_M = "fetcher.net.bandwidth.m";

    /**
     * The maximum number of pages to export in fetch phrase.
     * */
    String FETCH_PAGE_AUTO_EXPORT_LIMIT = "fetch.page.auto.export.limit";

    /**
     * Browser
     * */
    String FETCH_PAGE_LOAD_TIMEOUT = "fetch.page.load.timeout";
    String FETCH_SCRIPT_TIMEOUT = "fetch.script.timeout";
    String FETCH_SCROLL_DOWN_COUNT = "fetch.scroll.down.count";
    String FETCH_SCROLL_DOWN_INTERVAL = "fetch.scroll.down.interval";

    String FETCH_CLIENT_JS_COMPUTED_STYLES = "fetch.browser.client.js.computed.styles";
    String FETCH_CLIENT_JS_PROPERTY_NAMES = "fetch.browser.client.js.property.names";

    String FETCH_MAX_CONTENT_LENGTH = "fetch.max.content.length";

    ///////////////////////////////////////////////////////////////////////////
    // Privacy context

    /**
     * The number of active privacy contexts.
     */
    String PRIVACY_CONTEXT_NUMBER = "privacy.context.number";
    /**
     * The minimal number of sequential privacy agents, the active privacy contexts is chosen from them.
     * */
    String MIN_SEQUENTIAL_PRIVACY_AGENT_NUMBER = "min.sequential.privacy.agent.number";
    /**
     * The maximum number of sequential privacy agents, the active privacy contexts is chosen from them.
     * */
    String MAX_SEQUENTIAL_PRIVACY_AGENT_NUMBER = "max.sequential.privacy.agent.number";
    /**
     * The class name of privacy agent generator
     * */
    String PRIVACY_AGENT_GENERATOR_CLASS = "privacy.agent.generator.class";
    /**
     * The class name of privacy agent generator
     * @deprecated use {@link #PRIVACY_AGENT_GENERATOR_CLASS} instead
     * */
    String PRIVACY_AGENT_GENERATOR_CLASS_KEY = PRIVACY_AGENT_GENERATOR_CLASS;
    String PRIVACY_MINOR_WARNING_FACTOR = "privacy.minor.warning.factor";

    String PRIVACY_CONTEXT_IDLE_TIMEOUT = "privacy.idle.timeout";

    String PRIVACY_CONTEXT_FAILURE_RATE_THRESHOLD = "privacy.failure.rate.threshold";
    String PRIVACY_MAX_WARNINGS = "privacy.max.warnings";
    String PRIVACY_CONTEXT_MIN_THROUGHPUT = "privacy.context.min.throughput";
    /**
     * The strategy to close privacy context: asap, lazy
     * */
    String PRIVACY_CONTEXT_CLOSE_LAZY = "privacy.close.strategy";


    ///////////////////////////////////////////////////////////////////////////
    // Browser

    /**
     * The max value of tabs a browser can open
     */
    String BROWSER_MAX_ACTIVE_TABS = "browser.max.active.tabs";
    /**
     * The web driver to use
     * */
    String BROWSER_WEB_DRIVER_CLASS = "browser.web.driver.class";
    String BROWSER_WEB_DRIVER_PRIORITY = "browser.web.driver.priority";
    String BROWSER_DRIVER_POOL_IDLE_TIMEOUT = "browser.driver.pool.idle.timeout";
    String BROWSER_TYPE = "browser.type";
    // not used since the browser is always running in temporary contexts
    String BROWSER_INCOGNITO = "browser.incognito";
    /**
     * The browser interact settings
     * */
    String BROWSER_INTERACT_SETTINGS = "browser.interact.settings";
    String BROWSER_DISPLAY_MODE = "browser.display.mode";
    String BROWSER_IMAGES_ENABLED = "browser.images.enabled";
    String BROWSER_JS_INVADING_ENABLED = "browser.js.invading.enabled";

    String BROWSER_DELETE_ALL_COOKIES = "browser.delete.all.cookies";
    String BROWSER_RESPONSE_HANDLER = "browser.response.handler";
    /**
     * The probability to block urls specified by {@code WebDriver.addBlockedURLs}, between [0, 1]
     * */
    String BROWSER_RESOURCE_BLOCK_PROBABILITY = "browser.url.block.probability";
    String BROWSER_ENABLE_UA_OVERRIDING = "browser.enable.ua.overriding";
    String BROWSER_SPA_MODE = "browser.spa.mode";
    String BROWSER_CHROME_PATH = "browser.chrome.path";
    /**
     * Whether reuse the recovered drivers to serve new tasks.
     * */
    String BROWSER_REUSE_RECOVERED_DRIVERS = "browser.reuse.recovered.drivers";
    /**
     * Add a --no-sandbox flag to launch the chrome if we are running inside a virtual machine,
     * for example, virtualbox, vmware or WSL
     * */
    String BROWSER_LAUNCH_NO_SANDBOX = "browser.launch.no.sandbox";
    String BROWSER_LAUNCH_SUPERVISOR_PROCESS = "browser.launch.supervisor.process";
    String BROWSER_LAUNCH_SUPERVISOR_PROCESS_ARGS = "browser.launch.supervisor.process.args";

    ///////////////////////////////////////////////////////////////////////////
    // Proxy

    /**
     * Proxy
     */
    String PROXY_USE_PROXY = "use_proxy"; // keep consist with wget
    String PROXY_POOL_MANAGER_CLASS = "proxy.pool.manager.class";
    String PROXY_LOADER_CLASS = "proxy.loader.class";
    String PROXY_MAX_FETCH_SUCCESS = "proxy.max.fetch.success";
    String PROXY_MAX_ALLOWED_PROXY_ABSENCE = "proxy.max.allowed.proxy.absence";
    String PROXY_POOL_CAPACITY = "proxy.pool.size";
    String PROXY_POOL_POLLING_TIMEOUT = "proxy.pool.polling.interval";
    String PROXY_IDLE_TIMEOUT = "proxy.idle.timeout";
    String PROXY_ENABLE_DEFAULT_PROVIDERS = "proxy.enable.default.providers";

    ///////////////////////////////////////////////////////////////////////////
    // Network

    String HTTP_TIMEOUT = "http.timeout";
    String HTTP_FETCH_MAX_RETRY = "http.fetch.max.retry";


    ///////////////////////////////////////////////////////////////////////////
    // Generate phrase

    /**
     * Generator parameters
     */
    String PARTITION_MODE_KEY = "partition.url.mode";
    String PARTITION_URL_SEED = "partition.url.seed";

    String GENERATE_TIME = "generate.generate.time";
    String GENERATE_UPDATE_CRAWLDB = "generate.update.crawldb";
    String GENERATE_MIN_SCORE = "generate.min.score";
    String GENERATE_REGENERATE = "generate.regenerate";
    String GENERATE_REGENERATE_SEEDS = "generate.regenerate.seeds";
    String GENERATE_FILTER = "generate.filter";
    String GENERATE_NORMALISE = "generate.normalise";
    String GENERATE_MAX_TASKS_PER_HOST = "generate.max.tasks.per.host";
    String GENERATE_SITE_GROUP_MODE = "generate.count.mode";
    String GENERATE_TOP_N = "generate.topN";
    String GENERATE_LAST_GENERATED_ROWS = "generate.last.generated.rows";
    String GENERATE_CUR_TIME = "generate.curr.time";
    String GENERATE_DETAIL_PAGE_RATE = "generate.detail.page.rate";
    String GENERATE_DELAY = "crawl.gen.delay";
    String GENERATE_RANDOM_SEED = "generate.partition.seed";


    ///////////////////////////////////////////////////////////////////////////
    // Parse phrase

    /**
     * Parser parameters
     */
    String PARSE_PARSE = "parser.parse";
    String PARSE_REPARSE = "parser.reparse";
    String PARSE_TIMEOUT = "parser.timeout";
    String PARSE_NORMALISE = "parse.normalise";
    String PARSE_MAX_URL_LENGTH = "parse.max.url.length";
    String PARSE_MIN_ANCHOR_LENGTH = "parse.min.anchor.length";
    String PARSE_MAX_ANCHOR_LENGTH = "parse.max.anchor.length";
    String PARSE_LINK_PATTERN = "parse.link.pattern";
    String PARSE_MAX_LINKS_PER_PAGE = "parse.max.links";
    String PARSE_IGNORE_EXTERNAL_LINKS = "parse.ignore.external.links";
    String PARSE_SKIP_TRUNCATED = "parser.skip.truncated";
    String PARSE_HTML_IMPL = "parser.html.impl";
    String PARSE_SUPPORT_ALL_CHARSETS = "parser.support.all.charsets";
    String PARSE_SUPPORTED_CHARSETS = "parser.supported.charsets";
    String PARSE_DEFAULT_ENCODING = "parser.character.encoding.default";
    String PARSE_CACHING_FORBIDDEN_POLICY = "parser.caching.forbidden.policy";
    String PARSE_TIKA_HTML_MAPPER_NAME = "tika.htmlmapper.classname";

    String PARSE_RETRIEVE_FADED_LINKS = "parse.retrieve.faded.links";

    ///////////////////////////////////////////////////////////////////////////
    // Update phrase

    /**
     * DbUpdater parameters
     */
    String UPDATE_MAX_INLINKS = "update.max.inlinks";
    String UPDATE_IGNORE_IN2OUT_GRAPH = "update.ignore.in.graph";

    ///////////////////////////////////////////////////////////////////////////
    // Scheduling

    String SCHEDULE_INC_RATE = "db.fetch.schedule.adaptive.inc_rate";
    String SCHEDULE_DEC_RATE = "db.fetch.schedule.adaptive.dec_rate";
    String SCHEDULE_MIN_INTERVAL = "db.fetch.schedule.adaptive.min_interval";
    String SCHEDULE_MAX_INTERVAL = "db.fetch.schedule.adaptive.max_interval";
    String SCHEDULE_SEED_MAX_INTERVAL = "db.fetch.schedule.adaptive.seed_max_interval";
    String SCHEDULE_SYNC_DELTA = "db.fetch.schedule.adaptive.sync_delta";
    String SCHEDULE_SYNC_DELTA_RATE = "db.fetch.schedule.adaptive.sync_delta_rate";

    ///////////////////////////////////////////////////////////////////////////
    // Scoring

    /**
     * Scoring
     */
    // divisor may have a better name
    String SCORE_SORT_ERROR_COUNTER_DIVISOR = "score.sort.error.counter.divisor";
    String SCORE_SORT_WEB_GRAPH_SCORE_DIVISOR = "score.sort.web.graph.score.divisor";
    String SCORE_SORT_CONTENT_SCORE_DIVISOR = "score.sort.content.score.divisor";


    ///////////////////////////////////////////////////////////////////////////
    // Indexing

    /**
     * Indexing parameters
     */
    String INDEXER_JIT = "indexer.just.in.time";
    String INDEXER_HOSTNAME = "index.server.hostname";
    String INDEXER_PORT = "index.server.port";


    ///////////////////////////////////////////////////////////////////////////
    // Other

    /**
     * Create default data collectors or not
     * */
    String CRAWL_ENABLE_DEFAULT_DATA_COLLECTORS = "crawl.enable.default.data.collectors";
    /**
     * Create default data collectors or not
     * */
    String CRAWL_SMART_RETRY = "crawl.smart.retry";
    /**
     * The size of global page cache
     * */
    String GLOBAL_PAGE_CACHE_SIZE = "global.page.cache.size";
    /**
     * The size of global document cache
     * */
    String GLOBAL_DOCUMENT_CACHE_SIZE = "global.document.cache.size";

    /**
     * Sites may request that search engines don't provide access to cached
     * documents.
     */
    String CACHING_FORBIDDEN_KEY = "caching.forbidden";

    String PULSAR_DOMAIN = "pulsar.domain";

    // H2
    String H2_SESSION_FACTORY_CLASS = "h2.sessionFactory";

    String SCENT_EXTRACT_TABULATE_CELL_TYPE = "scent.extract.tabulate.cell.type";
}
