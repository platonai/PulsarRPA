/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package ai.platon.pulsar.jobs.app.generate;

import ai.platon.pulsar.common.*;
import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.common.config.Params;
import ai.platon.pulsar.jobs.common.JobUtils;
import ai.platon.pulsar.jobs.common.SelectorEntry;
import ai.platon.pulsar.jobs.common.URLPartitioner;
import ai.platon.pulsar.jobs.core.AppContextAwareJob;
import ai.platon.pulsar.persist.gora.generated.GWebPage;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static ai.platon.pulsar.common.PulsarParams.*;
import static ai.platon.pulsar.common.config.CapabilityTypes.*;
import static ai.platon.pulsar.common.config.PulsarConstants.*;

public final class GenerateJob extends AppContextAwareJob {

  public static final Logger LOG = LoggerFactory.getLogger(GenerateJob.class);

  private static final Set<GWebPage.Field> FIELDS = new HashSet<>();

  static {
    Collections.addAll(FIELDS, GWebPage.Field.values());
    FIELDS.remove(GWebPage.Field.CONTENT);
    FIELDS.remove(GWebPage.Field.PAGE_TEXT);
    FIELDS.remove(GWebPage.Field.CONTENT_TEXT);

    FIELDS.remove(GWebPage.Field.LINKS);
    FIELDS.remove(GWebPage.Field.LIVE_LINKS);
    FIELDS.remove(GWebPage.Field.INLINKS);

    FIELDS.remove(GWebPage.Field.PAGE_MODEL);
  }

  public GenerateJob() {
  }

  public GenerateJob(ImmutableConfig conf) {
    setConf(conf);
  }

  /**
   * The field list affects which field to reads, but does not affect which field to to write
   * */
  public Collection<GWebPage.Field> getFields(ImmutableConfig conf) {
    Collection<GWebPage.Field> fields = new HashSet<>(FIELDS);
    return fields;
  }

  @Override
  public void setup(Params params) throws Exception {
    String crawlId = params.get(ARG_CRAWL_ID, conf.get(STORAGE_CRAWL_ID, ""));
    String batchId = params.get(ARG_BATCH_ID, JobUtils.generateBatchId());
    boolean reGenerate = params.getBoolean(ARG_REGENERATE, false);
    boolean reGenerateSeeds = params.getBoolean(ARG_REGENERATE_SEEDS, false);
    int topN = params.getInt(ARG_TOPN, Integer.MAX_VALUE);
    boolean filter = !params.getBoolean(ARG_NO_FILTER, false);
    boolean normalize = !params.getBoolean(ARG_NO_NORMALIZER, false);
    long pseudoCurrTime = params.getLong(ARG_CURTIME, startTime);

    int round = conf.getInt(CRAWL_ROUND, 0);
    int maxDistance = conf.getUint(CRAWL_MAX_DISTANCE, DISTANCE_INFINITE);
    int lastGeneratedRows = PulsarFiles.INSTANCE.readLastGeneratedRows();
    if (!reGenerateSeeds) {
      reGenerateSeeds = RuntimeUtils.hasLocalFileCommand(CMD_FORCE_GENERATE_SEEDS);
    }

    conf.set(STORAGE_CRAWL_ID, crawlId);
    conf.set(BATCH_ID, batchId);
    conf.setLong(GENERATE_CUR_TIME, pseudoCurrTime);
    conf.setBoolean(GENERATE_REGENERATE, reGenerate);
    conf.setBoolean(GENERATE_REGENERATE_SEEDS, reGenerateSeeds);
    conf.setInt(GENERATE_TOP_N, topN);
    conf.setInt(GENERATE_LAST_GENERATED_ROWS, lastGeneratedRows);
    conf.setBoolean(GENERATE_FILTER, filter);
    conf.setBoolean(GENERATE_NORMALISE, normalize);

    URLUtil.GroupMode groupMode = conf.getEnum(GENERATE_COUNT_MODE, URLUtil.GroupMode.BY_HOST);
    conf.setEnum(PARTITION_MODE_KEY, groupMode);

    prepareSystemFiles(conf);

    LOG.info(Params.format(
        "className", this.getClass().getSimpleName(),
        "round", round,
        "crawlId", crawlId,
        "batchId", batchId,
        "filter", filter,
        "normalize", normalize,
        "maxDistance", maxDistance,
        "topN", topN,
        "lastGeneratedRows", lastGeneratedRows,
        "reGenerate", reGenerate,
        "reGenerateSeeds", reGenerateSeeds,
        "groupMode", groupMode,
        "partitionMode", groupMode,
        "pseudoCurrTime", DateTimeUtil.format(pseudoCurrTime)
    ));

    printInfo();
  }

  private void prepareSystemFiles(ImmutableConfig conf) throws IOException {
    PulsarFiles.INSTANCE.writeBatchId(conf.get(BATCH_ID));

    if (HdfsUtils.isDistributedFS(conf)) {
      LOG.info("Running under hadoop distributed file system, copy files to HDFS");

      if (Files.exists(PulsarPaths.PATH_BANNED_URLS)) {
        HdfsUtils.copyFromLocalFile(PulsarPaths.PATH_BANNED_URLS.toString(), conf);
      }
    }
  }

