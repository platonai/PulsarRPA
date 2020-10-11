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
 *
 * @author vincent
 * @version $Id: $Id
 */
@SuppressWarnings("unused")
public interface CapabilityTypes {

    /**
     * Common Parameters
     */
    String LEGACY_CONFIG_PROFILE = "hadoop.config.profile";
    /** Constant <code>SYSTEM_PROPERTY_SPECIFIED_RESOURCES="system.property.specified.resources"</code> */
    String SYSTEM_PROPERTY_SPECIFIED_RESOURCES = "system.property.specified.resources";

    /** Constant <code>PARAM_OUTPUT_DIR="pulsar.output.dir"</code> */
    String PARAM_OUTPUT_DIR = "pulsar.output.dir";
    /** Constant <code>PARAM_REPORT_DIR="pulsar.report.dir"</code> */
    String PARAM_REPORT_DIR = "pulsar.report.dir";
    /** Constant <code>PARAM_IDENT_STR="pulsar.id.str"</code> */
    String PARAM_IDENT_STR = "pulsar.id.str";

    /** Constant <code>PULSAR_CLUSTER_SLAVES="pulsar.cluster.slaves"</code> */
    String PULSAR_CLUSTER_SLAVES = "pulsar.cluster.slaves";
    /** Constant <code>DRY_RUN="pulsar.dry.run"</code> */
    String DRY_RUN = "pulsar.dry.run";

    /**
     * Args
     */
    String TENANT_ID = "tenantId";
    /** Constant <code>START_KEY="startKey"</code> */
    String START_KEY = "startKey";
    /** Constant <code>END_KEY="endKey"</code> */
    String END_KEY = "endKey";
    /** Constant <code>START="start"</code> */
    String START = "start";
    /** Constant <code>LIMIT="limit"</code> */
    String LIMIT = "limit";
    /** Constant <code>REGEX="regex"</code> */
    String REGEX = "regex";
    /** Constant <code>FIELDS="fields"</code> */
    String FIELDS = "fields";
    /** Constant <code>ATTRIBUTE="attribute"</code> */
    String ATTRIBUTE = "attribute";
    /** Constant <code>ENTITY_DOMAIN="domain"</code> */
    String ENTITY_DOMAIN = "domain";
    /** Constant <code>EXTRACTOR="extractor"</code> */
    String EXTRACTOR = "extractor";
    /** Constant <code>BUILDER="builder"</code> */
    String BUILDER = "builder";
    /** Constant <code>FORMAT="format"</code> */
    String FORMAT = "format";
    /** Constant <code>TASK_NAME="taskName"</code> */
    String TASK_NAME = "taskName";
    /** Constant <code>OUTPUT_DIR="outputDir"</code> */
    String OUTPUT_DIR = "outputDir";

    /** Constant <code>CRAWL_ROUND="crawl.round"</code> */
    String CRAWL_ROUND = "crawl.round";
    /** Constant <code>CRAWL_MAX_DISTANCE="crawl.max.distance"</code> */
    String CRAWL_MAX_DISTANCE = "crawl.max.distance";
    /** Constant <code>BATCH_ID="pulsar.batch.name"</code> */
    String BATCH_ID = "pulsar.batch.name";
    /** Constant <code>PARAM_JOB_NAME="pulsar.job.name"</code> */
    String PARAM_JOB_NAME = "pulsar.job.name";
    /** Constant <code>MAPREDUCE_JOB_REDUCES="mapreduce.job.reduces"</code> */
    String MAPREDUCE_JOB_REDUCES = "mapreduce.job.reduces";
    /** Constant <code>MAPPER_LIMIT="pulsar.mapper.limit"</code> */
    String MAPPER_LIMIT = "pulsar.mapper.limit";
    /** Constant <code>REDUCER_LIMIT="pulsar.reducer.limit"</code> */
    String REDUCER_LIMIT = "pulsar.reducer.limit";
    /** Constant <code>REINDEX="reindex"</code> */
    String REINDEX = "reindex";
    /** Constant <code>FORCE="force"</code> */
    String FORCE = "force";
    /** Constant <code>RESUME="pulsar.job.resume"</code> */
    String RESUME = "pulsar.job.resume";
    /** Constant <code>RECENT_DAYS_WINDOWN="recent.days.window"</code> */
    String RECENT_DAYS_WINDOWN = "recent.days.window";
    /** Constant <code>REPORTER_REPORT_INTERVAL="reporter.report.interval"</code> */
    String REPORTER_REPORT_INTERVAL = "reporter.report.interval";
    /** Constant <code>METATAG_NAMES="metatags.names"</code> */
    String METATAG_NAMES = "metatags.names";

    /**
     * Status / result message.
     * */
    String STAT_PULSAR_STATUS = "Pulsar Status";
    /** Constant <code>STAT_INFECTED_ROWS="injectedRows"</code> */
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

    /** Constant <code>COUNTER_GROUP_STATUS="Runtime Status"</code> */
    String COUNTER_GROUP_STATUS = "Runtime Status";

    /**
     * Generate
     * */
    String GENERATE_COUNT_VALUE_DOMAIN = "domain";
    /** Constant <code>GENERATE_COUNT_VALUE_HOST="host"</code> */
    String GENERATE_COUNT_VALUE_HOST = "host";
    /** Constant <code>GENERATE_COUNT_VALUE_IP="ip"</code> */
    String GENERATE_COUNT_VALUE_IP = "ip";

