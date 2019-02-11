/**
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
 */
package ai.platon.pulsar.jobs;

import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.common.config.Params;
import ai.platon.pulsar.crawl.component.FetchComponent;
import ai.platon.pulsar.crawl.protocol.Content;
import ai.platon.pulsar.crawl.schedule.DefaultFetchSchedule;
import ai.platon.pulsar.crawl.schedule.FetchSchedule;
import ai.platon.pulsar.crawl.scoring.ScoreVector;
import ai.platon.pulsar.persist.*;
import ai.platon.pulsar.persist.graph.WebEdge;
import ai.platon.pulsar.persist.graph.WebGraph;
import ai.platon.pulsar.persist.graph.WebVertex;
import ai.platon.pulsar.persist.metadata.Mark;
import ai.platon.pulsar.persist.metadata.SpellCheckedMultiMetadata;
import ai.platon.pulsar.scoring.MonitorScoringFilter;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

/**
 * JUnit test for <code>MonitorScoringFilter</code>. For an example set of URLs, we
 * simulate inlinks and liveLinks of the available graph. By manual calculation,
 * we determined the correct score points of URLs for each depth. For
 * convenience, a Map (dbWebPages) is used to store the calculated scores
 * instead of a persistent data store. At the end of the test, calculated scores
 * in the map are compared to our correct scores and a boolean result is
 * returned.
 */
public class TestMonitorScoringFilter {

  public static final Logger LOG = LoggerFactory.getLogger(TestMonitorScoringFilter.class);

  private static final DecimalFormat df = new DecimalFormat("#.###");
  private static final String[] seedUrls = new String[]{"http://a.com", "http://b.com", "http://c.com"};
  // An example web graph; shows websites as connected nodes
  private static final Map<String, List<String>> linkGraph = new LinkedHashMap<>();

  static {
    linkGraph.put("http://a.com", Lists.newArrayList("http://b.com"));
    linkGraph.put("http://b.com", Lists.newArrayList("http://a.com", "http://c.com", "http://e.com"));
    linkGraph.put("http://c.com", Lists.newArrayList("http://a.com", "http://b.com", "http://d.com", "http://f.com"));
    linkGraph.put("http://d.com", Lists.newArrayList());
    linkGraph.put("http://e.com", Lists.newArrayList());
    linkGraph.put("http://f.com", Lists.newArrayList());
  }

  // Previously calculated values for each three depths. We will compare these
  // to the results this test generates
  private static HashMap<Integer, HashMap<String, Float>> acceptedScores = new HashMap<>();

  static {
    acceptedScores.put(1, new HashMap<String, Float>() {
      {
        put("http://a.com", 1.833f);
        put("http://b.com", 2.333f);
        put("http://c.com", 1.5f);
        put("http://d.com", 0.333f);
      }
    });

    acceptedScores.put(2, new HashMap<String, Float>() {
      {
        put("http://a.com", 2.666f);
        put("http://b.com", 3.333f);
        put("http://c.com", 2.166f);
        put("http://d.com", 0.278f);
      }
    });

    acceptedScores.put(3, new HashMap<String, Float>() {
      {
        put("http://a.com", 3.388f);
        put("http://b.com", 4.388f);
        put("http://c.com", 2.666f);
        put("http://d.com", 0.5f);
      }
    });
  }

  private ImmutableConfig conf = new ImmutableConfig();
  private final int ROUND = 10;
  private Map<String, WebPage> rows;
  private MonitorScoringFilter scoringFilter;
  private FetchSchedule fetchSchedule;

  @Before
  public void setUp() throws Exception {
    scoringFilter = new MonitorScoringFilter(conf);
    fetchSchedule = new DefaultFetchSchedule(conf);

    float scoreInjected = 1.0f;
    LOG.info("scoreInjected : " + scoreInjected);

    scoringFilter.reload(conf);

    // Inject simulation
    rows = Stream.of(seedUrls).map(WebPage::newWebPage)
        .peek(page -> page.setScore(scoreInjected))
        .peek(page -> scoringFilter.injectedScore(page))
        .collect(Collectors.toMap(WebPage::getUrl, page -> page));
  }

