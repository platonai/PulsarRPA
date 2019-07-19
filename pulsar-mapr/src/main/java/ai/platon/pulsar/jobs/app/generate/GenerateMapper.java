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

import ai.platon.pulsar.common.CounterUtils;
import ai.platon.pulsar.common.config.Params;
import ai.platon.pulsar.crawl.component.GenerateComponent;
import ai.platon.pulsar.crawl.filter.CrawlFilter;
import ai.platon.pulsar.common.ScoreVector;
import ai.platon.pulsar.crawl.scoring.ScoringFilters;
import ai.platon.pulsar.jobs.common.SelectorEntry;
import ai.platon.pulsar.jobs.core.AppContextAwareGoraMapper;
import ai.platon.pulsar.persist.WebPage;
import ai.platon.pulsar.persist.gora.generated.GWebPage;
import org.slf4j.Logger;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static ai.platon.pulsar.common.CommonCounter.*;
import static ai.platon.pulsar.common.config.CapabilityTypes.*;

public class GenerateMapper extends AppContextAwareGoraMapper<String, GWebPage, SelectorEntry, GWebPage> {
  public static final Logger LOG = GenerateJob.LOG;

  private GenerateComponent generateComponent;
  private ScoringFilters scoringFilters;

  private final Set<String> unreachableHosts = new HashSet<>();

  @Override
  public void setup(Context context) throws IOException, InterruptedException {
    generateComponent = applicationContext.getBean(GenerateComponent.class);
    scoringFilters = applicationContext.getBean(ScoringFilters.class);

    Params.of(
        "className", this.getClass().getSimpleName(),
        "ignoreExternalLinks", conf.get(PARSE_IGNORE_EXTERNAL_LINKS),
        "maxUrlLength", conf.get(PARSE_MAX_URL_LENGTH),
        "defaultAnchorLenMin", conf.get(PARSE_MIN_ANCHOR_LENGTH),
        "defaultAnchorLenMax", conf.get(PARSE_MAX_ANCHOR_LENGTH),
        "unreachableHosts", unreachableHosts.size()
    )
        .merge(scoringFilters.getParams())
        .merge(generateComponent.getParams())
        .withLogger(LOG).info();
  }

  @Override
  public void map(String reversedUrl, GWebPage row, Context context) throws IOException, InterruptedException {
    metricsCounters.increase(mRows);

    WebPage page = WebPage.box(reversedUrl, row, true);
    String url = page.getUrl();

    if (!generateComponent.shouldFetch(url, reversedUrl, page)) {
      return;
    }

    // metricsSystem.report(page);

    ScoreVector sortScore = scoringFilters.generatorSortValue(page, 1.0f);
    page.setSortScore(sortScore.toString());

    output(new SelectorEntry(url, sortScore), page, context);

    updateStatus(page);
  }

  private void output(SelectorEntry entry, WebPage page, Context context) throws IOException, InterruptedException {
    context.write(entry, page.unbox());
  }

  private void updateStatus(WebPage page) throws IOException, InterruptedException {
    if (page.isSeed()) {
      metricsCounters.increase(GenerateComponent.Counter.mSeeds);
    }

    if (page.getPageCategory().isDetail() || CrawlFilter.sniffPageCategory(page.getUrl()).isDetail()) {
      metricsCounters.increase(mDetail);
    }

    CounterUtils.increaseMDepth(page.getDistance(), metricsCounters);

    if (!page.isSeed()) {
      Instant createTime = page.getCreateTime();
      long createdDays = Duration.between(createTime, startTime).toDays();
      CounterUtils.increaseMDays(createdDays, metricsCounters);
    }

    metricsCounters.increase(mPersist);
  }
}