    /**
     * Thread pool/ExecuteService
     */
    String GLOBAL_EXECUTOR_CONCURRENCY_HINT = "global.executor.concurrency.hint";
    /** Constant <code>GLOBAL_EXECUTOR_AUTO_CONCURRENCY_FACTOR="global.executor.auto.concurrency.factor"</code> */
    String GLOBAL_EXECUTOR_AUTO_CONCURRENCY_FACTOR = "global.executor.auto.concurrency.factor";
    /**
     * Distribution
     */
    String PULSAR_MASTER_HOST = "pulsar.master.host";
    /** Constant <code>PULSAR_MASTER_PORT="pulsar.master.port"</code> */
    String PULSAR_MASTER_PORT = "pulsar.master.port";
    /** Constant <code>UPSTREAM_PUSH_URL="pulsar.upstream.push.url"</code> */
    String UPSTREAM_PUSH_URL = "pulsar.upstream.push.url";
    /** Constant <code>UPSTREAM_PULL_URL="pulsar.upstream.pull.url"</code> */
    String UPSTREAM_PULL_URL = "pulsar.upstream.pull.url";
    /**
     * Storage
     */
    String STORAGE_CRAWL_ID = "storage.crawl.id";
    /** Constant <code>STORAGE_SCHEMA_WEBPAGE="storage.schema.webpage"</code> */
    String STORAGE_SCHEMA_WEBPAGE = "storage.schema.webpage";
    /** Constant <code>STORAGE_PREFERRED_SCHEMA_NAME="preferred.schema.name"</code> */
    String STORAGE_PREFERRED_SCHEMA_NAME = "preferred.schema.name";
    /** Constant <code>STORAGE_DATA_STORE_CLASS="storage.data.store.class"</code> */
    String STORAGE_DATA_STORE_CLASS = "storage.data.store.class";
    /** Constant <code>STORAGE_DATUM_EXPIRES="storage.datum.expires"</code> */
    String STORAGE_DATUM_EXPIRES = "storage.datum.expires";
    /** Constant <code>STORAGE_EMBED_MONGO="storage.embed.mongo"</code> */
    String STORAGE_EMBED_MONGO = "storage.embed.mongo";

    /** Constant <code>GORA_MONGODB_SERVERS="gora.mongodb.servers"</code> */
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
    /** Constant <code>SESSION_MIN_ACCEPTABLE_RESPONSE_SIZE="session.min.acceptable.response.size"</code> */
    String SESSION_MIN_ACCEPTABLE_RESPONSE_SIZE = "session.min.acceptable.response.size";

    /**
     * Inject parameters
     */
    String INJECT_SEEDS = "inject.seeds";
    /** Constant <code>INJECT_SEED_PATH="inject.seed.dir"</code> */
    String INJECT_SEED_PATH = "inject.seed.dir";
    /** Constant <code>INJECT_UPDATE="inject.update"</code> */
    String INJECT_UPDATE = "inject.update";
    /** Constant <code>INJECT_WATCH="inject.watch"</code> */
    String INJECT_WATCH = "inject.watch";
    /** Constant <code>INJECT_SCORE="db.score.injected"</code> */
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
    /** Constant <code>FETCH_WORKER_NAME_PREFIX="fetch.worker.name.prefix"</code> */
    String FETCH_WORKER_NAME_PREFIX = "fetch.worker.name.prefix";
    // In browser fetch mode, the fetch concurrency depends on the number of process of browsers which is the most critical resource
    /** Constant <code>FETCH_CONCURRENCY="fetch.concurrency"</code> */
    String FETCH_CONCURRENCY = "fetch.concurrency";