  @Override
  public void initJob() throws Exception {
    Collection<GWebPage.Field> fields = getFields(getConf());
    initMapper(currentJob, fields, SelectorEntry.class,
        GWebPage.class, GenerateMapper.class, URLPartitioner.SelectorEntryPartitioner.class,
        getInactiveFilter(), false);
    initReducer(currentJob, GenerateReducer.class);

//    log.debug("Loaded Fields : " + StringUtils.join(fields, ", "));
  }

  @Override
  protected void afterCleanup() {
    super.afterCleanup();

    try {
      long affectedRows = currentJob.getCounters().findCounter(STAT_PULSAR_STATUS, "2'rPersist").getValue();
      PulsarFiles.INSTANCE.writeLastGeneratedRows(affectedRows);
    } catch (IOException e) {
      LOG.error(e.toString());
    }
  }

  private void printInfo() {
    String info = "File Based Commands : \n"
        + "1. force generate and re-fetch seeds next round : \n"
        + "echo " + CMD_FORCE_GENERATE_SEEDS + " > " + PulsarPaths.PATH_LOCAL_COMMAND + "\n"
        + "2. ban a url : \n"
        + "echo \"" + EXAMPLE_URL + "\" >> " + PulsarPaths.PATH_BANNED_URLS + "\n";

    LOG.info(info);
  }

  /**
   * Mark URLs ready for fetching
   *
   * @throws Exception
   * */
  public String generate(String crawlId, String batchId, int topN) throws Exception {
    return generate(crawlId, batchId, topN, false, false, System.currentTimeMillis(), false, false);
  }

  public String generate(String crawlId, String batchId, int topN, boolean reGenerate, boolean reGenerateSeeds,
                         long pseudoCurrTime, boolean noNorm, boolean noFilter) throws Exception {
    run(Params.of(
        ARG_CRAWL_ID, crawlId,
        ARG_BATCH_ID, batchId,
        ARG_TOPN, topN,
        ARG_REGENERATE, reGenerate,
        ARG_REGENERATE_SEEDS, reGenerateSeeds,
        ARG_CURTIME, pseudoCurrTime,
        ARG_NO_NORMALIZER, noNorm,
        ARG_NO_FILTER, noFilter
    ));

    return getConf().get(BATCH_ID);
  }

  private class GenerateOptions {
    @Parameter(names = {ARG_CRAWL_ID}, description = "The crawl id, (default : \"storage.crawl.id\").")
    String crawlId = null;

    @Parameter(names = {ARG_BATCH_ID}, description = "The batch id")
    String batchId = JobUtils.generateBatchId();

    @Parameter(names = {"-reGen"}, description = "Re generate pages")
    boolean reGenerate = false;

    @Parameter(names = {"-reSeeds"}, description = "Re-generate all seeds")
    boolean reGenerateSeeds = false;

    @Parameter(names = {ARG_TOPN}, description = "Number of top URLs to be selected")
    int topN = Integer.MAX_VALUE;

    @Parameter(names = {ARG_NO_NORMALIZER}, description = "Activate the normalizer plugin to normalize the url")
    boolean noNormalizer = false;

    @Parameter(names = {ARG_NO_FILTER}, description = "Activate the filter plugin to filter the url")
    boolean noFilter = false;

    @Parameter(names = {ARG_ADDDAYS}, description = "Adds numDays to the current time to facilitate crawling urls already. " +
        "Fetched sooner then default.")
    long adddays = 0;

    @Parameter(names = ARG_LIMIT, description = "task limit")
    int limit = Integer.MAX_VALUE;

    @Parameter(names = {"-help", "-h"}, help = true, description = "print the help information")
    boolean help;

    private JCommander jc;

    public GenerateOptions(String[] args, ImmutableConfig conf) {
      crawlId = conf.get(STORAGE_CRAWL_ID);

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
      }
    }

    public boolean isHelp() {
      return help;
    }

    public Params toParams() {
      return Params.of(
          ARG_CRAWL_ID, crawlId,
          ARG_BATCH_ID, batchId,
          ARG_TOPN, topN,
          ARG_REGENERATE, reGenerate,
          ARG_REGENERATE_SEEDS, reGenerateSeeds,
          ARG_CURTIME, System.currentTimeMillis() + Duration.ofDays(adddays).toMillis(),
          ARG_NO_NORMALIZER, noNormalizer,
          ARG_NO_FILTER, noFilter
      );
    }
  }

  @Override
  public int run(String[] args) {
    GenerateOptions opts = new GenerateOptions(args, conf);
    if (!opts.isHelp()) {
      run(opts.toParams());
    }
    return 0;
  }

  public static void main(String args[]) throws Exception {
    String configLocation = System.getProperty(APPLICATION_CONTEXT_CONFIG_LOCATION, JOB_CONTEXT_CONFIG_LOCATION);
    int res = AppContextAwareJob.run(configLocation, new GenerateJob(), args);
    System.exit(res);
  }
}
