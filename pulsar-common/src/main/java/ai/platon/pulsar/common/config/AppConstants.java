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
import ai.platon.pulsar.common.measure.ByteUnit;

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

    String PULSAR_CONTEXT_CONFIG_LOCATION = "classpath*:/pulsar-beans/app-context.xml";
    String SCENT_CONTEXT_CONFIG_LOCATION = "classpath*:/scent-beans/app-context.xml";
    String MAPR_CONTEXT_CONFIG_LOCATION = "classpath*:/mapr/mapr-beans/mapr-context.xml";

    String YES_STRING = "y";

    /** Don't show original forbidden content, but show summaries. */
    String CACHING_FORBIDDEN_CONTENT = "content";

    String ALL_BATCHES = "all";

    String ALL_BATCH_ID_STR = "-all";

    /**
     * The first unicode character
     * @link <a href="https://en.wikipedia.org/wiki/UTF-8">...</a>
     * */
    Character UNICODE_FIRST_CODE_POINT = '\u0001';
    /**
     * The last unicode character
     * @link <a href="https://en.wikipedia.org/wiki/UTF-8">...</a>
     * */
    Character UNICODE_LAST_CODE_POINT = '\uFFFF';

    /**
     * An example of the shortest url
     * */
    String SHORTEST_VALID_URL = "http://t.tt";
    /**
     * The length of the shortest example url
     * */
    int SHORTEST_VALID_URL_LENGTH = SHORTEST_VALID_URL.length();
    /**
     * The example url
     * */
    String EXAMPLE_URL = "http://example.com";
    /**
     * A pseudo url can not be fetched directly, but the crawl events are handled
     * */
    String PSEUDO_URL_BASE = "http://www.pseudo.com";
    /**
     * The prefix of all pulsar specified urls
     * */
    String INTERNAL_URL_PREFIX = "http://internal.pulsar.platon.ai";
    /**
     * The url of the empty page
     * */
    String EMPTY_PAGE_URL = INTERNAL_URL_PREFIX + "/empty";
    /**
     * The url of the nil page
     * */
    String NIL_PAGE_URL = INTERNAL_URL_PREFIX + "/nil";
    /**
     * The url of the seed home page
     * */
    String SEED_HOME_URL = INTERNAL_URL_PREFIX + "/seeds";
    /**
     * The url of the seed home page
     * */
    String SEED_PAGE_1_URL = INTERNAL_URL_PREFIX + "/seeds/1";
    /**
     * The url of the top page
     * */
    String TOP_PAGE_HOME_URL = INTERNAL_URL_PREFIX + "/top";
    /**
     * The url of the top page
     * */
    String TOP_PAGE_PAGE_1_URL = INTERNAL_URL_PREFIX + "/top/1";
    String BACKGROUND_TASK_PAGE_HOME_URL = INTERNAL_URL_PREFIX + "/tmp/tasks";
    String BACKGROUND_TASK_PAGE_PAGE_1_URL = INTERNAL_URL_PREFIX + "/tmp/tasks/1";

    String URL_TRACKER_HOME_URL = INTERNAL_URL_PREFIX + "/url/tracker";
    String URL_TRACKER_PAGE_1_URL = INTERNAL_URL_PREFIX + "/url/tracker/1";

    String METRICS_HOME_URL = INTERNAL_URL_PREFIX + "/metrics";
    String METRICS_PAGE_1_URL = INTERNAL_URL_PREFIX + "/metrics/1";

    String CRAWL_LOG_HOME_URL = INTERNAL_URL_PREFIX + "/metrics";
    String CRAWL_LOG_INDEX_URL = INTERNAL_URL_PREFIX + "/metrics";
    String CRAWL_LOG_PAGE_1_URL = INTERNAL_URL_PREFIX + "/metrics/1";

    /**
     * Storage
     * */
    String MEM_STORE_CLASS = "org.apache.gora.memory.store.MemStore";
    /** A minimal file backend store */
    String FILE_BACKEND_STORE_CLASS = "ai.platon.pulsar.persist.gora.FileBackendPageStore";
    String MONGO_STORE_CLASS = "org.apache.gora.mongodb.store.MongoStore";
    String HBASE_STORE_CLASS = "org.apache.gora.hbase.store.HBaseStore";
    // schema version 1.10.x
