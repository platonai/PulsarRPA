/*******************************************************************************
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
 ******************************************************************************/
package ai.platon.pulsar.jobs.app.fetch;

import ai.platon.pulsar.PulsarEnv;
import ai.platon.pulsar.common.AppFiles;
import ai.platon.pulsar.common.URLUtil;
import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.common.config.Params;
import ai.platon.pulsar.common.config.PulsarConstants;
import ai.platon.pulsar.jobs.common.FetchEntryWritable;
import ai.platon.pulsar.jobs.common.URLPartitioner;
import ai.platon.pulsar.jobs.core.AppContextAwareJob;
import ai.platon.pulsar.persist.gora.generated.GWebPage;
import ai.platon.pulsar.persist.metadata.FetchMode;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import org.apache.commons.lang3.StringUtils;
import org.apache.gora.filter.MapFieldValueFilter;
import org.apache.hadoop.io.IntWritable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.*;

import static ai.platon.pulsar.common.PulsarParams.*;
import static ai.platon.pulsar.common.config.CapabilityTypes.*;
import static ai.platon.pulsar.common.config.PulsarConstants.*;

/**
 * Fetch job
 * */
public final class FetchJob extends AppContextAwareJob {

  public static final Logger LOG = LoggerFactory.getLogger(FetchJob.class);

  private static final Collection<GWebPage.Field> FIELDS = new HashSet<>();

  static {
    Collections.addAll(FIELDS, GWebPage.Field.values());
    FIELDS.remove(GWebPage.Field.CONTENT);
    FIELDS.remove(GWebPage.Field.PAGE_TEXT);
    FIELDS.remove(GWebPage.Field.CONTENT_TEXT);
  }

  public FetchJob() {}

  public FetchJob(ImmutableConfig conf) { setConf(conf); }

  @Override
  public void setup(Params params) throws Exception {
    int round = conf.getInt(CRAWL_ROUND, 0);
    String crawlId = params.get(ARG_CRAWL_ID, conf.get(STORAGE_CRAWL_ID, ""));
    FetchMode fetchMode = params.getEnum(ARG_FETCH_MODE, conf.getEnum(FETCH_MODE, FetchMode.NATIVE));
    boolean strictDf = params.getBoolean(ARG_STRICT_DF, false);
    String batchId = params.get(ARG_BATCH_ID, ALL_BATCHES);
    int threads = params.getInt(ARG_THREADS, conf.getInt(FETCH_THREADS_FETCH, 5));
    boolean resume = params.getBoolean(ARG_RESUME, false);
    int limit = params.getInt(ARG_LIMIT, Integer.MAX_VALUE);
    int numTasks = params.getInt(ARG_NUMTASKS, conf.getInt(MAPREDUCE_JOB_REDUCES, 2));

    /* Index */
    boolean index = params.getBoolean(ARG_INDEX, false);
    String indexerUrl = params.getString(ARG_INDEXER_URL);
    String zkHostString = params.getString(ARG_ZK);
    String indexerCollection = params.getString(ARG_COLLECTION);

    /* Set re-computed config variables */
    conf.set(STORAGE_CRAWL_ID, crawlId);
    conf.set(BATCH_ID, batchId);
    conf.setEnum(FETCH_MODE, fetchMode);
    conf.set(FETCH_CRAWL_PATH_STRATEGY, strictDf ? CRAWL_STRICT_DEPTH_FIRST : CRAWL_DEPTH_FIRST);

    conf.setInt(FETCH_THREADS_FETCH, threads);
    conf.setBoolean(RESUME, resume);
    conf.setInt(MAPPER_LIMIT, limit);
    conf.setInt(MAPREDUCE_JOB_REDUCES, numTasks);

    conf.setBoolean(INDEX_JIT, index);
    if (index) {
      conf.setIfNotEmpty(INDEXER_ZK, zkHostString);
      conf.setIfNotEmpty(INDEXER_URL, indexerUrl);
      conf.setIfNotEmpty(INDEXER_COLLECTION, indexerCollection);
    }

    LOG.info(Params.format(
        "className", this.getClass().getSimpleName(),
        "round", round,
        "crawlId", crawlId,
        "batchId", batchId,
        "fetchMode", fetchMode,
        "numTasks", numTasks,
        "threads", threads,
        "resume", resume,
        "limit", limit,
        "index", index,
        "indexerUrl", indexerUrl,
        "zkHostString", zkHostString,
        "indexerCollection", indexerCollection
    ));
  }

  @Override
  public void initJob() throws Exception {
    String batchId = conf.get(BATCH_ID);
    int numTasks = conf.getInt(MAPREDUCE_JOB_REDUCES, 2);

    // For politeness, don't permit parallel execution of a single task
    currentJob.setReduceSpeculativeExecution(false);

    MapFieldValueFilter<String, GWebPage> batchIdFilter = getBatchIdFilter(batchId);
    initMapper(currentJob, FIELDS, IntWritable.class, FetchEntryWritable.class, FetchMapper.class,
        URLPartitioner.FetchEntryPartitioner.class, batchIdFilter, false);
    initReducer(currentJob, FetchReducer.class);

    currentJob.setNumReduceTasks(numTasks);

    LOG.debug("Loaded Fields : " + StringUtils.join(FIELDS, ", "));
  }

