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

import ai.platon.pulsar.common.AppContext;

import java.awt.*;
import java.time.Duration;
import java.time.Instant;

/**
 * A collection of constants.
 *
 * @author vincent
 * @version $Id: $Id
 */
@SuppressWarnings("unused")
public interface AppConstants {

    /** Constant <code>APP_CONTEXT_CONFIG_LOCATION="classpath*:/pulsar-beans/app-context.xm"{trunked}</code> */
    String APP_CONTEXT_CONFIG_LOCATION = "classpath*:/pulsar-beans/app-context.xml";
    /** Constant <code>SCENT_CONTEXT_CONFIG_LOCATION="classpath*:/scent-beans/app-context.xml"</code> */
    String SCENT_CONTEXT_CONFIG_LOCATION = "classpath*:/scent-beans/app-context.xml";
    /** Constant <code>MAPR_CONTEXT_CONFIG_LOCATION="classpath*:/mapr/mapr-beans/mapr-contex"{trunked}</code> */
    String MAPR_CONTEXT_CONFIG_LOCATION = "classpath*:/mapr/mapr-beans/mapr-context.xml";

    /** Constant <code>YES_STRING="y"</code> */
    String YES_STRING = "y";

    /** Don't show original forbidden content, but show summaries. */
    String CACHING_FORBIDDEN_CONTENT = "content";

    /** Constant <code>ALL_BATCHES="all"</code> */
    String ALL_BATCHES = "all";

    /** Constant <code>ALL_BATCH_ID_STR="-all"</code> */
    String ALL_BATCH_ID_STR = "-all";

    // see https://en.wikipedia.org/wiki/UTF-8
    /** Constant <code>UNICODE_FIRST_CODE_POINT</code> */
    Character UNICODE_FIRST_CODE_POINT = '\u0001';
    /** Constant <code>UNICODE_LAST_CODE_POINT</code> */
    Character UNICODE_LAST_CODE_POINT = '\uFFFF';

    // The shortest url
    /** Constant <code>SHORTEST_VALID_URL="ftp://t.tt"</code> */
    String SHORTEST_VALID_URL = "ftp://t.tt";
    /** Constant <code>SHORTEST_VALID_URL_LENGTH=SHORTEST_VALID_URL.length()</code> */
    int SHORTEST_VALID_URL_LENGTH = SHORTEST_VALID_URL.length();
    /** Constant <code>EXAMPLE_URL="http://example.com"</code> */
    String EXAMPLE_URL = "http://example.com";

    /** Constant <code>INTERNAL_URL_PREFIX="http://internal.pulsar.platon.ai"</code> */
    String INTERNAL_URL_PREFIX = "http://internal.pulsar.platon.ai";
    /** Constant <code>EMPTY_PAGE_URL="INTERNAL_URL_PREFIX + /empty"</code> */
    String EMPTY_PAGE_URL = INTERNAL_URL_PREFIX + "/empty";
    /** Constant <code>NIL_PAGE_URL="INTERNAL_URL_PREFIX + /nil"</code> */
    String NIL_PAGE_URL = INTERNAL_URL_PREFIX + "/nil";
    /** Constant <code>SEED_HOME_URL="INTERNAL_URL_PREFIX + /seeds"</code> */
    String SEED_HOME_URL = INTERNAL_URL_PREFIX + "/seeds";
    /** Constant <code>SEED_PAGE_1_URL="INTERNAL_URL_PREFIX + /seeds/1"</code> */
    String SEED_PAGE_1_URL = INTERNAL_URL_PREFIX + "/seeds/1";
    /** Constant <code>TOP_PAGE_HOME_URL="INTERNAL_URL_PREFIX + /top"</code> */
    String TOP_PAGE_HOME_URL = INTERNAL_URL_PREFIX + "/top";
    /** Constant <code>TOP_PAGE_PAGE_1_URL="INTERNAL_URL_PREFIX + /top/1"</code> */
    String TOP_PAGE_PAGE_1_URL = INTERNAL_URL_PREFIX + "/top/1";
    /** Constant <code>BACKGROUND_TASK_PAGE_HOME_URL="INTERNAL_URL_PREFIX + /tmp/tasks"</code> */
    String BACKGROUND_TASK_PAGE_HOME_URL = INTERNAL_URL_PREFIX + "/tmp/tasks";
    /** Constant <code>BACKGROUND_TASK_PAGE_PAGE_1_URL="INTERNAL_URL_PREFIX + /tmp/tasks/1"</code> */
    String BACKGROUND_TASK_PAGE_PAGE_1_URL = INTERNAL_URL_PREFIX + "/tmp/tasks/1";