    /** Constant <code>FETCH_CRAWL_PATH_STRATEGY="fetch.crawl.path.strategy"</code> */
    String FETCH_CRAWL_PATH_STRATEGY = "fetch.crawl.path.strategy";
    /** Constant <code>FETCH_JOB_TIMEOUT="fetch.job.timeout"</code> */
    String FETCH_JOB_TIMEOUT = "fetch.job.timeout";
    /** Constant <code>FETCH_TASK_TIMEOUT="fetch.task.timeout"</code> */
    String FETCH_TASK_TIMEOUT = "fetch.task.timeout";
    /** Constant <code>FETCH_PENDING_TIMEOUT="fetch.pending.timeout"</code> */
    String FETCH_PENDING_TIMEOUT = "fetch.pending.timeout";
    /** Constant <code>FETCH_SERVER_REQUIRED="fetch.fetch.server.required"</code> */
    String FETCH_SERVER_REQUIRED = "fetch.fetch.server.required";
    // TODO: name "queue" has changed to be "pool"
    /** Constant <code>FETCH_MAX_HOST_FAILURES="fetch.max.host.failures"</code> */
    String FETCH_MAX_HOST_FAILURES = "fetch.max.host.failures";
    /** Constant <code>FETCH_QUEUE_MODE="fetch.queue.mode"</code> */
    String FETCH_QUEUE_MODE = "fetch.queue.mode";
    /** Constant <code>FETCH_QUEUE_USE_HOST_SETTINGS="fetch.queue.use.host.settings"</code> */
    String FETCH_QUEUE_USE_HOST_SETTINGS = "fetch.queue.use.host.settings";
    /** Constant <code>FETCH_QUEUE_RETUNE_INTERVAL="fetch.pending.queue.check.time"</code> */
    String FETCH_QUEUE_RETUNE_INTERVAL = "fetch.pending.queue.check.time";
    /** Constant <code>FETCH_FEEDER_INIT_BATCH_SIZE="fetch.feeder.init.batch.size"</code> */
    String FETCH_FEEDER_INIT_BATCH_SIZE = "fetch.feeder.init.batch.size";
    /** Constant <code>FETCH_THREADS_PER_POOL="fetch.threads.per.pool"</code> */
    String FETCH_THREADS_PER_POOL = "fetch.threads.per.pool";
    /** Constant <code>FETCH_THROUGHPUT_PAGES_PER_SECOND="fetch.throughput.threshold.pages"</code> */
    String FETCH_THROUGHPUT_PAGES_PER_SECOND = "fetch.throughput.threshold.pages";
    /** Constant <code>FETCH_THROUGHPUT_THRESHOLD_SEQENCE="fetch.throughput.threshold.sequence"</code> */
    String FETCH_THROUGHPUT_THRESHOLD_SEQENCE = "fetch.throughput.threshold.sequence";
    /** Constant <code>FETCH_THROUGHPUT_CHECK_INTERVAL="fetch.throughput.check.interval"</code> */
    String FETCH_THROUGHPUT_CHECK_INTERVAL = "fetch.throughput.check.interval";
    /** Constant <code>FETCH_CHECK_INTERVAL="fetch.check.interval"</code> */
    String FETCH_CHECK_INTERVAL = "fetch.check.interval";
    /** Constant <code>FETCH_QUEUE_DELAY="fetch.queue.delay"</code> */
    String FETCH_QUEUE_DELAY = "fetch.queue.delay";
    /** Constant <code>FETCH_QUEUE_MIN_DELAY="fetch.queue.min.delay"</code> */
    String FETCH_QUEUE_MIN_DELAY = "fetch.queue.min.delay";
    /** Constant <code>FETCH_MIN_INTERVAL="db.fetch.interval.min"</code> */
    String FETCH_MIN_INTERVAL = "db.fetch.interval.min";
    /** Constant <code>FETCH_MAX_INTERVAL="db.fetch.interval.max"</code> */
    String FETCH_MAX_INTERVAL = "db.fetch.interval.max";
    /** Constant <code>FETCH_INTERVAL="fetch.fetch.interval"</code> */
    String FETCH_INTERVAL = "fetch.fetch.interval";
    /** Constant <code>FETCH_DEFAULT_INTERVAL="db.fetch.interval.default"</code> */
    String FETCH_DEFAULT_INTERVAL = "db.fetch.interval.default";
    /** Constant <code>FETCH_MAX_RETRY="db.fetch.retry.max"</code> */
    String FETCH_MAX_RETRY = "db.fetch.retry.max";
    /** Constant <code>FETCH_STORE_CONTENT="fetch.store.content"</code> */
    String FETCH_STORE_CONTENT = "fetch.store.content";
    /** Constant <code>FETCH_PROTOCOL_SHARED_FILE_TIMEOUT="fetch.protocol.shared.file.timeout"</code> */
    String FETCH_PROTOCOL_SHARED_FILE_TIMEOUT = "fetch.protocol.shared.file.timeout";
    /** Constant <code>FETCH_NET_BANDWIDTH_M="fetcher.net.bandwidth.m"</code> */
    String FETCH_NET_BANDWIDTH_M = "fetcher.net.bandwidth.m";

    /** Constant <code>FETCH_BEFORE_FETCH_HANDLER="onBeforeFetch"</code> */
    String FETCH_BEFORE_FETCH_HANDLER = "onBeforeFetch";
    /** Constant <code>FETCH_AFTER_FETCH_HANDLER="onAfterFetch"</code> */
    String FETCH_AFTER_FETCH_HANDLER = "onAfterFetch";
    /** Constant <code>FETCH_AFTER_FETCH_N_HANDLER="onAfterFetchN"</code> */
    String FETCH_AFTER_FETCH_N_HANDLER = "onAfterFetchN";
    /** Constant <code>FETCH_BEFORE_FETCH_BATCH_HANDLER="onBeforeFetchBatch"</code> */
    String FETCH_BEFORE_FETCH_BATCH_HANDLER = "onBeforeFetchBatch";
    /** Constant <code>FETCH_AFTER_FETCH_BATCH_HANDLER="onAfterFetchBatch"</code> */
    String FETCH_AFTER_FETCH_BATCH_HANDLER = "onAfterFetchBatch";

    /**
     * Browser
     * */
    String FETCH_PAGE_LOAD_TIMEOUT = "fetch.page.load.timeout";
    /** Constant <code>FETCH_SCRIPT_TIMEOUT="fetch.script.timeout"</code> */
    String FETCH_SCRIPT_TIMEOUT = "fetch.script.timeout";
    /** Constant <code>FETCH_SCROLL_DOWN_COUNT="fetch.scroll.down.count"</code> */
    String FETCH_SCROLL_DOWN_COUNT = "fetch.scroll.down.count";
    /** Constant <code>FETCH_SCROLL_DOWN_INTERVAL="fetch.scroll.down.interval"</code> */
    String FETCH_SCROLL_DOWN_INTERVAL = "fetch.scroll.down.interval";

