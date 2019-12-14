/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
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

import org.apache.commons.lang3.SystemUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;

/**
 * A collection of constants.
 */
@SuppressWarnings("unused")
public interface AppConstants {

    // String USER = System.getenv("USER");
    String USER = SystemUtils.USER_NAME;

    String APP_CONTEXT_CONFIG_LOCATION = "classpath:/pulsar-beans/app-context.xml";
    String JOB_CONTEXT_CONFIG_LOCATION = "classpath:/pulsar-beans/job-context.xml";

    String YES_STRING = "y";

    /** Don't show original forbidden content, but show summaries. */
    String CACHING_FORBIDDEN_CONTENT = "content";

    String ALL_BATCHES = "all";

    String ALL_BATCH_ID_STR = "-all";

    // see https://en.wikipedia.org/wiki/UTF-8
    Character UNICODE_FIRST_CODE_POINT = '\u0001';
    Character UNICODE_LAST_CODE_POINT = '\uFFFF';

    int DISTANCE_INFINITE = 10000;

    int FETCH_TASK_REMAINDER_NUMBER = 5;

    Instant TCP_IP_STANDARDIZED_TIME = Instant.parse("1982-01-01T00:00:00Z");

    Instant MIN_ARTICLE_PUBLISH_TIME = Instant.parse("1995-01-01T00:00:00Z");

    Duration DEFAULT_MAX_PARSE_TIME = Duration.ofSeconds(30);

    // The shortest url
    String SHORTEST_VALID_URL = "ftp://t.tt";
    int SHORTEST_VALID_URL_LENGTH = SHORTEST_VALID_URL.length();
    String EXAMPLE_URL = "http://example.com";
    String EMPTY_PAGE_URL = "http://example.com/empty";
    String NIL_PAGE_URL = "http://example.com/nil";

    String SEED_HOME_URL = "http://pulsar.platon.ai/seeds";
    String SEED_PAGE_1_URL = "http://pulsar.platon.ai/seeds/1";
    String TOP_PAGE_HOME_URL = "http://pulsar.platon.ai/top";
    String TOP_PAGE_PAGE_1_URL = "http://pulsar.platon.ai/top/1";
    String BACKGROUND_TASK_PAGE_HOME_URL = "http://pulsar.platon.ai/tmp/tasks";
    String BACKGROUND_TASK_PAGE_PAGE_1_URL = "http://pulsar.platon.ai/tmp/tasks/1";

    String URL_TRACKER_HOME_URL = "http://pulsar.platon.ai/url/tracker";
    String URL_TRACKER_PAGE_1_URL = "http://pulsar.platon.ai/url/tracker/1";

    String METRICS_HOME_URL = "http://pulsar.platon.ai/metrics";
    String METRICS_PAGE_1_URL = "http://pulsar.platon.ai/metrics/1";

    String CRAWL_LOG_HOME_URL = "http://pulsar.platon.ai/metrics";
    String CRAWL_LOG_INDEX_URL = "http://pulsar.platon.ai/metrics";
    String CRAWL_LOG_PAGE_1_URL = "http://pulsar.platon.ai/metrics/1";

    /**
     * Storage
     * */
    String MEM_STORE_CLASS = "org.apache.gora.memory.store.MemStore";
    String TOY_STORE_CLASS = "ai.platon.pulsar.persist.gora.ToyFileStore";
    String MONGO_STORE_CLASS = "org.apache.gora.mongodb.store.MongoStore";
    String HBASE_STORE_CLASS = "org.apache.gora.hbase.store.HBaseStore";

    int FETCH_PRIORITY_MIN = -10 * 10_000;
    int FETCH_PRIORITY_ANY = -1;
    int FETCH_PRIORITY_DEFAULT = 10_000;
    int FETCH_PRIORITY_DEPTH_BASE = 20_000;
    int FETCH_PRIORITY_DEPTH_0 = FETCH_PRIORITY_DEPTH_BASE;
    int FETCH_PRIORITY_DEPTH_1 = FETCH_PRIORITY_DEPTH_BASE - 1;
    int FETCH_PRIORITY_DEPTH_2 = FETCH_PRIORITY_DEPTH_BASE - 2;
    int FETCH_PRIORITY_DEPTH_3 = FETCH_PRIORITY_DEPTH_BASE - 3;
    int FETCH_PRIORITY_EMERGENT_INJECT = 30_000;
    int FETCH_PRIORITY_MAX = 10 * 10_000;