    /** Constant <code>URL_TRACKER_HOME_URL="INTERNAL_URL_PREFIX + /url/tracker"</code> */
    String URL_TRACKER_HOME_URL = INTERNAL_URL_PREFIX + "/url/tracker";
    /** Constant <code>URL_TRACKER_PAGE_1_URL="INTERNAL_URL_PREFIX + /url/tracker/1"</code> */
    String URL_TRACKER_PAGE_1_URL = INTERNAL_URL_PREFIX + "/url/tracker/1";

    /** Constant <code>METRICS_HOME_URL="INTERNAL_URL_PREFIX + /metrics"</code> */
    String METRICS_HOME_URL = INTERNAL_URL_PREFIX + "/metrics";
    /** Constant <code>METRICS_PAGE_1_URL="INTERNAL_URL_PREFIX + /metrics/1"</code> */
    String METRICS_PAGE_1_URL = INTERNAL_URL_PREFIX + "/metrics/1";

    /** Constant <code>CRAWL_LOG_HOME_URL="INTERNAL_URL_PREFIX + /metrics"</code> */
    String CRAWL_LOG_HOME_URL = INTERNAL_URL_PREFIX + "/metrics";
    /** Constant <code>CRAWL_LOG_INDEX_URL="INTERNAL_URL_PREFIX + /metrics"</code> */
    String CRAWL_LOG_INDEX_URL = INTERNAL_URL_PREFIX + "/metrics";
    /** Constant <code>CRAWL_LOG_PAGE_1_URL="INTERNAL_URL_PREFIX + /metrics/1"</code> */
    String CRAWL_LOG_PAGE_1_URL = INTERNAL_URL_PREFIX + "/metrics/1";

    /**
     * Storage
     * */
    String MEM_STORE_CLASS = "org.apache.gora.memory.store.MemStore";
    /** Constant <code>MONGO_STORE_CLASS="org.apache.gora.mongodb.store.MongoStor"{trunked}</code> */
    String MONGO_STORE_CLASS = "org.apache.gora.mongodb.store.MongoStore";
    /** Constant <code>HBASE_STORE_CLASS="org.apache.gora.hbase.store.HBaseStore"</code> */
    String HBASE_STORE_CLASS = "org.apache.gora.hbase.store.HBaseStore";

    /**
     * Fetch
     * */
    int DISTANCE_INFINITE = 10000;
    /** Constant <code>FETCH_TASK_REMAINDER_NUMBER=5</code> */
    int FETCH_TASK_REMAINDER_NUMBER = 5;
    /** Constant <code>FETCH_PRIORITY_MIN=-10 * 10_000</code> */
    int FETCH_PRIORITY_MIN = -10 * 10_000;
    /** Constant <code>FETCH_PRIORITY_ANY=-1</code> */
    int FETCH_PRIORITY_ANY = -1;
    /** Constant <code>FETCH_PRIORITY_DEFAULT=10_000</code> */
    int FETCH_PRIORITY_DEFAULT = 10_000;
    /** Constant <code>FETCH_PRIORITY_DEPTH_BASE=20_000</code> */
    int FETCH_PRIORITY_DEPTH_BASE = 20_000;
    /** Constant <code>FETCH_PRIORITY_DEPTH_0=FETCH_PRIORITY_DEPTH_BASE</code> */
    int FETCH_PRIORITY_DEPTH_0 = FETCH_PRIORITY_DEPTH_BASE;
    /** Constant <code>FETCH_PRIORITY_DEPTH_1=FETCH_PRIORITY_DEPTH_BASE - 1</code> */
    int FETCH_PRIORITY_DEPTH_1 = FETCH_PRIORITY_DEPTH_BASE - 1;
    /** Constant <code>FETCH_PRIORITY_DEPTH_2=FETCH_PRIORITY_DEPTH_BASE - 2</code> */
    int FETCH_PRIORITY_DEPTH_2 = FETCH_PRIORITY_DEPTH_BASE - 2;
    /** Constant <code>FETCH_PRIORITY_DEPTH_3=FETCH_PRIORITY_DEPTH_BASE - 3</code> */
    int FETCH_PRIORITY_DEPTH_3 = FETCH_PRIORITY_DEPTH_BASE - 3;
    /** Constant <code>FETCH_PRIORITY_EMERGENT_INJECT=30_000</code> */
    int FETCH_PRIORITY_EMERGENT_INJECT = 30_000;
    /** Constant <code>FETCH_PRIORITY_MAX=10 * 10_000</code> */
    int FETCH_PRIORITY_MAX = 10 * 10_000;
    /** Constant <code>FETCH_THREADS=AppContext.INSTANCE.getNCPU()</code> */
    int FETCH_THREADS = AppContext.INSTANCE.getNCPU();

