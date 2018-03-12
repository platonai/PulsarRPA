/*
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
package org.warps.pulsar.indexer.solr;

import com.google.common.collect.Lists;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.SystemDefaultHttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class SolrUtils {

    public static Logger LOG = LoggerFactory.getLogger(SolrUtils.class);
    /**
     * Make it static to avoid version dismatch problem
     */
    private static HttpClient HTTP_CLIENT = new SystemDefaultHttpClient();

    /**
     * @return SolrClient
     */
    public static ArrayList<SolrClient> getSolrClients(String[] solrUrls, String[] zkHosts, String collection) {
        ArrayList<SolrClient> solrClients = new ArrayList<>();

        for (String solrUrl : solrUrls) {
            SolrClient client = new HttpSolrClient.Builder(solrUrl)
                    .withHttpClient(HTTP_CLIENT)
                    .build();
            solrClients.add(client);
        }

        if (solrClients.isEmpty()) {
            CloudSolrClient client = getCloudSolrClient(zkHosts);
            client.setDefaultCollection(collection);
            solrClients.add(client);
        }

        return solrClients;
    }

    public static SolrClient getSolrClient(String solrUrl) {
        SolrClient client = new HttpSolrClient.Builder(solrUrl)
                .withHttpClient(HTTP_CLIENT)
                .build();
        return client;
    }

    public static CloudSolrClient getCloudSolrClient(String... zkHosts) {
        CloudSolrClient client = new CloudSolrClient.Builder()
                .withZkHost(Lists.newArrayList(zkHosts))
                .withHttpClient(HTTP_CLIENT)
                .build();

        client.setParallelUpdates(true);
        client.connect();

        return client;
    }

    public static SolrClient getHttpSolrClient(String url) {
        SolrClient client = new HttpSolrClient.Builder(url).build();
        return client;
    }
}