    String CRAWL_DEPTH_FIRST = "depthFirst";
    String CRAWL_STRICT_DEPTH_FIRST = "strictDepthFirst";

    int PERM_REFRESH_TIME = 5;

    float SCORE_DEFAULT = 1.0f;
    float SCORE_INDEX_PAGE = 1.0f;
    float SCORE_SEED = 1.0f;
    float SCORE_INJECTED = Float.MAX_VALUE / 1000;
    float SCORE_DETAIL_PAGE = 10000.0f;
    float SCORE_PAGES_FROM_SEED = 10000.0f;

    String SCENT_PSEUDO_CSS_ID_MENU = "#scent-pseudo-id-menu";
    String SCENT_PSEUDO_CSS_ID_TITLE = "#scent-pseudo-id-title";

    int MAX_LIVE_LINK_PER_PAGE = 1000;
    int MAX_LINK_PER_PAGE = 4000;

    /**
     * Local file commands
     * */
    String CMD_FORCE_GENERATE_SEEDS = "force-generate-seeds";
    String CMD_ENABLE_PROXY = "use_proxy"; // keep consistent with linux system variable
    String CMD_PROXY_POOL_DUMP = "dump-proxy-pool";

    String CMD_INTERNAL_PROXY_SERVER_FORCE_IDLE = "force-IPS-idle";
    String CMD_INTERNAL_PROXY_SERVER_RECONNECT = "IPS-reconnect";

    String CMD_WEB_DRIVER_CLOSE_ALL = "close-all-web-drivers";
    String CMD_WEB_DRIVER_DELETE_ALL_COOKIES = "delete-all-cookies";

    int BANDWIDTH_INFINITE_M = 10000; // bandwidth in M bits

    /**
     * Index
     * */
    String INDEXER_WRITE_COMMIT_SIZE = "indexer.write.commit.size";

    String DEFAULT_NODE_FEATURE_CALCULATOR = "ai.platon.pulsar.dom.features.NodeFeatureCalculator";

    String DEFAULT_PULSAR_MASTER_HOST = "0.0.0.0";
    int DEFAULT_PULSAR_MASTER_PORT = 8182;

    String DEFAULT_INDEX_SERVER_HOSTNAME = "master";
    int DEFAULT_INDEX_SERVER_PORT = 8183;

    String DEFAULT_EMBED_MONGO_SERVER = "127.0.0.1:27017";

    /**
     * Other notable properties:
     * overflow
     * text-overflow
     * */
    String CLIENT_JS_PROPERTY_NAMES = "font-size, color, background-color";

    /**
     * Directories
     * */
    String TMP_DIR = SystemUtils.JAVA_IO_TMPDIR;
    String HOME_DIR = SystemUtils.USER_HOME;

    Path PULSAR_DEFAULT_TMP_DIR = Paths.get(TMP_DIR, "pulsar-" + USER);
    Path PULSAR_DEFAULT_DATA_DIR = Paths.get(HOME_DIR, ".pulsar");

    String PULSAR_META_INFORMATION_ID = "PulsarMetaInformation";
    String PULSAR_SCRIPT_SECTION_ID = "PulsarScriptSection";
    String PULSAR_ATTR_HIDDEN = "_h";
    String PULSAR_ATTR_OVERFLOW_HIDDEN = "_oh";
    String PULSAR_ATTR_OVERFLOW_VISIBLE = "_visible";

    /**
     * Proxy
     * */
    int INTERNAL_PROXY_SERVER_PORT = 8184;

    /**
     * SQL engine
     * */
    String H2_SESSION_FACTORY = "ai.platon.pulsar.ql.h2.H2SessionFactory";
}