    /** Constant <code>CRAWL_DEPTH_FIRST="depthFirst"</code> */
    String CRAWL_DEPTH_FIRST = "depthFirst";
    /** Constant <code>CRAWL_STRICT_DEPTH_FIRST="strictDepthFirst"</code> */
    String CRAWL_STRICT_DEPTH_FIRST = "strictDepthFirst";

    /** Constant <code>PERM_REFRESH_TIME=5</code> */
    int PERM_REFRESH_TIME = 5;

    /** Constant <code>SCORE_DEFAULT=1.0f</code> */
    float SCORE_DEFAULT = 1.0f;
    /** Constant <code>SCORE_INDEX_PAGE=1.0f</code> */
    float SCORE_INDEX_PAGE = 1.0f;
    /** Constant <code>SCORE_SEED=1.0f</code> */
    float SCORE_SEED = 1.0f;
    /** Constant <code>SCORE_INJECTED=Float.MAX_VALUE / 1000</code> */
    float SCORE_INJECTED = Float.MAX_VALUE / 1000;
    /** Constant <code>SCORE_DETAIL_PAGE=10000.0f</code> */
    float SCORE_DETAIL_PAGE = 10000.0f;
    /** Constant <code>SCORE_PAGES_FROM_SEED=10000.0f</code> */
    float SCORE_PAGES_FROM_SEED = 10000.0f;

    /**
     * Parse
     * */
    Instant TCP_IP_STANDARDIZED_TIME = Instant.parse("1982-01-01T00:00:00Z");
    /** Constant <code>MIN_ARTICLE_PUBLISH_TIME</code> */
    Instant MIN_ARTICLE_PUBLISH_TIME = Instant.parse("1995-01-01T00:00:00Z");
    /** Constant <code>DEFAULT_MAX_PARSE_TIME</code> */
    Duration DEFAULT_MAX_PARSE_TIME = Duration.ofSeconds(60);
    /** Constant <code>SCENT_PSEUDO_CSS_ID_MENU="#scent-pseudo-id-menu"</code> */
    String SCENT_PSEUDO_CSS_ID_MENU = "#scent-pseudo-id-menu";
    /** Constant <code>SCENT_PSEUDO_CSS_ID_TITLE="#scent-pseudo-id-title"</code> */
    String SCENT_PSEUDO_CSS_ID_TITLE = "#scent-pseudo-id-title";

    /** Constant <code>DEFAULT_NODE_FEATURE_CALCULATOR="ai.platon.pulsar.dom.features.NodeFeatu"{trunked}</code> */
    String DEFAULT_NODE_FEATURE_CALCULATOR = "ai.platon.pulsar.dom.features.NodeFeatureCalculator";

    /** Constant <code>MAX_LIVE_LINK_PER_PAGE=1000</code> */
    int MAX_LIVE_LINK_PER_PAGE = 1000;
    /** Constant <code>MAX_LINK_PER_PAGE=4000</code> */
    int MAX_LINK_PER_PAGE = 4000;

