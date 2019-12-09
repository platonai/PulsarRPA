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
package ai.platon.pulsar.jobs.app.parse;

import ai.platon.pulsar.common.PulsarParams;
import ai.platon.pulsar.common.config.CapabilityTypes;
import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.common.config.Params;
import ai.platon.pulsar.jobs.common.IdentityPageReducer;
import ai.platon.pulsar.jobs.core.AppContextAwareJob;
import ai.platon.pulsar.jobs.core.PulsarJob;
import ai.platon.pulsar.persist.gora.generated.GWebPage;
import org.apache.gora.filter.MapFieldValueFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import static ai.platon.pulsar.common.config.CapabilityTypes.*;
import static ai.platon.pulsar.common.config.PulsarConstants.ALL_BATCHES;
import static ai.platon.pulsar.common.config.PulsarConstants.JOB_CONTEXT_CONFIG_LOCATION;

public class ParserJob extends AppContextAwareJob {

  public static final Logger LOG = LoggerFactory.getLogger(ParserJob.class);

  private static final Collection<GWebPage.Field> FIELDS = new HashSet<>();

  private String batchId;

  static {
    Collections.addAll(FIELDS, GWebPage.Field.values());
  }

  public ParserJob() {

  }

  public ParserJob(ImmutableConfig conf) {
    setJobConf(conf);
  }

  public static Collection<GWebPage.Field> getFields(ImmutableConfig conf) {
    return FIELDS;
  }

  @Override
  public void setup(Params params) throws Exception {
    String crawlId = params.get(PulsarParams.ARG_CRAWL_ID, jobConf.get(STORAGE_CRAWL_ID));
    String fetchMode = jobConf.get(CapabilityTypes.FETCH_MODE);

    batchId = params.get(PulsarParams.ARG_BATCH_ID, ALL_BATCHES);
    Boolean reparse = batchId.equalsIgnoreCase("-reparse");
    batchId = reparse ? ALL_BATCHES : batchId;
    int limit = params.getInt(PulsarParams.ARG_LIMIT, -1);
    Boolean resume = params.getBoolean(PulsarParams.ARG_RESUME, false);
    Boolean force = params.getBoolean(PulsarParams.ARG_FORCE, false);

    jobConf.set(STORAGE_CRAWL_ID, crawlId);
    jobConf.set(BATCH_ID, batchId);
    jobConf.setInt(CapabilityTypes.LIMIT, limit);
    jobConf.setBoolean(CapabilityTypes.RESUME, resume);
    jobConf.setBoolean(CapabilityTypes.FORCE, force);
    jobConf.setBoolean(CapabilityTypes.PARSE_REPARSE, reparse);

    LOG.info(Params.format(
        "className", this.getClass().getSimpleName(),
        "crawlId", crawlId,
        "batchId", batchId,
        "fetchMode", fetchMode,
        "resume", resume,
        "force", force,
        "reparse", reparse,
        "limit", limit
    ));
  }

  @Override
  public void initJob() throws Exception {
    Collection<GWebPage.Field> fields = getFields(getJobConf());
    MapFieldValueFilter<String, GWebPage> batchIdFilter = getBatchIdFilter(batchId);

    initMapper(currentJob, fields, String.class, GWebPage.class, ParserMapper.class, batchIdFilter);
    initReducer(currentJob, IdentityPageReducer.class);

    // there is no reduce phase, so set reduce tasks to be 0
    currentJob.setNumReduceTasks(0);
  }

  private void printUsage() {
    System.err.println("Usage: ParserJob (<batchId> | -reparse) [-crawlId <id>] [-resume] [-force]");
    System.err.println("    <batchId>     - symbolic batch ID created by Generator");
    System.err.println("    -reparse      - reparse pages from all crawl jobs");
    System.err.println("    -crawlId <id> - the id to prefix the schemas to operate on, \n \t \t    (default: storage.crawl.id)");
    System.err.println("    -limit        - limit");
    System.err.println("    -resume       - resume a previous incomplete job");
    System.err.println("    -force        - force re-parsing even if a page is already parsed");
  }

  public int run(String[] args) throws Exception {
    if (args.length < 1) {
      printUsage();
      return -1;
    }

    ImmutableConfig conf = getJobConf();

    String batchId = args[0];
    if (batchId.startsWith("-")) {
      printUsage();
      return -1;
    }

    String crawlId = conf.get(STORAGE_CRAWL_ID, "");

    int limit = -1;
    boolean resume = false;
    boolean force = false;

    for (int i = 0; i < args.length; i++) {
      if ("-crawlId".equals(args[i])) {
        crawlId = args[++i];
        // getConf().set(CRAWL_ID, args[++i]);
      }
      else if ("-limit".equals(args[i])) {
        limit = Integer.parseInt(args[++i]);
      }
      else if ("-resume".equals(args[i])) {
        resume = true;
      }
      else if ("-force".equals(args[i])) {
        force = true;
      }
    }

    parse(crawlId, batchId, limit, resume, force);

    return 0;
  }

  public void parse(String crawlId, String batchId, int limit, boolean resume, boolean force) {
    run(Params.of(
        PulsarParams.ARG_CRAWL_ID, crawlId,
        PulsarParams.ARG_BATCH_ID, batchId,
        PulsarParams.ARG_RESUME, resume,
        PulsarParams.ARG_FORCE, force,
        PulsarParams.ARG_LIMIT, limit > 0 ? limit : null
    ));
  }

  public static void main(String[] args) throws Exception {
    String configLocation = System.getProperty(APPLICATION_CONTEXT_CONFIG_LOCATION, JOB_CONTEXT_CONFIG_LOCATION);
    final int res = PulsarJob.run(new ImmutableConfig(), new ParserJob(), args);
    System.exit(res);
  }
}
