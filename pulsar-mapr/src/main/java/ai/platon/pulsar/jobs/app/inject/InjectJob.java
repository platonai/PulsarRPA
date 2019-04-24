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
package ai.platon.pulsar.jobs.app.inject;

import ai.platon.pulsar.common.HdfsUtils;
import ai.platon.pulsar.common.StringUtil;
import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.common.config.Params;
import ai.platon.pulsar.common.options.CommonOptions;
import ai.platon.pulsar.jobs.core.AppContextAwareJob;
import ai.platon.pulsar.persist.gora.generated.GWebPage;
import com.beust.jcommander.Parameter;
import org.apache.commons.io.FileUtils;
import org.apache.gora.mapreduce.GoraOutputFormat;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static ai.platon.pulsar.common.PulsarParams.*;
import static ai.platon.pulsar.common.config.CapabilityTypes.*;
import static ai.platon.pulsar.common.config.PulsarConstants.JOB_CONTEXT_CONFIG_LOCATION;

public final class InjectJob extends AppContextAwareJob {

  public static final Logger LOG = LoggerFactory.getLogger(InjectJob.class);

  public InjectJob() {}

  public InjectJob(ImmutableConfig conf) {
    setConf(conf);
  }

  @Override
  public void setup(Params params) throws Exception {
    String crawlId = params.get(ARG_CRAWL_ID, conf.get(STORAGE_CRAWL_ID));
    String seeds = params.get(ARG_SEEDS, "");

    if (seeds.startsWith("@")) {
      seeds = new String(Files.readAllBytes(Paths.get(seeds.substring(1))));
    }

    List<String> configuredUrls = StringUtil.getUnslashedLines(seeds).stream()
        .filter(u -> !u.isEmpty() && !u.startsWith("#"))
        .sorted().distinct().collect(Collectors.toList());

    // And also save it in HDFS as normal file
    File seedFile = File.createTempFile("seed", ".txt");
    FileUtils.writeLines(seedFile, configuredUrls);
    String seedPath = seedFile.getAbsolutePath();
    if (HdfsUtils.isDistributedFS(conf)) {
      LOG.info("Running under hadoop distributed file system, copy seed file onto HDFS");
      HdfsUtils.copyFromLocalFile(seedPath, conf);
    }

    conf.set(STORAGE_CRAWL_ID, crawlId);
    conf.set(INJECT_SEED_PATH, seedPath);

    LOG.info(Params.format(
        "className", this.getClass().getSimpleName(),
        "crawlId", crawlId,
        "seedPath", seedPath
    ));
  }

  @Override
  public void initJob() throws Exception {
    Configuration conf = getConf().unbox();

    String seedPath = conf.get(INJECT_SEED_PATH);

    FileInputFormat.addInputPath(currentJob, new Path(seedPath));
    currentJob.setMapperClass(InjectMapper.class);
    currentJob.setMapOutputKeyClass(String.class);
    currentJob.setMapOutputValueClass(GWebPage.class);
    currentJob.setOutputFormatClass(GoraOutputFormat.class);

    currentJob.setReducerClass(Reducer.class);
    GoraOutputFormat.setOutput(currentJob, webDb.getStore(), true);
    currentJob.setNumReduceTasks(0);
  }

  /**
   * Command options for {@link InjectJob}.
   * Expect the list option which specify the seed or seed file, the @ sign is not supported
   * */
  private static class InjectOptions extends CommonOptions {
    @Parameter(required = true, description = "<seeds> \nSeed urls. Use {@code @FILE} syntax to read from file.")
    List<String> seeds = new ArrayList<>();
    @Parameter(names = ARG_LIMIT, description = "task limit")
    int limit = -1;

    public InjectOptions(String[] args, ImmutableConfig conf) {
      super(args);
      // We may read seeds from a file using @ sign, the file parsing should be handled manually
      setExpandAtSign(false);
      this.setCrawlId(conf.get(STORAGE_CRAWL_ID, ""));
    }

    @Override
    public Params getParams() {
      return Params.of(
          ARG_CRAWL_ID, getCrawlId(),
          ARG_SEEDS, seeds.get(0),
          ARG_LIMIT, limit
      );
    }
  }

  @Override
  public int run(String[] args) throws Exception {
    InjectOptions opts = new InjectOptions(args, conf);
    opts.parseOrExit();
    run(opts.getParams());
    return 0;
  }

  public static void main(String[] args) throws Exception {
    String configLocation = System.getProperty(APPLICATION_CONTEXT_CONFIG_LOCATION, JOB_CONTEXT_CONFIG_LOCATION);
    int res = AppContextAwareJob.run(configLocation, new InjectJob(), args);
    System.exit(res);
  }
}