    /** Constant <code>FETCH_CLIENT_JS="fetch.browser.client.js"</code> */
    String FETCH_CLIENT_JS = "fetch.browser.client.js";
    /** Constant <code>FETCH_CLIENT_JS_BEFORE_FEATURE_COMPUTE="fetch.browser.client.js.after.feature.c"{trunked}</code> */
    String FETCH_CLIENT_JS_BEFORE_FEATURE_COMPUTE = "fetch.browser.client.js.before.feature.compute";
    /** Constant <code>FETCH_CLIENT_JS_AFTER_FEATURE_COMPUTE="fetch.browser.client.js.after.feature.c"{trunked}</code> */
    String FETCH_CLIENT_JS_AFTER_FEATURE_COMPUTE = "fetch.browser.client.js.after.feature.compute";
    /** Constant <code>FETCH_CLIENT_JS_COMPUTED_STYLES="fetch.browser.client.js.computed.styles"</code> */
    String FETCH_CLIENT_JS_COMPUTED_STYLES = "fetch.browser.client.js.computed.styles";
    /** Constant <code>FETCH_CLIENT_JS_PROPERTY_NAMES="fetch.browser.client.js.property.names"</code> */
    String FETCH_CLIENT_JS_PROPERTY_NAMES = "fetch.browser.client.js.property.names";
    /**
     * Privacy control
     */
    String PRIVACY_CONTEXT_NUMBER = "privacy.context.number";
    /** The class name of privacy context id generator */
    String PRIVACY_CONTEXT_ID_GENERATOR_CLASS = "privacy.context.id.generator.class";
    /** Constant <code>PRIVACY_MINOR_WARNING_FACTOR="privacy.minor.warning.factor"</code> */
    String PRIVACY_MINOR_WARNING_FACTOR = "privacy.minor.warning.factor";
    /** Constant <code>PRIVACY_MAX_WARNINGS="privacy.max.warnings"</code> */
    String PRIVACY_MAX_WARNINGS = "privacy.max.warnings";
    /** Constant <code>PRIVACY_CONTEXT_MIN_THROUGHPUT="privacy.context.min.throughput"</code> */
    String PRIVACY_CONTEXT_MIN_THROUGHPUT = "privacy.context.min.throughput";
    /**
     * Browser control
     */
    String BROWSER_MAX_ACTIVE_TABS = "browser.max.active.tabs";
    /** Constant <code>BROWSER_EAGER_ALLOCATE_TABS="browser.eager.allocate.tabs"</code> */
    String BROWSER_EAGER_ALLOCATE_TABS = "browser.eager.allocate.tabs";
    /** Constant <code>BROWSER_WEB_DRIVER_CLASS="browser.web.driver.class"</code> */
    String BROWSER_WEB_DRIVER_CLASS = "browser.web.driver.class";
    /** Constant <code>BROWSER_WEB_DRIVER_PRIORITY="browser.web.driver.priority"</code> */
    String BROWSER_WEB_DRIVER_PRIORITY = "browser.web.driver.priority";
    /** Constant <code>BROWSER_DRIVER_POOL_IDLE_TIMEOUT="browser.driver.pool.idle.timeout"</code> */
    String BROWSER_DRIVER_POOL_IDLE_TIMEOUT = "browser.driver.pool.idle.timeout";
    /** Constant <code>BROWSER_TYPE="browser.type"</code> */
    String BROWSER_TYPE = "browser.type";
    /** Constant <code>BROWSER_INCOGNITO="browser.incognito"</code> */
    String BROWSER_INCOGNITO = "browser.incognito";
    /** Constant <code>BROWSER_DRIVER_HEADLESS="browser.driver.headless"</code> */
    String BROWSER_DRIVER_HEADLESS = "browser.driver.headless";
    /** Constant <code>BROWSER_IMAGES_ENABLED="browser.images.enabled"</code> */
    String BROWSER_IMAGES_ENABLED = "browser.images.enabled";
    /** Constant <code>BROWSER_JS_INVADING_ENABLED="browser.js.invading.enabled"</code> */
    String BROWSER_JS_INVADING_ENABLED = "browser.js.invading.enabled";
    /** Constant <code>BROWSER_DELETE_ALL_COOKIES="browser.delete.all.cookies"</code> */
    String BROWSER_DELETE_ALL_COOKIES = "browser.delete.all.cookies";
    /** Constant <code>BROWSER_EMULATOR_EVENT_HANDLER="browser.emulate.event.handler"</code> */
    String BROWSER_EMULATOR_EVENT_HANDLER = "browser.emulate.event.handler";
    /** Constant <code>BROWSER_ENABLE_URL_BLOCKING="browser.enable.url.blocking"</code> */
    String BROWSER_ENABLE_URL_BLOCKING = "browser.enable.url.blocking";
    /** Constant <code>BROWSER_CHROME_PATH="browser.chrome.path"</code> */
    String BROWSER_CHROME_PATH = "browser.chrome.path";
    /** Constant <code>BROWSER_DATA_DIR="browser.data.dir"</code> */
    String BROWSER_DATA_DIR = "browser.data.dir";
    /** Constant <code>BROWSER_TAKE_SCREENSHOT="browser.take.screenshot"</code> */
    String BROWSER_TAKE_SCREENSHOT = "browser.take.screenshot";
    /** Constant <code>BROWSER_LAUNCH_SUPERVISOR_PROCESS="browser.launch.supervisor.process"</code> */
    String BROWSER_LAUNCH_SUPERVISOR_PROCESS = "browser.launch.supervisor.process";
    /** Constant <code>BROWSER_LAUNCH_SUPERVISOR_PROCESS_ARGS="browser.launch.supervisor.process.args"</code> */
    String BROWSER_LAUNCH_SUPERVISOR_PROCESS_ARGS = "browser.launch.supervisor.process.args";

