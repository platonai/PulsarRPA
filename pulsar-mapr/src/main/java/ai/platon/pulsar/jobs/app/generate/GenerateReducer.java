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
import ai.platon.pulsar.common.config.Params;
import ai.platon.pulsar.crawl.filter.CrawlFilter;
import ai.platon.pulsar.jobs.app.fetch.FetchMapper;
import ai.platon.pulsar.jobs.common.SelectorEntry;
import ai.platon.pulsar.jobs.core.AppContextAwareGoraReducer;
import ai.platon.pulsar.persist.WebPage;
import ai.platon.pulsar.persist.gora.generated.GWebPage;
import ai.platon.pulsar.persist.metadata.Mark;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import org.slf4j.Logger;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

import static ai.platon.pulsar.common.CommonCounter.*;
import static ai.platon.pulsar.common.PulsarParams.*;
import static ai.platon.pulsar.common.UrlUtil.reverseUrl;
import static ai.platon.pulsar.common.config.CapabilityTypes.*;
import static ai.platon.pulsar.common.config.PulsarConstants.ALL_BATCHES;
import static java.time.temporal.ChronoUnit.DAYS;

/**
 * Reduce class for generate
 *
 * The #reduce() method write a random integer to all generated URLs. This
 * random number is then used by {@link FetchMapper}.
 */
public class GenerateReducer extends AppContextAwareGoraReducer<SelectorEntry, GWebPage, String, GWebPage> {

  public static final Logger LOG = GenerateJob.LOG;

  private enum Counter { rHosts, rMalformedUrl, rSeeds, rFromSeed }
  static { MetricsCounters.register(Counter.class); }

  private Instant impreciseNow = Instant.now();
  private LocalDateTime middleNight = LocalDateTime.now().truncatedTo(DAYS);
  private Instant middleNightInstant = Instant.now().truncatedTo(DAYS);
  private int limit;
  private int maxCountPerHost;
  private URLUtil.GroupMode groupMode;
  private Multiset<String> hostNames = LinkedHashMultiset.create();
  private String batchId;
  private MetricsSystem metricsSystem;
  private int count = 0;

  @Override
  protected void setup(Context context) {
    String crawlId = conf.get(STORAGE_CRAWL_ID);
    batchId = conf.get(BATCH_ID, ALL_BATCHES);

    // Generate top N links only
    limit = conf.getUint(GENERATE_TOP_N, Integer.MAX_VALUE);
    limit /= context.getNumReduceTasks();

    maxCountPerHost = conf.getUint(GENERATE_MAX_TASKS_PER_HOST, 100000);
    groupMode = conf.getEnum(FETCH_QUEUE_MODE, URLUtil.GroupMode.BY_HOST);

    metricsSystem = applicationContext.getBean(MetricsSystem.class);

    LOG.info(Params.format(
        "className", this.getClass().getSimpleName(),
        "crawlId", crawlId,
        "batchId", batchId,
        "limit", limit,
        "maxCountPerHost", maxCountPerHost,
        "groupMode", groupMode
    ));
  }

  @Override
  protected void reduce(SelectorEntry key, Iterable<GWebPage> rows, Context context) throws IOException, InterruptedException {
    metricsCounters.increase(rRows);

    String url = key.getUrl();
    String host = URLUtil.getHost(url, groupMode);
    if (host.isEmpty()) {
      metricsCounters.increase(Counter.rMalformedUrl);
      return;
    }

    for (GWebPage row : rows) {
      WebPage page = WebPage.box(url, row);

      try {
        if (limit > 0 && count >= limit) {
          stop("Generated enough pages, quit generator");
          break;
        }

        addGeneratedHosts(host);
        if (hostNames.count(host) > maxCountPerHost) {
          LOG.warn("Too many urls in host {}, ignore ...", host);
          break;
        }

        updatePage(page);

        context.write(reverseUrl(url), page.unbox());

        ++count;

        updateStatus(page, context);

        metricsSystem.debugSortScore(page);
      }
      catch (Throwable e) {
        LOG.error(StringUtil.stringifyException(e));
      }
    } // for
  }

  private void updatePage(WebPage page) {
    page.setBatchId(batchId);
    page.setGenerateTime(startTime);

    page.getMarks().remove(Mark.INJECT);
    page.getMarks().put(Mark.GENERATE, batchId);
  }

  private void updateStatus(WebPage page, Context context) throws IOException {
    CounterUtils.increaseRDepth(page.getDistance(), metricsCounters);

    if (!page.isSeed()) {
      Instant createTime = page.getCreateTime();
      long createdDays = Duration.between(createTime, startTime).toDays();
      CounterUtils.increaseRDays(createdDays, metricsCounters);
    }

    // double check (depth == 0 or has IS-SEED metadata) , can be removed later
    if (page.isSeed()) {
      metricsCounters.increase(Counter.rSeeds);
    }

    if (page.getPageCategory().isDetail() || CrawlFilter.sniffPageCategory(page.getUrl()).isDetail()) {
      metricsCounters.increase(rDetail);
    }

    metricsCounters.increase(rPersist);
  }

  @Override
  protected void cleanup(Context context) {
    LOG.info("Generated total " + hostNames.elementSet().size() + " hosts/domains");
    metricsSystem.reportGeneratedHosts(hostNames.elementSet());
  }

  private void addGeneratedHosts(String host) {
    hostNames.add(host);

    getPulsarCounters().setValue(Counter.rHosts, hostNames.entrySet().size());
  }
}