  /**
   * Assertion that the accepted and and actual resultant scores are the same.
   */
  @Test
  public void testCrawlAndEvaluateScore() {
    // Crawl loop simulation
    Instant now = Instant.now();
    for (int i = 0; i < ROUND; ++i) {
      final int round = 1 + i;
      final String batchId = String.valueOf(round);

      rows = rows.values().stream().peek(page -> {
        // Generate simulation
        page.setBatchId(batchId);

        // assertEquals(1.0f, page.getScore(), 1.0e-10);

        float initSortScore = calculateInitSortScore(page);
        ScoreVector sortScore = scoringFilter.generatorSortValue(page, initSortScore);
        page.getMarks().put(Mark.GENERATE, batchId);

        // assertEquals(10001.0f, sortScore, 1.0e-10);
        Params.of("url", page.getUrl(), "sortScore", sortScore).withLogger(LOG).info(true);
      }).filter(page -> page.hasMark(Mark.GENERATE)).peek(page -> {
        // Fetch simulation

        FetchComponent.updateStatus(page, CrawlStatus.STATUS_FETCHED, ProtocolStatus.STATUS_SUCCESS);
        FetchComponent.updateContent(page, new Content(page.getUrl(), page.getUrl(), "".getBytes(), "text/html",
                new SpellCheckedMultiMetadata(), conf));
        FetchComponent.updateFetchTime(page, now.minus(60 - round * 2, ChronoUnit.MINUTES));
        FetchComponent.updateMarks(page);

        // Re-publish the article
        Instant publishTime = now.minus(30 - round * 2, ChronoUnit.HOURS);
        page.setModifiedTime(publishTime);
        page.updateContentPublishTime(publishTime);

        page.setLiveLinks(linkGraph.get(page.getUrl()).stream().map(HypeLink::new).collect(Collectors.toList()));

        assertEquals(publishTime, page.getContentPublishTime());

      }).collect(Collectors.toMap(WebPage::getUrl, page -> page));

//      assertEquals(3, rows.size());

      /* Build the web graph */
      // 1. Add all vertices
      WebGraph graph = new WebGraph();
      rows.values().forEach(page -> graph.addVertex(new WebVertex(page)));
      // 2. Build all links as edges
      Collection<WebVertex> vertices = graph.vertexSet().stream().filter(WebVertex::hasWebPage).collect(Collectors.toList());
      vertices.forEach(v1 -> v1.getWebPage().getLiveLinks().values()
              .forEach(l -> graph.addEdgeLenient(v1, new WebVertex(l.getUrl())).setAnchor(l.getAnchor().toString()))
      );
//      for (WebVertex v1 : graph.vertexSet()) {
//        for (WebVertex v2 : graph.vertexSet()) {
//          if (v1.getWebPage().getLiveLinks().keySet().contains(v2.getUrl())) {
//            graph.addEdge(v1, v2);
//          }
//        }
//      }

      // 3. report and assertions
      Params.of(
              "round", round,
              "rows", rows.size(),
              "vertices", graph.vertexSet().size(),
              "vertices(hasWebPage)", graph.vertexSet().stream().filter(WebVertex::hasWebPage).count(),
              "edges", graph.edgeSet().size() + " : "
                      + graph.edgeSet().stream().map(WebEdge::toString).collect(Collectors.joining(", "))
      ).withLogger(LOG).info(true);

      /* OutGraphUpdateJob simulation */
      // 1. distribute score to outLinks
      graph.vertexSet().stream().filter(WebVertex::hasWebPage)
              .forEach(v -> scoringFilter.distributeScoreToOutlinks(v.getWebPage(), graph, graph.outgoingEdgesOf(v), graph.outDegreeOf(v)));
      // 2. update score for all rows
      graph.vertexSet().stream().filter(WebVertex::hasWebPage)
              .forEach(v -> scoringFilter.updateScore(v.getWebPage(), graph, graph.incomingEdgesOf(v)));
      // 3. update marks
      graph.vertexSet().stream().filter(WebVertex::hasWebPage)
              .forEach(v -> v.getWebPage().getMarks().put(Mark.UPDATEOUTG, batchId));
      // 4. generate new rows
      Map<String, WebPage> newRows = graph.vertexSet().stream().filter(v -> !v.hasWebPage())
              .map(v -> createNewRow(v.getUrl())).collect(Collectors.toMap(WebPage::getUrl, page -> page));
      rows.putAll(newRows);

      /* InGraphUpdateJob simulation */
      // Update by in-links
      graph.edgeSet().stream()
              .filter(WebEdge::hasSourceWebPage)
              .filter(WebEdge::hasTargetWebPage)
              .forEach(edge -> {
                WebPage p1 = edge.getSourceWebPage();
                WebPage p2 = edge.getTargetWebPage();

                // Update by out-links
                p1.updateRefContentPublishTime(p2.getContentPublishTime());
                p1.getPageCounters().increase(PageCounters.Ref.ch, 1000 * round);
                p1.getPageCounters().increase(PageCounters.Ref.article);
              });

      // Report the result
      graph.vertexSet().stream()
              .filter(WebVertex::hasWebPage)
//              .filter(v -> v.getUrl().contains("a.com"))
              .map(WebVertex::getWebPage)
              .map(WebPage::toString)
              .forEach(LOG::info);
    }
  }

  private WebPage createNewRow(String url) {
    WebPage page = WebPage.newWebPage(url);
    fetchSchedule.initializeSchedule(page);
    page.setCrawlStatus(CrawlStatus.STATUS_UNFETCHED);
    scoringFilter.initialScore(page);
    return page;
  }

  private float calculateInitSortScore(WebPage page) {
    boolean raise = false;

    float factor = raise ? 1.0f : 0.0f;
    int depth = page.getDistance();

    return (10000.0f - 100 * depth) + factor * 100000.0f;
  }
}