//    String WEBPAGE_SCHEMA = "webpage110";
    /**
     * Schema version 1.12.x, has to be compatible with webpage110
     * */
    String WEBPAGE_SCHEMA = "webpage120";

    /**
     * Fetch
     * */
    int DISTANCE_INFINITE = 10000;
    int FETCH_TASK_REMAINDER_NUMBER = 5;
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
    int FETCH_THREADS = AppContext.INSTANCE.getNCPU();
    Duration FETCH_TASK_TIMEOUT_DEFAULT = Duration.ofMinutes(10);

    String CRAWL_DEPTH_FIRST = "depthFirst";
    String CRAWL_STRICT_DEPTH_FIRST = "strictDepthFirst";

    int PERM_REFRESH_TIME = 5;

    float SCORE_DEFAULT = 1.0f;
    float SCORE_INDEX_PAGE = 1.0f;
    float SCORE_SEED = 1.0f;
    float SCORE_INJECTED = Float.MAX_VALUE / 1000;
    float SCORE_DETAIL_PAGE = 10000.0f;
    float SCORE_PAGES_FROM_SEED = 10000.0f;

    /**
     * Parse
     * */
    Instant TCP_IP_STANDARDIZED_TIME = Instant.parse("1982-01-01T00:00:00Z");
    Instant MIN_ARTICLE_PUBLISH_TIME = Instant.parse("1995-01-01T00:00:00Z");
    Duration DEFAULT_MAX_PARSE_TIME = Duration.ofSeconds(60);

    String DEFAULT_NODE_FEATURE_CALCULATOR = "ai.platon.pulsar.dom.features.NodeFeatureCalculator";

    int MAX_LIVE_LINK_PER_PAGE = 1000;
    int MAX_LINK_PER_PAGE = 4000;

    /**
     * Local file commands
     * */
    String CMD_FORCE_GENERATE_SEEDS = "force-generate-seeds";
    String CMD_PROXY_POOL_DUMP = "dump-proxy-pool";

    String CMD_PROXY_FORCE_IDLE = "IPS-force-idle";
    String CMD_PROXY_RECONNECT = "IPS-reconnect";
    String CMD_PROXY_DISCONNECT = "IPS-disconnect";

    String CMD_WEB_DRIVER_CLOSE_ALL = "close-all-web-drivers";
    String CMD_WEB_DRIVER_DELETE_ALL_COOKIES = "delete-all-cookies";

    int BANDWIDTH_INFINITE_M = 10000; // bandwidth in M bits

    /**
     * Index
     * */
    String INDEXER_WRITE_COMMIT_SIZE = "indexer.write.commit.size";

    String DEFAULT_INDEX_SERVER_HOSTNAME = "master";
    int DEFAULT_INDEX_SERVER_PORT = 8183;

    /**
     * Browser
     * */
    int DEFAULT_BROWSER_MAX_ACTIVE_TABS = 8;

    double BROWSER_TAB_REQUIRED_MEMORY = ByteUnit.GIB.toBytes(1.5); // at least 1.5 GiB to open a new tab

    double DEFAULT_BROWSER_RESERVED_MEMORY = ByteUnit.GIB.toBytes(2.0); // 3 GiB

    double DEFAULT_BROWSER_RESERVED_MEMORY_MIB = ByteUnit.BYTE.toMiB(DEFAULT_BROWSER_RESERVED_MEMORY); // 5 GiB

    Dimension DEFAULT_VIEW_PORT = new Dimension(1920, 1080);
    String PULSAR_META_INFORMATION_ID = "PulsarMetaInformation";
    String PULSAR_META_INFORMATION_SELECTOR = "#" + PULSAR_META_INFORMATION_ID;
    String PULSAR_SCRIPT_SECTION_ID = "PulsarScriptSection";
    String PULSAR_SCRIPT_SECTION_SELECTOR = "#" + PULSAR_SCRIPT_SECTION_ID;
    String PULSAR_DOCUMENT_NORMALIZED_URI = "normalizedURI";
    String PULSAR_ATTR_HIDDEN = "_h";
    String PULSAR_ATTR_OVERFLOW_HIDDEN = "_oh";
    String PULSAR_ATTR_OVERFLOW_VISIBLE = "_visible";
    String PULSAR_ATTR_ELEMENT_NODE_VI = "vi";
    String PULSAR_ATTR_TEXT_NODE_VI = "tv";
    /**
     * @deprecated Use PULSAR_ATTR_COMPUTED_STYLE instead
     * */
    @Deprecated
    String PULSAR_ATTR_ELEMENT_NODE_STYLE = "st";
    String PULSAR_ATTR_COMPUTED_STYLE = "cs";

    /**
     * Other notable properties:
     * overflow
     * text-overflow
     * */
    String CLIENT_JS_PROPERTY_NAMES = "font-size, color, background-color";

    /**
     * Metrics
     * */
    String DEFAULT_METRICS_NAME = "pulsar";

    /**
     * SQL engine
     * */
    String H2_SESSION_FACTORY = "ai.platon.pulsar.ql.h2.H2SessionFactory";

    //////////////////////////////////////////////////////////
    // Scent

    String SCENT_PSEUDO_CSS_ID_MENU = "#scent-pseudo-id-menu";
    String SCENT_PSEUDO_CSS_ID_TITLE = "#scent-pseudo-id-title";


    String LOCAL_FILE_SERVE_PREFIX = "http://localfile.org";

    String BROWSER_SPECIFIC_URL_PREFIX = "http://browser-specific.org";
}
