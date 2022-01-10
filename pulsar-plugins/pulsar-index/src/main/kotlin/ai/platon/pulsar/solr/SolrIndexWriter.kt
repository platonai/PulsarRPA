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

import ai.platon.pulsar.common.DateTimes.isoInstantFormat
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.crawl.index.IndexDocument
import ai.platon.pulsar.crawl.index.IndexWriter
import ai.platon.pulsar.crawl.index.IndexerMapping
import ai.platon.pulsar.persist.HyperlinkPersistable
import ai.platon.pulsar.persist.WebDb
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.solr.SolrIndexWriter
import com.google.common.collect.Lists
import org.apache.avro.util.Utf8
import org.apache.commons.lang3.ArrayUtils
import org.apache.commons.lang3.StringUtils
import org.apache.solr.client.solrj.SolrClient
import org.apache.solr.client.solrj.SolrServerException
import org.apache.solr.client.solrj.request.AbstractUpdateRequest
import org.apache.solr.client.solrj.request.UpdateRequest
import org.apache.solr.common.SolrException
import org.apache.solr.common.SolrInputDocument
import org.apache.solr.common.params.ModifiableSolrParams
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.time.Instant
import java.util.*

class SolrIndexWriter(private val indexerMapping: IndexerMapping, conf: ImmutableConfig) : IndexWriter {
    private val inputDocs: MutableList<SolrInputDocument> = ArrayList()
    private val updateDocs: List<SolrInputDocument> = ArrayList()
    private val deleteIds: MutableList<String> = ArrayList()
    private var conf: ImmutableConfig? = null
    private var solrUrls = ArrayUtils.EMPTY_STRING_ARRAY
    private var zkHosts = ArrayUtils.EMPTY_STRING_ARRAY
    private var collection: String? = null
    private var solrClients: MutableList<SolrClient>? = null
    private var solrParams: ModifiableSolrParams? = null
    var webDb: WebDb? = null
    override var isActive = false
        private set
    private var batchSize = 0
    private var numDeletes = 0
    private var totalAdds = 0
    private var totalDeletes = 0
    private val totalUpdates = 0
    private var delete = false
    private val writeFile = false
    override fun setup(conf: ImmutableConfig) {
        this.conf = conf
        solrUrls = conf.getStrings(CapabilityTypes.INDEXER_URL, *ArrayUtils.EMPTY_STRING_ARRAY)
        zkHosts = conf.getStrings(CapabilityTypes.INDEXER_ZK, *ArrayUtils.EMPTY_STRING_ARRAY)
        collection = conf[CapabilityTypes.INDEXER_COLLECTION]
        if (solrUrls == null && zkHosts == null) {
            var message = "Either Zookeeper URL or SOLR URL is required"
            message += """
                
                ${describe()}
                """.trimIndent()
            LOG.error(message)
            throw RuntimeException("Failed to init SolrIndexWriter")
        }
        batchSize = conf.getInt(CapabilityTypes.INDEXER_WRITE_COMMIT_SIZE, 250)
        delete = conf.getBoolean(INDEXER_DELETE, false)
        solrParams = ModifiableSolrParams()
        conf.getKvs(INDEXER_PARAMS).forEach { (key: String?, value: String?) -> solrParams!!.add(key, value) }
        LOG.info(params.format())
    }

    override fun getParams(): Params {
        return Params.of(
            "className", this.javaClass.simpleName,
            "batchSize", batchSize,
            "delete", delete,
            "solrParams", solrParams,
            "zkHosts", StringUtils.join(zkHosts, ", "),
            "solrUrls", StringUtils.join(solrUrls, ", "),
            "collection", collection
        )
    }

    override fun open(conf: ImmutableConfig?) {
        solrClients = SolrUtils.getSolrClients(solrUrls, zkHosts, collection)
        isActive = true
    }

    override fun open(solrUrl: String?) {
        solrClients = Lists.newArrayList(SolrUtils.getSolrClient(solrUrl))
        isActive = true
    }

    @Throws(IOException::class)
    fun deleteByQuery(query: String) {
        try {
            LOG.info("SolrWriter: deleting $query")
            for (solrClient in solrClients!!) {
                solrClient.deleteByQuery(query)
            }
        } catch (e: SolrServerException) {
            LOG.error("Error deleting: $deleteIds")
            throw IOException(e)
        }
    }

    @Throws(IOException::class)
    override fun delete(key: String?) {
        var key = key
        try {
            key = URLDecoder.decode(key, "UTF8")
        } catch (e: UnsupportedEncodingException) {
            LOG.error("Error decoding: $key")
            throw IOException("UnsupportedEncodingException for $key")
        } catch (e: IllegalArgumentException) {
            LOG.warn("Could not decode: $key, it probably wasn't encoded in the first place..")
        }

        // Escape solr hash separator
        key = key!!.replace("!".toRegex(), "\\!")
        if (delete) {
            deleteIds.add(key)
            totalDeletes++
        }
        if (deleteIds.size >= batchSize) {
            push()
        }
    }

    @Throws(IOException::class)
    override fun update(doc: IndexDocument?) {
        write(doc)
    }