  public int fetch(String crawlId, FetchMode fetchMode, String batchId, int threads, boolean resume, int limit, int numTasks) throws Exception {
    return fetch(crawlId, fetchMode, batchId, threads, resume, limit, numTasks, false, null, null, null);
  }

  /**
   * Run fetcher.
   *
   * @param batchId
   *          batchId (obtained from Generator) or null to fetch all generated
   *          fetchlists
   * @param threads
   *          number of threads per map task
   * @param resume
   *          resume
   * @param numTasks
   *          number of fetching tasks (reducers). If set to < 1 then use the
   *          default, which is jobs.job.reduces.
   * @return 0 on success
   * @throws Exception
   * */
  public int fetch(String crawlId, FetchMode fetchMode, String batchId, int threads, boolean resume, int limit, int numTasks,
                   boolean index, String indexer, String zk, String collection) throws Exception {
    run(Params.of(
        ARG_CRAWL_ID, crawlId,
        ARG_BATCH_ID, batchId,
        ARG_FETCH_MODE, fetchMode,
        ARG_THREADS, threads,
        ARG_RESUME, resume,
        ARG_NUMTASKS, numTasks > 0 ? numTasks : null,
        ARG_LIMIT, limit > 0 ? limit : null,
        ARG_INDEX, index,
        ARG_INDEXER, indexer,
        ARG_ZK, zk,
        ARG_COLLECTION, collection
    ));

    return 0;
  }

  @Parameters(commandNames = {"FetchJob"}, commandDescription = "Fetch tasks created during generate.")
  public static class FetchOptions {
    @Parameter(description = "[batchId], If not specified, use last generated batch id.")
    List<String> batchId = new ArrayList<>();
    @Parameter(names = ARG_CRAWL_ID, description = "The id to prefix the schemas to operate on")
    String crawlId = null;
    @Parameter(names = ARG_FETCH_MODE, description = "Fetch mode")
    FetchMode fetchMode = FetchMode.NATIVE;
    @Parameter(names = ARG_STRICT_DF, description = "If true, crawl the web using strict depth-first strategy")
    boolean strictDf = false;
    @Parameter(names = ARG_INDEX, description = "Active indexer")
    boolean index = false;
    @Parameter(names = ARG_INDEXER, description = "Indexer full url or collection only, "
        + " for example, http://localhost:8983/solr/collection or collection")
    String indexer = null;
    @Parameter(names = ARG_COLLECTION, description = "Index server side collection name")
    String collection = null;
    @Parameter(names = ARG_ZK, description = "Zookeeper host string")
    String zk = null;
    @Parameter(names = ARG_NUMTASKS, description = "Number of reducers")
    int numTasks = -1;
    @Parameter(names = ARG_THREADS, description = "Number of fetching threads per task")
    int threads = -1;
    @Parameter(names = ARG_RESUME, description = "Resume interrupted job")
    boolean resume = false;
    @Parameter(names = ARG_LIMIT, description = "Task limit")
    int limit = -1;
    @Parameter(names = ARG_VERBOSE, description = "More logs")
    int verbose = 0;
    @Parameter(names = {"-help", "-h"}, help = true, description = "Print this help text")
    boolean help;

    private JCommander jc;

    public FetchOptions(String[] args, ImmutableConfig conf) {
      jc = new JCommander(this);

      try {
        jc.parse(args);
      }
      catch (ParameterException e) {
        System.out.println(e.toString());
        System.out.println("Try '-h' or '-help' for more information.");
        System.exit(0);
      }

      if (help) {
        jc.usage();
        return;
      }

      if (batchId.isEmpty()) {
        batchId.add(AppFiles.INSTANCE.readBatchIdOrDefault(ALL_BATCHES));
      }

      indexer = StringUtils.stripEnd(indexer, "/");
      String indexerHost = URLUtil.getHostName(indexer);
      if (indexerHost == null) {
        collection = indexer;
        indexer = null;
      }
    }

    public boolean isHelp() {
      return help;
    }

    public Params toParams() {
      return Params.of(
          ARG_CRAWL_ID, crawlId,
          ARG_BATCH_ID, batchId.get(0),
          ARG_FETCH_MODE, fetchMode,
          ARG_STRICT_DF, strictDf,
          ARG_THREADS, threads > 0 ? threads : null,
          ARG_RESUME, resume,
          ARG_NUMTASKS, numTasks > 0 ? numTasks : null,
          ARG_LIMIT, limit > 0 ? limit : null,
          ARG_INDEX, index,
          ARG_INDEXER_URL, indexer,
          ARG_ZK, zk,
          ARG_COLLECTION, collection
      );
    }
  }

  @Override
  public int run(String[] args) throws Exception {
    FetchOptions opts = new FetchOptions(args, conf);
    if (opts.isHelp()) {
      return 0;
    }

    run(opts.toParams());

    return 0;
  }
}
