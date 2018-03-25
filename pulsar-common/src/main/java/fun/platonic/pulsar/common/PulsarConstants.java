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
package fun.platonic.pulsar.common;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;

/**
 * A collection of constants.
 */
public interface PulsarConstants {

    String APP_CONTEXT_CONFIG_LOCATION = "classpath:/pulsar-beans/app-context.xml";
    String JOB_CONTEXT_CONFIG_LOCATION = "classpath:/pulsar-beans/job-context.xml";

    String YES_STRING = "y";

    /** Don't show original forbidden content, but show summaries. */
    String CACHING_FORBIDDEN_CONTENT = "content";

    String ALL_BATCHES = "all";

    // see https://en.wikipedia.org/wiki/UTF-8
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

    String SEED_HOME_URL = "http://pulsar.warps.org/seeds";
    String SEED_PAGE_1_URL = "http://pulsar.warps.org/seeds/1";
    String TOP_PAGE_HOME_URL = "http://pulsar.warps.org/top";
    String TOP_PAGE_PAGE_1_URL = "http://pulsar.warps.org/top/1";
    String BACKGROUND_TASK_PAGE_HOME_URL = "http://pulsar.warps.org/tmp/tasks";
    String BACKGROUND_TASK_PAGE_PAGE_1_URL = "http://pulsar.warps.org/tmp/tasks/1";

    String URL_TRACKER_HOME_URL = "http://pulsar.warps.org/url/tracker";
    String URL_TRACKER_PAGE_1_URL = "http://pulsar.warps.org/url/tracker/1";

    String METRICS_HOME_URL = "http://pulsar.warps.org/metrics";
    String METRICS_PAGE_1_URL = "http://pulsar.warps.org/metrics/1";

    String CRAWL_LOG_HOME_URL = "http://pulsar.warps.org/metrics";
    String CRAWL_LOG_INDEX_URL = "http://pulsar.warps.org/metrics";
    String CRAWL_LOG_PAGE_1_URL = "http://pulsar.warps.org/metrics/1";

    /**
     * Storage
     * */
    String MEM_STORE_CLASS = "org.apache.gora.memory.store.MemStore";
    String HBASE_STORE_CLASS = "org.apache.gora.hbase.store.HBaseStore";

    int FETCH_PRIORITY_MIN = -10 * 1000;
    int FETCH_PRIORITY_ANY = -1;
    int FETCH_PRIORITY_DEFAULT = 1000;
    int FETCH_PRIORITY_DEPTH_BASE = 2000;
    int FETCH_PRIORITY_DEPTH_0 = FETCH_PRIORITY_DEPTH_BASE;
    int FETCH_PRIORITY_DEPTH_1 = FETCH_PRIORITY_DEPTH_BASE - 1;
    int FETCH_PRIORITY_DEPTH_2 = FETCH_PRIORITY_DEPTH_BASE - 2;
    int FETCH_PRIORITY_DEPTH_3 = FETCH_PRIORITY_DEPTH_BASE - 3;
    int FETCH_PRIORITY_EMERGENT_INJECT = 3000;
    int FETCH_PRIORITY_MAX = 10 * 1000;

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
     * Status / result message.
     * */
    String STAT_PULSAR_STATUS = "Pulsar Status";
    String STAT_INFECTED_ROWS = "injectedRows";

    /**
     * Generate
     * */
    String GENERATE_COUNT_VALUE_DOMAIN = "domain";
    String GENERATE_COUNT_VALUE_HOST = "host";
    String GENERATE_COUNT_VALUE_IP = "ip";

    int MAX_LIVE_LINK_PER_PAGE = 1000;
    int MAX_LINK_PER_PAGE = 4000;

    String CMD_FORCE_GENERATE_SEEDS = "force-generate-seeds";

    int BANDWIDTH_INFINITE = 10000; // bandwidth in mbytes

    /**
     * Index
     * */
    String INDEXER_WRITE_COMMIT_SIZE = "indexer.write.commit.size";

    String DEFAULT_PULSAR_MASTER_HOST = "0.0.0.0";
    int DEFAULT_PULSAR_MASTER_PORT = 8182;

    String DEFAULT_INDEX_SERVER_HOSTNAME = "master";
    int DEFAULT_INDEX_SERVER_PORT = 8983;

    String PATH_PULSAR_TMP_DIR = "/tmp/pulsar-" + System.getenv("USER");
    String PATH_PULSAR_OUTPUT_DIR = PATH_PULSAR_TMP_DIR;
    String PATH_PULSAR_REPORT_DIR = PATH_PULSAR_OUTPUT_DIR + "/report";
    String PATH_PULSAR_CACHE_DIR = PATH_PULSAR_OUTPUT_DIR + "/cache";
    String PATH_LAST_BATCH_ID = PATH_PULSAR_TMP_DIR + "/last-batch-id";
    String PATH_LAST_GENERATED_ROWS = PATH_PULSAR_TMP_DIR + "/last-generated-rows";
    String PATH_LOCAL_COMMAND = PATH_PULSAR_TMP_DIR + "/pulsar-commands";
    String PATH_EMERGENT_SEEDS = PATH_PULSAR_TMP_DIR + "/emergent-seeds";
    String PATH_BANNED_URLS = PATH_PULSAR_TMP_DIR + "/banned-urls";
    Path PATH_BANNED_URLS_PATH = Paths.get(PATH_BANNED_URLS);

    String FILE_UNREACHABLE_HOSTS = "unreachable-hosts.txt";
}