    @Throws(IOException::class)
    override fun write(doc: IndexDocument?) {
        val inputDoc = SolrInputDocument()
        for ((key1, value) in doc!!.fields) {
            val key = indexerMapping.mapKeyIfExists(key1) ?: continue
            val weight = value.getWeight()
            for (field in value.getValues()) {
                // normalise the string representation for a Date
                val val2 = convertIndexField(field)
                val isMultiValued = indexerMapping.isMultiValued(key1)
                if (!isMultiValued) {
                    if (inputDoc.getField(key) == null) {
                        inputDoc.addField(key, val2, weight)
                    }
                } else {
                    inputDoc.addField(key, val2, weight)
                }
            } // for
        } // for
        inputDoc.documentBoost = doc.weight
        inputDocs.add(inputDoc)
        totalAdds++
        if (inputDocs.size + numDeletes >= batchSize) {
            push()
        }
    }

    private fun convertIndexField(field: Any): Any {
        val field2: Any
        if (field is Date) {
            field2 = isoInstantFormat(field)
        } else if (field is Instant) {
            field2 = isoInstantFormat(field)
        } else if (field is Utf8) {
            field2 = field.toString()
        }
        return field
    }

    @Throws(IOException::class)
    override fun close() {
        if (!isActive) {
            return
        }
        commit()
        for (solrClient in solrClients!!) {
            solrClient.close()
        }
        solrClients!!.clear()
        isActive = false
    }

    @Throws(IOException::class)
    override fun commit() {
        if (!isActive || inputDocs.isEmpty()) {
            return
        }
        push()
        try {
            for (solrClient in solrClients!!) {
                solrClient.commit()
            }
        } catch (e: SolrServerException) {
            LOG.error("Failed to write to solr $e")
            LOG.info(describe())
            throw IOException(e)
        } catch (e: SolrException) {
            LOG.error("Failed to write to solr $e")
            LOG.info(describe())
            throw IOException(e)
        }
    }

    @Throws(IOException::class)
    fun push() {
        if (inputDocs.size > 0) {
            var message = "Indexing " + inputDocs.size + "/" + totalAdds + " documents"
            if (numDeletes > 0) {
                message += ", deleting $numDeletes ones"
            }
            LOG.info(message)
            numDeletes = 0
            val req = UpdateRequest()
            req.add(inputDocs)
            req.setAction(AbstractUpdateRequest.ACTION.OPTIMIZE, false, false)
            req.params = solrParams
            try {
                for (solrClient in solrClients!!) {
                    solrClient.request(req)
                }
            } catch (e: SolrServerException) {
                LOG.error("Failed to write to solr $e")
                LOG.info(describe())
                throw IOException(e)
            } catch (e: SolrException) {
                LOG.error("Failed to write to solr $e")
                LOG.info(describe())
                throw IOException(e)
            } finally {
                reportFailure(inputDocs)
                inputDocs.clear()
            }
        }
        if (deleteIds.size > 0) {
            try {
                LOG.info("SolrIndexer: deleting "
                        + deleteIds.size.toString() + "/" + totalDeletes.toString() + " documents")
                for (solrClient in solrClients!!) {
                    solrClient.deleteById(deleteIds)
                }
            } catch (e: SolrServerException) {
                LOG.error("Error deleting: $deleteIds")
                throw IOException(e)
            } catch (e: SolrException) {
                LOG.error("Error deleting: $deleteIds")
                throw IOException(e)
            } finally {
                // reportFailure(deleteIds);
                deleteIds.clear()
            }
        }
    }

    private fun reportFailure(failedDocs: List<SolrInputDocument>) {
        if (webDb == null) return

        // TODO: use TaskStatusTracker
        val page = WebPage.newInternalPage(INDEXER_REPORT_PAGE_HOME + "/solr/failure", "Failed solr indexing pages")
        failedDocs.stream()
            .map { doc: SolrInputDocument ->
                doc["url"]!!
                    .value.toString()
            }
            .map { url: String? ->
                HyperlinkPersistable(
                    url!!)
            }
            .forEach { l: HyperlinkPersistable -> page.vividLinks[l.url] = "" }
        webDb!!.put(page)
        webDb!!.flush()
    }

    override fun describe(): String? {
        val sb = StringBuilder("SOLRIndexWriter\n")
        sb.append("\t").append(CapabilityTypes.INDEXER_URL).append(" : URL of the indexer instance\n")
        sb.append("\t").append(CapabilityTypes.INDEXER_ZK).append(" : URL of the Zookeeper quorum\n")
        sb.append("\t").append(CapabilityTypes.INDEXER_COLLECTION).append(" : indexer collection\n")
        sb.append("\t").append(CapabilityTypes.INDEXER_WRITE_COMMIT_SIZE)
            .append(" : buffer size when sending to SOLR (default 1000)\n")
        sb.append("\t").append(IndexerMapping.PARAM_INDEXER_MAPPING_FILE)
            .append(" : name of the mapping file for fields (default solrindex-mapping.xml)\n")
        sb.append("\t").append(SolrConstants.USE_AUTH).append(" : use authentication (default false)\n")
        sb.append("\t").append(SolrConstants.USERNAME).append(" : username for authentication\n")
        sb.append("\t").append(SolrConstants.PASSWORD).append(" : password for authentication\n")
        return sb.toString()
    }

    companion object {
        val LOG = LoggerFactory.getLogger(SolrIndexWriter::class.java)
        const val INDEXER_PARAMS = "index.additional.params"
        const val INDEXER_DELETE = "index.delete"
        const val INDEXER_REPORT_PAGE_HOME = "http://pulsar.platon.ai/report/indexer"
    }

    init {
        setup(conf)
    }
}
