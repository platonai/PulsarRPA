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
package ai.platon.pulsar.jobs.app.update;

import ai.platon.pulsar.common.AppFiles;
import ai.platon.pulsar.common.config.Params;
import ai.platon.pulsar.common.options.CommonOptions;
import ai.platon.pulsar.jobs.core.AppContextAwareJob;
import ai.platon.pulsar.persist.gora.generated.GWebPage;
import com.beust.jcommander.Parameter;
import org.apache.hadoop.mapreduce.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static ai.platon.pulsar.common.PulsarParams.*;
import static ai.platon.pulsar.common.config.CapabilityTypes.*;
import static ai.platon.pulsar.common.config.PulsarConstants.ALL_BATCHES;

abstract class WebGraphUpdateJob extends AppContextAwareJob {

  public static final Logger LOG = LoggerFactory.getLogger(WebGraphUpdateJob.class);

  private static final Collection<GWebPage.Field> FIELDS = new HashSet<>();

  static {
    Collections.addAll(FIELDS, GWebPage.Field.values());
    FIELDS.remove(GWebPage.Field.CONTENT);
    FIELDS.remove(GWebPage.Field.PAGE_TEXT);
    FIELDS.remove(GWebPage.Field.CONTENT_TEXT);
    FIELDS.remove(GWebPage.Field.LINKS);
    FIELDS.remove(GWebPage.Field.PAGE_MODEL);
  }

  public Collection<GWebPage.Field> getFields(Job job) {
    return FIELDS;
  }

  protected String crawlId;
  protected String batchId;
  protected int limit;
  protected int round;

  @Override
  public void setup(Params params) throws Exception {
    crawlId = params.get(ARG_CRAWL_ID, conf.get(STORAGE_CRAWL_ID));
    batchId = params.get(ARG_BATCH_ID, ALL_BATCHES);
    limit = params.getInt(ARG_LIMIT, -1);
    round = conf.getInt(CRAWL_ROUND, 0);

    conf.set(STORAGE_CRAWL_ID, crawlId);
    conf.set(BATCH_ID, batchId);
    conf.setInt(LIMIT, limit);

    LOG.info(Params.format(
        "className", this.getClass().getSimpleName(),
        "round", round,
        "crawlId", crawlId,
        "batchId", batchId,
        "limit", limit
    ));
  }

  protected int update(String crawlId, String batchId, int limit) throws Exception {
    run(Params.of(ARG_CRAWL_ID, crawlId, ARG_BATCH_ID, batchId, ARG_LIMIT, limit));
    return 0;
  }

  /**
   * Command options for {@link WebGraphUpdateJob}.
   * */
  private static class UpdateOptions extends CommonOptions {
    @Parameter(description = "[batchId], If not specified, use last generated batch id.")
    List<String> batchId = new ArrayList<>();

    public UpdateOptions(String[] args) {
      super(args);
    }

    @Override
    public Params getParams() {
      if (batchId.isEmpty()) {
        batchId.add(AppFiles.INSTANCE.readBatchIdOrDefault("all"));
      }

      return Params.of(
          ARG_CRAWL_ID, getCrawlId(),
          ARG_BATCH_ID, batchId.get(0)
      );
    }
  }

  @Override
  public int run(String[] args) throws Exception {
    UpdateOptions opts = new UpdateOptions(args);
    opts.parseOrExit();
    run(opts.getParams());
    return 0;
  }
}