    /**
     * Local file commands
     * */
    String CMD_FORCE_GENERATE_SEEDS = "force-generate-seeds";
    /** Constant <code>CMD_PROXY_POOL_DUMP="dump-proxy-pool"</code> */
    String CMD_PROXY_POOL_DUMP = "dump-proxy-pool";

    /** Constant <code>CMD_PROXY_FORCE_IDLE="IPS-force-idle"</code> */
    String CMD_PROXY_FORCE_IDLE = "IPS-force-idle";
    /** Constant <code>CMD_PROXY_RECONNECT="IPS-reconnect"</code> */
    String CMD_PROXY_RECONNECT = "IPS-reconnect";
    /** Constant <code>CMD_PROXY_DISCONNECT="IPS-disconnect"</code> */
    String CMD_PROXY_DISCONNECT = "IPS-disconnect";

    /** Constant <code>CMD_WEB_DRIVER_CLOSE_ALL="close-all-web-drivers"</code> */
    String CMD_WEB_DRIVER_CLOSE_ALL = "close-all-web-drivers";
    /** Constant <code>CMD_WEB_DRIVER_DELETE_ALL_COOKIES="delete-all-cookies"</code> */
    String CMD_WEB_DRIVER_DELETE_ALL_COOKIES = "delete-all-cookies";

    /** Constant <code>BANDWIDTH_INFINITE_M=10000</code> */
    int BANDWIDTH_INFINITE_M = 10000; // bandwidth in M bits

    /**
     * Index
     * */
    String INDEXER_WRITE_COMMIT_SIZE = "indexer.write.commit.size";

    /** Constant <code>DEFAULT_PULSAR_MASTER_HOST="0.0.0.0"</code> */
    String DEFAULT_PULSAR_MASTER_HOST = "0.0.0.0";
    /** Constant <code>DEFAULT_PULSAR_MASTER_PORT=8182</code> */
    int DEFAULT_PULSAR_MASTER_PORT = 8182;

    /** Constant <code>DEFAULT_INDEX_SERVER_HOSTNAME="master"</code> */
    String DEFAULT_INDEX_SERVER_HOSTNAME = "master";
    /** Constant <code>DEFAULT_INDEX_SERVER_PORT=8183</code> */
    int DEFAULT_INDEX_SERVER_PORT = 8183;

    /**
     * Browser
     * */
    int BROWSER_DRIVER_INSTANCE_REQUIRED_MEMORY = 500 * 1024 * 1024; // 500 MiB

    /** Constant <code>DEFAULT_VIEW_PORT</code> */
    Dimension DEFAULT_VIEW_PORT = new Dimension(1920, 1080);
    /** Constant <code>PULSAR_META_INFORMATION_ID="PulsarMetaInformation"</code> */
    String PULSAR_META_INFORMATION_ID = "PulsarMetaInformation";
    /** Constant <code>PULSAR_SCRIPT_SECTION_ID="PulsarScriptSection"</code> */
    String PULSAR_SCRIPT_SECTION_ID = "PulsarScriptSection";
    /** Constant <code>PULSAR_ATTR_HIDDEN="_h"</code> */
    String PULSAR_ATTR_HIDDEN = "_h";
    /** Constant <code>PULSAR_ATTR_OVERFLOW_HIDDEN="_oh"</code> */
    String PULSAR_ATTR_OVERFLOW_HIDDEN = "_oh";
    /** Constant <code>PULSAR_ATTR_OVERFLOW_VISIBLE="_visible"</code> */
    String PULSAR_ATTR_OVERFLOW_VISIBLE = "_visible";
    /**
     * Other notable properties:
     * overflow
     * text-overflow
     * */
    String CLIENT_JS_PROPERTY_NAMES = "font-size, color, background-color";

    /**
     * Proxy
     * */
    int PROXY_SERVER_PORT_BASE = 8584;

    /**
     * Metrics
     * */
    String DEFAULT_METRICS_NAME = "pulsar";

    /**
     * SQL engine
     * */
    String H2_SESSION_FACTORY = "ai.platon.pulsar.ql.h2.H2SessionFactory";
}
