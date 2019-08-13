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
package ai.platon.pulsar.jobs.app.homepage;

import ai.platon.pulsar.common.ScoreVector;
import ai.platon.pulsar.common.config.Params;
import ai.platon.pulsar.crawl.scoring.ScoringFilters;
import ai.platon.pulsar.jobs.common.SelectorEntry;
import ai.platon.pulsar.jobs.common.URLPartitioner;
import ai.platon.pulsar.jobs.core.AppContextAwareGoraMapper;
import ai.platon.pulsar.persist.WebPage;
import ai.platon.pulsar.persist.gora.generated.GWebPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static ai.platon.pulsar.common.CommonCounter.mPersist;
import static ai.platon.pulsar.common.CommonCounter.mRows;
import static ai.platon.pulsar.common.config.CapabilityTypes.APPLICATION_CONTEXT_CONFIG_LOCATION;
import static ai.platon.pulsar.common.config.CapabilityTypes.STAT_INDEX_HOME_URL;
import static ai.platon.pulsar.common.config.PulsarConstants.JOB_CONTEXT_CONFIG_LOCATION;
import static ai.platon.pulsar.common.config.PulsarConstants.SEED_HOME_URL;

public final class SeedHomeUpdateJob extends HomePageUpdateJob {

  public static final Logger LOG = LoggerFactory.getLogger(SeedHomeUpdateJob.class);

  @Override
  public void setIndexHomeUrl() {
    conf.set(STAT_INDEX_HOME_URL, SEED_HOME_URL);
  }

  @Override
  public void initJob() throws Exception {
    initMapper(currentJob, FIELDS, SelectorEntry.class,
        GWebPage.class, SeedIndexMapper.class, URLPartitioner.SelectorEntryPartitioner.class,
        getQueryFilter(), false);
    initReducer(currentJob, HomePageUpdateReducer.class);
  }

  public static class SeedIndexMapper extends AppContextAwareGoraMapper<String, GWebPage, SelectorEntry, GWebPage> {
    private ScoringFilters scoringFilters;

    @Override
    public void setup(Context context) throws IOException, InterruptedException {
      scoringFilters = applicationContext.getBean(ScoringFilters.class);

      Params.of(
          "className", this.getClass().getSimpleName(),
          "scoringFilters", scoringFilters)
          .merge(scoringFilters.getParams())
          .withLogger(LOG).info();
    }

    @Override
    public void map(String reversedUrl, GWebPage row, Context context) throws IOException, InterruptedException {
      metricsCounters.increase(mRows);

      WebPage page = WebPage.box(reversedUrl, row, true);
      String url = page.getUrl();

      if (!page.isSeed()) {
        return;
      }

      ScoreVector sortScore = scoringFilters.generatorSortValue(page, 1.0f);
      context.write(new SelectorEntry(url, sortScore), page.unbox());

      metricsCounters.increase(mPersist);
    }
  }

  public static void main(String args[]) throws Exception {
    String configLocation = System.getProperty(APPLICATION_CONTEXT_CONFIG_LOCATION, JOB_CONTEXT_CONFIG_LOCATION);
    int res = run(configLocation, new SeedHomeUpdateJob(), args);
    System.exit(res);
  }
}
