/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a getConf of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.platon.pulsar.common.config;

/**
 * Created by vincent on 17-1-17.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
@SuppressWarnings("unused")
public interface CapabilityTypes {

    /**
     * Common Parameters
     */
    String PULSAR_CONFIG_DIR = "pulsar.configuration.dir";
    String PULSAR_CONFIG_ID = "pulsar.config.id";
    String PULSAR_CONFIG_PREFERRED_DIR = "pulsar.config.preferred.dir";
    String PULSAR_CONFIG_RESOURCES = "pulsar.config.resources";

    String PARAM_HOME_DIR = "pulsar.home.dir";
    String PARAM_TMP_DIR = "pulsar.tmp.dir";
    String PARAM_DATA_DIR = "pulsar.data.dir";

    String PARAM_OUTPUT_DIR = "pulsar.output.dir";
    String PARAM_REPORT_DIR = "pulsar.report.dir";
    String PARAM_IDENT_STR = "pulsar.id.str";

    String PULSAR_CLUSTER_SLAVES = "pulsar.cluster.slaves";
    String DRY_RUN = "pulsar.dry.run";

    /**
     * Args
     */
    String TENANT_ID = "tenantId";
    String START_KEY = "startKey";
    String END_KEY = "endKey";
    String START = "start";
    String LIMIT = "limit";
    String REGEX = "regex";
    String FIELDS = "fields";
    String ATTRIBUTE = "attribute";
    String ENTITY_DOMAIN = "domain";
    String EXTRACTOR = "extractor";
    String BUILDER = "builder";
    String FORMAT = "format";
    String TASK_NAME = "taskName";
    String OUTPUT_DIR = "outputDir";

    String CRAWL_ROUND = "crawl.round";
    String CRAWL_MAX_DISTANCE = "crawl.max.distance";
    String BATCH_ID = "pulsar.batch.name";
    String PARAM_JOB_NAME = "pulsar.job.name";
    String MAPREDUCE_JOB_REDUCES = "mapreduce.job.reduces";
    String MAPPER_LIMIT = "pulsar.mapper.limit";
    String REDUCER_LIMIT = "pulsar.reducer.limit";
    String REINDEX = "reindex";
    String FORCE = "force";
    String RESUME = "pulsar.job.resume";
    String RECENT_DAYS_WINDOWN = "recent.days.window";
    String REPORTER_REPORT_INTERVAL = "reporter.report.interval";
    String METATAG_NAMES = "metatags.names";

    /**
     * Status / result message.
     * */
    String STAT_PULSAR_STATUS = "Pulsar Status";
    String STAT_INFECTED_ROWS = "injectedRows";

    // short constants for status / results fields
    /**
     * Status / result message.
     */
    String STAT_MESSAGE = "msg";
    /**
     * Phase of processing.
     */
    String STAT_PHASE = "phase";
    /**
     * Progress (float).
     */
    String STAT_PROGRESS = "progress";
    /**
     * Jobs.
     */
    String STAT_JOBS = "jobs";
    /**
     * Counters.
     */
    String STAT_COUNTERS = "counters";

    String COUNTER_GROUP_STATUS = "Runtime Status";

    /**
     * Generate
     * */
    String GENERATE_COUNT_VALUE_DOMAIN = "domain";
    String GENERATE_COUNT_VALUE_HOST = "host";
    String GENERATE_COUNT_VALUE_IP = "ip";

    /**
     * Thread pool/ExecuteService
     */
    String GLOBAL_EXECUTOR_CONCURRENCY_HINT = "global.executor.concurrency.hint";
    String GLOBAL_EXECUTOR_AUTO_CONCURRENCY_FACTOR = "global.executor.auto.concurrency.factor";
    /**
     * Distribution
     */
    String PULSAR_MASTER_HOST = "pulsar.master.host";
    String PULSAR_MASTER_PORT = "pulsar.master.port";
    String UPSTREAM_PUSH_URL = "pulsar.upstream.push.url";
    String UPSTREAM_PULL_URL = "pulsar.upstream.pull.url";
    /**
     * Storage
     */
    String STORAGE_CRAWL_ID = "storage.crawl.id";
    String STORAGE_SCHEMA_WEBPAGE = "storage.schema.webpage";
    String STORAGE_PREFERRED_SCHEMA_NAME = "preferred.schema.name";
    String STORAGE_DATA_STORE_CLASS = "storage.data.store.class";
    String STORAGE_DATUM_EXPIRES = "storage.datum.expires";
    String STORAGE_EMBED_MONGO = "storage.embed.mongo";

    String GORA_MONGODB_SERVERS = "gora.mongodb.servers";
    // String GORA_MONGODB_EMBED_SERVERS = "gora.mongodb.embed.servers";
    /**
     * Spring
     */
    String APPLICATION_CONTEXT_CONFIG_LOCATION = "application.context.config.location";
    /**
     * Session
     */
    String SESSION_MAX_WAIT_TIME = "session.max.wait.time";
    String SESSION_MIN_ACCEPTABLE_RESPONSE_SIZE = "session.min.acceptable.response.size";

    /**
     * Inject parameters
     */
    String INJECT_SEEDS = "inject.seeds";
    String INJECT_SEED_PATH = "inject.seed.dir";
    String INJECT_UPDATE = "inject.update";
    String INJECT_WATCH = "inject.watch";
    String INJECT_SCORE = "db.score.injected";

    /**
     * Query engine parameters
     */
    String QE_HANDLE_PERIODICAL_FETCH_TASKS = "query.engine.handle.periodical.fetch.tasks";

    /**
     * Load parameters
     */
    String LOAD_HARD_REDIRECT = "load.hard.redirect";
    /**
     * Fetch parameters
     */
    String FETCH_MODE = "fetch.fetch.mode";
    String FETCH_EAGER_FETCH_LIMIT = "fetch.eager.fetch.limit";

    String FETCH_THREADS_FETCH = "fetch.threads.fetch";
    String FETCH_CRAWL_PATH_STRATEGY = "fetch.crawl.path.strategy";
    String FETCH_JOB_TIMEOUT = "fetch.job.timeout";
    String FETCH_TASK_TIMEOUT = "fetch.task.timeout";
    String FETCH_PENDING_TIMEOUT = "fetch.pending.timeout";
    String FETCH_SERVER_REQUIRED = "fetch.fetch.server.required";
    String FETCH_QUEUE_MODE = "fetch.queue.mode";
    String FETCH_QUEUE_USE_HOST_SETTINGS = "fetch.queue.use.host.settings";
    String FETCH_QUEUE_RETUNE_INTERVAL = "fetch.pending.queue.check.time";
    String FETCH_FEEDER_INIT_BATCH_SIZE = "fetch.feeder.init.batch.size";
    String FETCH_THREADS_PER_QUEUE = "fetch.threads.per.queue";
    String FETCH_THROUGHPUT_THRESHOLD_PAGES = "fetch.throughput.threshold.pages";
    String FETCH_THROUGHPUT_THRESHOLD_SEQENCE = "fetch.throughput.threshold.sequence";
    String FETCH_THROUGHPUT_CHECK_INTERVAL = "fetch.throughput.check.interval";
    String FETCH_CHECK_INTERVAL = "fetch.check.interval";
    String FETCH_QUEUE_DELAY = "fetch.queue.delay";
    String FETCH_QUEUE_MIN_DELAY = "fetch.queue.min.delay";
    String FETCH_MIN_INTERVAL = "db.fetch.interval.min";
    String FETCH_MAX_INTERVAL = "db.fetch.interval.max";
    String FETCH_INTERVAL = "fetch.fetch.interval";
    String FETCH_DEFAULT_INTERVAL = "db.fetch.interval.default";
    String FETCH_MAX_RETRY = "db.fetch.retry.max";
    String FETCH_STORE_CONTENT = "fetch.store.content";
    String FETCH_PROTOCOL_SHARED_FILE_TIMEOUT = "fetch.protocol.shared.file.timeout";

    /**
     * Browser
     * */
    String FETCH_PAGE_LOAD_TIMEOUT = "fetch.page.load.timeout";
    String FETCH_SCRIPT_TIMEOUT = "fetch.script.timeout";
    String FETCH_DOM_WAIT_FOR_TIMEOUT = "fetch.dom.wait.for.timeout";
    String FETCH_SCROLL_DOWN_COUNT = "fetch.scroll.down.count";
    String FETCH_SCROLL_DOWN_INTERVAL = "fetch.scroll.down.interval";
    String FETCH_CLIENT_JS = "fetch.browser.client.js";
    String FETCH_CLIENT_JS_COMPUTED_STYLES = "fetch.browser.client.js.computed.styles";
    String FETCH_CLIENT_JS_PROPERTY_NAMES = "fetch.browser.client.js.required.property.names";
    /**
     * Selenium
     */
    String SELENIUM_BROWSER = "selenium.browser";
    String SELENIUM_MAX_WEB_DRIVERS = "selenium.max.web.drivers";
    String SELENIUM_BROWSER_HEADLESS = "selenium.browser.headless";
    String SELENIUM_WEB_DRIVER_CLASS = "selenium.web.driver.class";
    String SELENIUM_WEB_DRIVER_PRIORITY = "selenium.web.driver.priority";

    /**
     * Proxy
     */
    String PROXY_DISABLED = "proxy.disabled";
    String PROXY_IP_PORT = "proxy.ip.port";
    String PROXY_POOL_SIZE = "proxy.pool.size";
    String PROXY_POOL_POLLING_WAIT = "proxy.pool.polling.wait";

    /**
     * Network
     */
    String HTTP_TIMEOUT = "http.timeout";
    String HTTP_FETCH_MAX_RETRY = "http.fetch.max.retry";
    /**
     * Generator parameters
     */
    String GENERATE_TIME = "generate.generate.time";
    String GENERATE_UPDATE_CRAWLDB = "generate.update.crawldb";
    String GENERATE_MIN_SCORE = "generate.min.score";
    String GENERATE_REGENERATE = "generate.regenerate";
    String GENERATE_REGENERATE_SEEDS = "generate.regenerate.seeds";
    String GENERATE_FILTER = "generate.filter";
    String GENERATE_NORMALISE = "generate.normalise";
    String GENERATE_MAX_TASKS_PER_HOST = "generate.max.tasks.per.host";
    String GENERATE_COUNT_MODE = "generate.count.mode";
    String GENERATE_TOP_N = "generate.topN";
    String GENERATE_LAST_GENERATED_ROWS = "generate.last.generated.rows";
    String GENERATE_CUR_TIME = "generate.curr.time";
    String GENERATE_DETAIL_PAGE_RATE = "generate.detail.page.rate";
    String GENERATE_DELAY = "crawl.gen.delay";
    String GENERATE_RANDOM_SEED = "generate.partition.seed";
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
    // TODO: not used, may be caused by a git merge problem
    String PARSE_RETRIEVE_FADED_LINKS = "parse.retrieve.faded.links";

    /**
     * DbUpdater parameters
     */
    String UPDATE_MAX_INLINKS = "db.update.max.inlinks";
    /**
     * Scoring
     */
    // divisor may have a better name
    String SCORE_SORT_ERROR_COUNTER_DIVISOR = "score.sort.error.counter.divisor";
    String SCORE_SORT_WEB_GRAPH_SCORE_DIVISOR = "score.sort.web.graph.score.divisor";
    String SCORE_SORT_CONTENT_SCORE_DIVISOR = "score.sort.content.score.divisor";
    /**
     * Indexing parameters
     */
    String INDEX_JIT = "fetch.index.just.in.time";
    String INDEXER_HOSTNAME = "index.server.hostname";
    String INDEXER_PORT = "index.server.port";
    String INDEXER_URL = "indexer.url";
    String INDEXER_ZK = "indexer.zookeeper.hosts";
    String INDEXER_COLLECTION = "indexer.collection";
    String INDEXER_WRITE_COMMIT_SIZE = "indexer.write.commit.size";
    /**
     * Stat
     */
    String STAT_INDEX_HOME_URL = "stat.index.home.url";
    /**
     * Service
     */
    String MASTER_PORT = "master.port";
    /**
     * Sites may request that search engines don't provide access to cached
     * documents.
     */
    String CACHING_FORBIDDEN_KEY = "caching.forbidden";

    /**
     * Show both original forbidden content and summaries (default).
     */
    String CACHING_FORBIDDEN_NONE = "none";

    /**
     * Don't show either original forbidden content or summaries.
     */
    String CACHING_FORBIDDEN_ALL = "all";

    String NODE_FEATURE_CALCULATOR = "pulsar.node.feature.calculator";

    String PULSAR_DOMAIN = "scent.domain";
    String SCENT_TASK_IDENT = "scent.task.ident";
    String SCENT_FILE_SERVER_HOST = "scent.file.server.host";
    String SCENT_FILE_SERVER_PORT = "scent.file.server.port";

    String SCENT_DIAGNOSTOR_ENABLED = "scent.diagnostor.enabled";

    // FEATURE
    String SCENT_OUT_DIR_FEATURE = "scent.out.dir.feature";

    // NLP
    String SCENT_NLP_WORD_NET_CONCEPT = "scent.nlp.word.net.concept";
    String SCENT_NLP_SEMANTIC_SIMILARITY_ENABLED = "scent.nlp.semantic.similarity.enabled";

    // SEGMENT
    String SCENT_CHILDREN_SUMMARY_ITEM_MIN = "scent.children.summary.item.min";
    String SCENT_CHILDREN_SUMMARY_SAMPLE_MAX = "scent.children.summary.sample.max";
    String SCENT_CHILDREN_SUMMARY_FEATURES = "scent.children.summary.features";
    String SCENT_CHILDREN_SUMMARY_THRESHOLD = "scent.children.summary.threshold";
    String SCENT_CHILDREN_SUMMARY_REPORT = "scent.children.summary.report";

    // CLASSIFY
    String SCENT_CLASSIFIER_BLOCK_LABELS = "scent.classifier.block.labels";
    String SCENT_DIAGNOSE_CLASSIFIER_BLOCK_LABELS = "scent.diagnose.classifier.block.labels";
    String SCENT_CLASSIFIER_BLOCK_INHERITABLE_LABLES = "scent.classifier.block.inheritable.labels";
    String SCENT_CLASSIFIER_WEIGHT_CODE_STRUCTURE = "scent.classifier.weight.code.structure";
    String SCENT_CLASSIFIER_WEIGHT_BLOCK_TEXT = "scent.classifier.weight.block.text";
    String SCENT_CLASSIFIER_WEIGHT_BLOCK_TITLE = "scent.classifier.weight.block.title";

    // EXTRACT
    String SCENT_EXTRACT_EXTRACT_FOR_LABEL = "scent.extract.extractor.for.label";
    String SCENT_EXTRACT_REFRESH_FEATURE = "scent.extract.refresh.feature";
    String SCENT_EXTRACT_GREEDY = "scent.extract.greedy";
    String SCENT_EXTRACT_KEEP_ELEMENT_METADATA = "scent.extract.keep.element.metadata";

    // BUILD
    String SCENT_WIKI_DOMAIN = "scent.wiki.domain";
    String SCENT_WIKI_USERNAME = "scent.wiki.username";
    String SCENT_WIKI_PASSWORD = "scent.wiki.password";

    // Spark
    String SPARK_MASTER = "spark.master";
}