    /**
     * Proxy
     */
    String PROXY_USE_PROXY = "use_proxy"; // keep consist with wget
    /** Constant <code>PROXY_POOL_MONITOR_CLASS="proxy.pool.monitor.class"</code> */
    String PROXY_POOL_MONITOR_CLASS = "proxy.pool.monitor.class";
    /** Constant <code>PROXY_LOADER_CLASS="proxy.loader.class"</code> */
    String PROXY_LOADER_CLASS = "proxy.loader.class";
    /** Constant <code>PROXY_MAX_FETCH_SUCCESS="proxy.max.fetch.success"</code> */
    String PROXY_MAX_FETCH_SUCCESS = "proxy.max.fetch.success";
    /** Constant <code>PROXY_MAX_ALLOWED_PROXY_ABSENCE="proxy.max.allowed.proxy.absence"</code> */
    String PROXY_MAX_ALLOWED_PROXY_ABSENCE = "proxy.max.allowed.proxy.absence";
    /** Constant <code>PROXY_POOL_CAPACITY="proxy.pool.size"</code> */
    String PROXY_POOL_CAPACITY = "proxy.pool.size";
    /** Constant <code>PROXY_POOL_POLLING_TIMEOUT="proxy.pool.polling.interval"</code> */
    String PROXY_POOL_POLLING_TIMEOUT = "proxy.pool.polling.interval";
    /** Constant <code>PROXY_IDLE_TIMEOUT="proxy.idle.timeout"</code> */
    String PROXY_IDLE_TIMEOUT = "proxy.idle.timeout";
    /** Constant <code>PROXY_ENABLE_DEFAULT_PROVIDERS="proxy.enable.default.providers"</code> */
    String PROXY_ENABLE_DEFAULT_PROVIDERS = "proxy.enable.default.providers";
    /** Constant <code>PROXY_ENABLE_LOCAL_FORWARD_SERVER="proxy.enable.local.forward.server"</code> */
    String PROXY_ENABLE_LOCAL_FORWARD_SERVER = "proxy.enable.local.forward.server";
    /** Constant <code>PROXY_SERVER_BOSS_THREADS="proxy.forward.server.boss.threads"</code> */
    String PROXY_SERVER_BOSS_THREADS = "proxy.forward.server.boss.threads";
    /** Constant <code>PROXY_SERVER_WORKER_THREADS="proxy.forward.server.worker.threads"</code> */
    String PROXY_SERVER_WORKER_THREADS = "proxy.forward.server.worker.threads";

    /**
     * Network
     */
    String HTTP_TIMEOUT = "http.timeout";
    /** Constant <code>HTTP_FETCH_MAX_RETRY="http.fetch.max.retry"</code> */
    String HTTP_FETCH_MAX_RETRY = "http.fetch.max.retry";

    /**
     * Generator parameters
     */
    String PARTITION_MODE_KEY = "partition.url.mode";
    /** Constant <code>PARTITION_URL_SEED="partition.url.seed"</code> */
    String PARTITION_URL_SEED = "partition.url.seed";

