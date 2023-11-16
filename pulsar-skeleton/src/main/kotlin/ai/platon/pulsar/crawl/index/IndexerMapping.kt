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
package ai.platon.pulsar.crawl.index

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.crawl.index.IndexerMapping
import com.google.common.collect.Lists
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.w3c.dom.Element
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import java.io.IOException
import java.util.*
import java.util.stream.Collectors
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException

class IndexerMapping(private val mappingFile: String, private val conf: ImmutableConfig) {
    private val keyMap: MutableMap<String, MappingField> = HashMap()
    private var uniqueKey = "id"

    constructor(conf: ImmutableConfig) : this(conf[PARAM_INDEXER_MAPPING_FILE, "indexer-mapping.xml"], conf) {}

    private fun parseMapping() {
        val ssInputStream = conf.getConfResourceAsInputStream(mappingFile)
        val solrFields: MutableList<String?> = Lists.newArrayList()
        val inputSource = InputSource(ssInputStream)
        try {
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val document = builder.parse(inputSource)
            val rootElement = document.documentElement
            val fieldList = rootElement.getElementsByTagName("field")
            if (fieldList.length > 0) {
                for (i in 0 until fieldList.length) {
                    val element = fieldList.item(i) as Element
                    val name = element.getAttribute("name")
                    val type = element.getAttribute("type")
                    val indexed = element.getAttribute("indexed")
                    val stored = element.getAttribute("stored")
                    val required = element.getAttribute("required")
                    val multiValued = element.getAttribute("multiValued")
                    val bIndexed = indexed.equals("true", ignoreCase = true)
                    val bStored = stored.equals("true", ignoreCase = true)
                    val bRequired = required.equals("true", ignoreCase = true)
                    val bMultiValued = multiValued.equals("true", ignoreCase = true)
                    val mappingFiled = MappingField(name, type, bIndexed, bStored, bRequired, bMultiValued)
                    solrFields.add(name)
                    keyMap[name] = mappingFiled
                }
            }
            LOG.info("Registered " + solrFields.size + " solr fields : " + StringUtils.join(solrFields, ", "))
            val uniqueKeyItem = rootElement.getElementsByTagName("uniqueKey")
            if (uniqueKeyItem.length > 1) {
                LOG.warn("More than one unique key definitions found in solr index mapping, using default 'id'")
                uniqueKey = "id"
            } else if (uniqueKeyItem.length == 0) {
                LOG.warn("No unique key definition found in solr index mapping using, default 'id'")
            } else {
                uniqueKey = uniqueKeyItem.item(0).firstChild.nodeValue
            }
        } catch (e: SAXException) {
            LOG.warn(e.toString())
        } catch (e: IOException) {
            LOG.warn(e.toString())
        } catch (e: ParserConfigurationException) {
            LOG.warn(e.toString())
        }
    }

    fun getKeyMap(): Map<String, MappingField> {
        return keyMap
    }

    @Throws(IOException::class)
    fun mapKeyIfExists(key: String): String? {
        return if (keyMap.containsKey(key)) {
            key
        } else null
    }

    @Throws(IOException::class)
    fun isMultiValued(key: String): Boolean {
        return keyMap.containsKey(key) && keyMap[key]!!.multiValued
    }

    fun reportKeys(): String {
        return keyMap.keys.stream().collect(Collectors.joining(", "))
    }

    /**
     * We do not map a name to another for solr
     */
    inner class MappingField internal constructor(
        var name: String,
        type: String,
        indexed: Boolean,
        stored: Boolean,
        required: Boolean,
        multiValued: Boolean,
    ) {
        var mappedName: String
        var type: String
        var indexed: Boolean
        var stored: Boolean
        var required: Boolean
        var multiValued: Boolean

        init {
            mappedName = name
            this.type = type
            this.indexed = indexed
            this.stored = stored
            this.required = required
            this.multiValued = multiValued
        }
    }

    companion object {
        const val PARAM_INDEXER_MAPPING_FILE = "indexer.mapping.file"
        var LOG = LoggerFactory.getLogger(IndexerMapping::class.java)
    }

    init {
        parseMapping()
    }
}