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
package ai.platon.pulsar.solr

import org.apache.http.impl.client.SystemDefaultHttpClient
import org.apache.solr.client.solrj.SolrClient
import org.apache.solr.client.solrj.impl.HttpSolrClient
import ai.platon.pulsar.solr.SolrUtils
import com.google.common.collect.Lists
import org.apache.http.client.HttpClient
import org.apache.solr.client.solrj.impl.CloudSolrClient
import org.slf4j.LoggerFactory
import java.util.ArrayList

object SolrUtils {
    var LOG = LoggerFactory.getLogger(SolrUtils::class.java)

    /**
     * Make it static to avoid version dismatch problem
     */
    private val HTTP_CLIENT: HttpClient = SystemDefaultHttpClient()

    /**
     * @return SolrClient
     */
    fun getSolrClients(solrUrls: Array<String?>, zkHosts: Array<String?>, collection: String?): ArrayList<SolrClient> {
        val solrClients = ArrayList<SolrClient>()
        for (solrUrl in solrUrls) {
            val client: SolrClient = HttpSolrClient.Builder(solrUrl)
                .withHttpClient(HTTP_CLIENT)
                .build()
            solrClients.add(client)
        }
        if (solrClients.isEmpty()) {
            val client = getCloudSolrClient(*zkHosts)
            client.defaultCollection = collection
            solrClients.add(client)
        }
        return solrClients
    }

    fun getSolrClient(solrUrl: String?): SolrClient {
        return HttpSolrClient.Builder(solrUrl)
            .withHttpClient(HTTP_CLIENT)
            .build()
    }

    fun getCloudSolrClient(vararg zkHosts: String?): CloudSolrClient {
        val client = CloudSolrClient.Builder()
            .withZkHost(Lists.newArrayList(*zkHosts))
            .withHttpClient(HTTP_CLIENT)
            .build()
        client.setParallelUpdates(true)
        client.connect()
        return client
    }

    fun getHttpSolrClient(url: String?): SolrClient {
        return HttpSolrClient.Builder(url).build()
    }
}