    /** Constant <code>GENERATE_TIME="generate.generate.time"</code> */
    String GENERATE_TIME = "generate.generate.time";
    /** Constant <code>GENERATE_UPDATE_CRAWLDB="generate.update.crawldb"</code> */
    String GENERATE_UPDATE_CRAWLDB = "generate.update.crawldb";
    /** Constant <code>GENERATE_MIN_SCORE="generate.min.score"</code> */
    String GENERATE_MIN_SCORE = "generate.min.score";
    /** Constant <code>GENERATE_REGENERATE="generate.regenerate"</code> */
    String GENERATE_REGENERATE = "generate.regenerate";
    /** Constant <code>GENERATE_REGENERATE_SEEDS="generate.regenerate.seeds"</code> */
    String GENERATE_REGENERATE_SEEDS = "generate.regenerate.seeds";
    /** Constant <code>GENERATE_FILTER="generate.filter"</code> */
    String GENERATE_FILTER = "generate.filter";
    /** Constant <code>GENERATE_NORMALISE="generate.normalise"</code> */
    String GENERATE_NORMALISE = "generate.normalise";
    /** Constant <code>GENERATE_MAX_TASKS_PER_HOST="generate.max.tasks.per.host"</code> */
    String GENERATE_MAX_TASKS_PER_HOST = "generate.max.tasks.per.host";
    /** Constant <code>GENERATE_SITE_GROUP_MODE="generate.count.mode"</code> */
    String GENERATE_SITE_GROUP_MODE = "generate.count.mode";
    /** Constant <code>GENERATE_TOP_N="generate.topN"</code> */
    String GENERATE_TOP_N = "generate.topN";
    /** Constant <code>GENERATE_LAST_GENERATED_ROWS="generate.last.generated.rows"</code> */
    String GENERATE_LAST_GENERATED_ROWS = "generate.last.generated.rows";
    /** Constant <code>GENERATE_CUR_TIME="generate.curr.time"</code> */
    String GENERATE_CUR_TIME = "generate.curr.time";
    /** Constant <code>GENERATE_DETAIL_PAGE_RATE="generate.detail.page.rate"</code> */
    String GENERATE_DETAIL_PAGE_RATE = "generate.detail.page.rate";
    /** Constant <code>GENERATE_DELAY="crawl.gen.delay"</code> */
    String GENERATE_DELAY = "crawl.gen.delay";
    /** Constant <code>GENERATE_RANDOM_SEED="generate.partition.seed"</code> */
    String GENERATE_RANDOM_SEED = "generate.partition.seed";
    /**
     * Parser parameters
     */
    String PARSE_PARSE = "parser.parse";
    /** Constant <code>PARSE_REPARSE="parser.reparse"</code> */
    String PARSE_REPARSE = "parser.reparse";
    /** Constant <code>PARSE_TIMEOUT="parser.timeout"</code> */
    String PARSE_TIMEOUT = "parser.timeout";
    /** Constant <code>PARSE_NORMALISE="parse.normalise"</code> */
    String PARSE_NORMALISE = "parse.normalise";
    /** Constant <code>PARSE_MAX_URL_LENGTH="parse.max.url.length"</code> */
    String PARSE_MAX_URL_LENGTH = "parse.max.url.length";
    /** Constant <code>PARSE_MIN_ANCHOR_LENGTH="parse.min.anchor.length"</code> */
    String PARSE_MIN_ANCHOR_LENGTH = "parse.min.anchor.length";
    /** Constant <code>PARSE_MAX_ANCHOR_LENGTH="parse.max.anchor.length"</code> */
    String PARSE_MAX_ANCHOR_LENGTH = "parse.max.anchor.length";
    /** Constant <code>PARSE_LINK_PATTERN="parse.link.pattern"</code> */
    String PARSE_LINK_PATTERN = "parse.link.pattern";
    /** Constant <code>PARSE_MAX_LINKS_PER_PAGE="parse.max.links"</code> */
    String PARSE_MAX_LINKS_PER_PAGE = "parse.max.links";
    /** Constant <code>PARSE_IGNORE_EXTERNAL_LINKS="parse.ignore.external.links"</code> */
    String PARSE_IGNORE_EXTERNAL_LINKS = "parse.ignore.external.links";
    /** Constant <code>PARSE_SKIP_TRUNCATED="parser.skip.truncated"</code> */
    String PARSE_SKIP_TRUNCATED = "parser.skip.truncated";
    /** Constant <code>PARSE_HTML_IMPL="parser.html.impl"</code> */
    String PARSE_HTML_IMPL = "parser.html.impl";
    /** Constant <code>PARSE_SUPPORT_ALL_CHARSETS="parser.support.all.charsets"</code> */
    String PARSE_SUPPORT_ALL_CHARSETS = "parser.support.all.charsets";
    /** Constant <code>PARSE_SUPPORTED_CHARSETS="parser.supported.charsets"</code> */
    String PARSE_SUPPORTED_CHARSETS = "parser.supported.charsets";
    /** Constant <code>PARSE_DEFAULT_ENCODING="parser.character.encoding.default"</code> */
    String PARSE_DEFAULT_ENCODING = "parser.character.encoding.default";
    /** Constant <code>PARSE_CACHING_FORBIDDEN_POLICY="parser.caching.forbidden.policy"</code> */
    String PARSE_CACHING_FORBIDDEN_POLICY = "parser.caching.forbidden.policy";
    /** Constant <code>PARSE_TIKA_HTML_MAPPER_NAME="tika.htmlmapper.classname"</code> */
    String PARSE_TIKA_HTML_MAPPER_NAME = "tika.htmlmapper.classname";

    // TODO: not used, may be caused by a git merge problem
    /** Constant <code>PARSE_RETRIEVE_FADED_LINKS="parse.retrieve.faded.links"</code> */
    String PARSE_RETRIEVE_FADED_LINKS = "parse.retrieve.faded.links";

    /**
     * DbUpdater parameters
     */
    String UPDATE_MAX_INLINKS = "update.max.inlinks";
    /** Constant <code>UPDATE_IGNORE_IN2OUT_GRAPH="update.ignore.in.graph"</code> */
    String UPDATE_IGNORE_IN2OUT_GRAPH = "update.ignore.in.graph";

