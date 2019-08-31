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
package ai.platon.pulsar.rest.resources;

import ai.platon.pulsar.common.Urls;
import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.common.config.Params;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.SystemDefaultHttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.stream.Collectors;

import static ai.platon.pulsar.common.config.CapabilityTypes.INDEXER_URL;

@RestController
@RequestMapping("/solr")
public class SolrResource {

  private final ImmutableConfig conf;
  private static HttpClient HTTP_CLIENT = new SystemDefaultHttpClient();

  private SolrClient solrClient;
  private int rows = 100;
  private String sort = "publish_time desc";
  private String fields = "article_title,encoding,resource_category,author,director,last_crawl_time,publish_time,domain,url";

  @Autowired
  public SolrResource(ImmutableConfig conf) {
    String indexerUrl = conf.get(INDEXER_URL);
    // solrClient = SolrUtils.getSolrClient(indexerUrl);
    solrClient = new HttpSolrClient.Builder(INDEXER_URL)
            .withHttpClient(HTTP_CLIENT)
            .build();
    this.conf = conf;
  }

  @GetMapping("/get")
  public String get(@PathVariable("key") String key) throws IOException, SolrServerException {
    SolrDocument doc = solrClient.getById(Urls.reverseUrlOrEmpty(key));
    return format(doc);
  }

  @GetMapping("/p24h")
  public ArrayList<SolrDocument> p24h() throws IOException, SolrServerException {
    QueryResponse response = runQuery(Params.of("publish_time", "[NOW-DAY/DAY TO NOW]"));
    return response.getResults();
  }

  @GetMapping("/p48h")
  public ArrayList<SolrDocument> p48h() throws IOException, SolrServerException {
    QueryResponse response = runQuery(Params.of("publish_time", "[NOW-2DAY/DAY TO NOW]"));
    return response.getResults();
  }

  @GetMapping("/c24h")
  public ArrayList<SolrDocument> c24h() throws IOException, SolrServerException {
    QueryResponse response = runQuery(Params.of("last_crawl_time", "[NOW-DAY/DAY TO NOW]"));
    return response.getResults();
  }

  @GetMapping("/c48h")
  public ArrayList<SolrDocument> c48h() throws IOException, SolrServerException {
    QueryResponse response = runQuery(Params.of("last_crawl_time", "[NOW-2DAY/DAY TO NOW]"));
    return response.getResults();
  }

  @GetMapping("/truncate")
  public boolean truncate(@PathVariable("collection") String collection) throws IOException, SolrServerException {
    solrClient.deleteByQuery(collection, "*:*");
    return true;
  }

  private String format(SolrDocument document) {
    return document.entrySet().stream().map(e -> e.getKey() + ":\t" + e.getValue()).collect(Collectors.joining(",\n"));
  }

  private QueryResponse runQuery(Params queryParams) throws IOException, SolrServerException {
    String q = queryParams.asMap().entrySet()
            .stream().map(e -> e.getKey() + ":" + e.getValue()).collect(Collectors.joining(" AND "));
    SolrQuery query = newSolrQuery().setQuery(q);
    return solrClient.query(query);
  }

  private SolrQuery newSolrQuery() {
    SolrQuery solrQuery = new SolrQuery();
    solrQuery
            .setParam("q", "publish_time:[NOW-1DAY/DAY TO NOW] AND article_title:[\"\" TO *]")
            .setParam("fl", fields)
            .setParam("sort", sort)
            .setParam("rows", String.valueOf(rows))
            .setParam("TZ", "Asia/Shanghai")
            .setParam("indent", "on")
            .setParam("wt", "json");
    return solrQuery;
  }
}