    /** Constant <code>SCHEDULE_INC_RATE="db.fetch.schedule.adaptive.inc_rate"</code> */
    String SCHEDULE_INC_RATE = "db.fetch.schedule.adaptive.inc_rate";
    /** Constant <code>SCHEDULE_DEC_RATE="db.fetch.schedule.adaptive.dec_rate"</code> */
    String SCHEDULE_DEC_RATE = "db.fetch.schedule.adaptive.dec_rate";
    /** Constant <code>SCHEDULE_MIN_INTERVAL="db.fetch.schedule.adaptive.min_interval"</code> */
    String SCHEDULE_MIN_INTERVAL = "db.fetch.schedule.adaptive.min_interval";
    /** Constant <code>SCHEDULE_MAX_INTERVAL="db.fetch.schedule.adaptive.max_interval"</code> */
    String SCHEDULE_MAX_INTERVAL = "db.fetch.schedule.adaptive.max_interval";
    /** Constant <code>SCHEDULE_SEED_MAX_INTERVAL="db.fetch.schedule.adaptive.seed_max_int"{trunked}</code> */
    String SCHEDULE_SEED_MAX_INTERVAL = "db.fetch.schedule.adaptive.seed_max_interval";
    /** Constant <code>SCHEDULE_SYNC_DELTA="db.fetch.schedule.adaptive.sync_delta"</code> */
    String SCHEDULE_SYNC_DELTA = "db.fetch.schedule.adaptive.sync_delta";
    /** Constant <code>SCHEDULE_SYNC_DELTA_RATE="db.fetch.schedule.adaptive.sync_delta_r"{trunked}</code> */
    String SCHEDULE_SYNC_DELTA_RATE = "db.fetch.schedule.adaptive.sync_delta_rate";

    /**
     * Scoring
     */
    // divisor may have a better name
    String SCORE_SORT_ERROR_COUNTER_DIVISOR = "score.sort.error.counter.divisor";
    /** Constant <code>SCORE_SORT_WEB_GRAPH_SCORE_DIVISOR="score.sort.web.graph.score.divisor"</code> */
    String SCORE_SORT_WEB_GRAPH_SCORE_DIVISOR = "score.sort.web.graph.score.divisor";
    /** Constant <code>SCORE_SORT_CONTENT_SCORE_DIVISOR="score.sort.content.score.divisor"</code> */
    String SCORE_SORT_CONTENT_SCORE_DIVISOR = "score.sort.content.score.divisor";
    /**
     * Indexing parameters
     */
    String INDEXER_JIT = "indexer.just.in.time";
    /** Constant <code>INDEXER_HOSTNAME="index.server.hostname"</code> */
    String INDEXER_HOSTNAME = "index.server.hostname";
    /** Constant <code>INDEXER_PORT="index.server.port"</code> */
    String INDEXER_PORT = "index.server.port";
    /** Constant <code>INDEXER_URL="indexer.url"</code> */
    String INDEXER_URL = "indexer.url";
    /** Constant <code>INDEXER_ZK="indexer.zookeeper.hosts"</code> */
    String INDEXER_ZK = "indexer.zookeeper.hosts";
    /** Constant <code>INDEXER_COLLECTION="indexer.collection"</code> */
    String INDEXER_COLLECTION = "indexer.collection";
    /** Constant <code>INDEXER_WRITE_COMMIT_SIZE="indexer.write.commit.size"</code> */
    String INDEXER_WRITE_COMMIT_SIZE = "indexer.write.commit.size";

    /**
     * Crawl
     * */
    String SESSION_PAGE_CACHE_SIZE = "session.page.cache.size";
    /** Constant <code>SESSION_DOCUMENT_CACHE_SIZE="session.document.cache.size"</code> */
    String SESSION_DOCUMENT_CACHE_SIZE = "session.document.cache.size";

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

    /** Constant <code>NODE_FEATURE_CALCULATOR_CLASS="pulsar.node.feature.calculator"</code> */
    String NODE_FEATURE_CALCULATOR_CLASS = "pulsar.node.feature.calculator";

    /** Constant <code>PULSAR_DOMAIN="scent.domain"</code> */
    String PULSAR_DOMAIN = "scent.domain";
    /** Constant <code>SCENT_TASK_IDENT="scent.task.ident"</code> */
    String SCENT_TASK_IDENT = "scent.task.ident";
    /** Constant <code>SCENT_FILE_SERVER_HOST="scent.file.server.host"</code> */
    String SCENT_FILE_SERVER_HOST = "scent.file.server.host";
    /** Constant <code>SCENT_FILE_SERVER_PORT="scent.file.server.port"</code> */
    String SCENT_FILE_SERVER_PORT = "scent.file.server.port";

    /** Constant <code>SCENT_DIAGNOSTOR_ENABLED="scent.diagnostor.enabled"</code> */
    String SCENT_DIAGNOSTOR_ENABLED = "scent.diagnostor.enabled";

    // FEATURE
    /** Constant <code>SCENT_OUT_DIR_FEATURE="scent.out.dir.feature"</code> */
    String SCENT_OUT_DIR_FEATURE = "scent.out.dir.feature";

    // NLP
    /** Constant <code>SCENT_NLP_WORD_NET_CONCEPT="scent.nlp.word.net.concept"</code> */
    String SCENT_NLP_WORD_NET_CONCEPT = "scent.nlp.word.net.concept";
    /** Constant <code>SCENT_NLP_SEMANTIC_SIMILARITY_ENABLED="scent.nlp.semantic.similarity.enabled"</code> */
    String SCENT_NLP_SEMANTIC_SIMILARITY_ENABLED = "scent.nlp.semantic.similarity.enabled";

    // SEGMENT
    /** Constant <code>SCENT_CHILDREN_SUMMARY_ITEM_MIN="scent.children.summary.item.min"</code> */
    String SCENT_CHILDREN_SUMMARY_ITEM_MIN = "scent.children.summary.item.min";
    /** Constant <code>SCENT_CHILDREN_SUMMARY_SAMPLE_MAX="scent.children.summary.sample.max"</code> */
    String SCENT_CHILDREN_SUMMARY_SAMPLE_MAX = "scent.children.summary.sample.max";
    /** Constant <code>SCENT_CHILDREN_SUMMARY_FEATURES="scent.children.summary.features"</code> */
    String SCENT_CHILDREN_SUMMARY_FEATURES = "scent.children.summary.features";
    /** Constant <code>SCENT_CHILDREN_SUMMARY_THRESHOLD="scent.children.summary.threshold"</code> */
    String SCENT_CHILDREN_SUMMARY_THRESHOLD = "scent.children.summary.threshold";
    /** Constant <code>SCENT_CHILDREN_SUMMARY_REPORT="scent.children.summary.report"</code> */
    String SCENT_CHILDREN_SUMMARY_REPORT = "scent.children.summary.report";

    // CLASSIFY
    /** Constant <code>SCENT_CLASSIFIER_BLOCK_LABELS="scent.classifier.block.labels"</code> */
    String SCENT_CLASSIFIER_BLOCK_LABELS = "scent.classifier.block.labels";
    /** Constant <code>SCENT_DIAGNOSE_CLASSIFIER_BLOCK_LABELS="scent.diagnose.classifier.block.labels"</code> */
    String SCENT_DIAGNOSE_CLASSIFIER_BLOCK_LABELS = "scent.diagnose.classifier.block.labels";
    /** Constant <code>SCENT_CLASSIFIER_BLOCK_INHERITABLE_LABLES="scent.classifier.block.inheritable.labe"{trunked}</code> */
    String SCENT_CLASSIFIER_BLOCK_INHERITABLE_LABLES = "scent.classifier.block.inheritable.labels";
    /** Constant <code>SCENT_CLASSIFIER_WEIGHT_CODE_STRUCTURE="scent.classifier.weight.code.structure"</code> */
    String SCENT_CLASSIFIER_WEIGHT_CODE_STRUCTURE = "scent.classifier.weight.code.structure";
    /** Constant <code>SCENT_CLASSIFIER_WEIGHT_BLOCK_TEXT="scent.classifier.weight.block.text"</code> */
    String SCENT_CLASSIFIER_WEIGHT_BLOCK_TEXT = "scent.classifier.weight.block.text";
    /** Constant <code>SCENT_CLASSIFIER_WEIGHT_BLOCK_TITLE="scent.classifier.weight.block.title"</code> */
    String SCENT_CLASSIFIER_WEIGHT_BLOCK_TITLE = "scent.classifier.weight.block.title";

    // EXTRACT
    /** Constant <code>SCENT_EXTRACT_EXTRACT_FOR_LABEL="scent.extract.extractor.for.label"</code> */
    String SCENT_EXTRACT_EXTRACT_FOR_LABEL = "scent.extract.extractor.for.label";
    /** Constant <code>SCENT_EXTRACT_REFRESH_FEATURE="scent.extract.refresh.feature"</code> */
    String SCENT_EXTRACT_REFRESH_FEATURE = "scent.extract.refresh.feature";
    /** Constant <code>SCENT_EXTRACT_GREEDY="scent.extract.greedy"</code> */
    String SCENT_EXTRACT_GREEDY = "scent.extract.greedy";
    /** Constant <code>SCENT_EXTRACT_KEEP_ELEMENT_METADATA="scent.extract.keep.element.metadata"</code> */
    String SCENT_EXTRACT_KEEP_ELEMENT_METADATA = "scent.extract.keep.element.metadata";
    /** Constant <code>SCENT_EXTRACT_TABULATE_CELL_TYPE="scent.extract.tabulate.cell.type"</code> */
    String SCENT_EXTRACT_TABULATE_CELL_TYPE = "scent.extract.tabulate.cell.type";

    // BUILD
    /** Constant <code>SCENT_WIKI_DOMAIN="scent.wiki.domain"</code> */
    String SCENT_WIKI_DOMAIN = "scent.wiki.domain";
    /** Constant <code>SCENT_WIKI_USERNAME="scent.wiki.username"</code> */
    String SCENT_WIKI_USERNAME = "scent.wiki.username";
    /** Constant <code>SCENT_WIKI_PASSWORD="scent.wiki.password"</code> */
    String SCENT_WIKI_PASSWORD = "scent.wiki.password";

    // Spark
    /** Constant <code>SPARK_MASTER="spark.master"</code> */
    String SPARK_MASTER = "spark.master";

    // H2
    /** Constant <code>H2_SESSION_FACTORY_CLASS="h2.sessionFactory"</code> */
    String H2_SESSION_FACTORY_CLASS = "h2.sessionFactory";
}
