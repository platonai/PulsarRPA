/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.platon.pulsar.crawl.parse

import com.google.common.collect.Lists

/**
 * This class represents a natural ordering for which parsing plugin should get
 * called for a particular mimeType. It provides methods to store the
 * parse-plugins.xml data, and methods to retreive the name of the appropriate
 * parsing plugin for a contentType.
 *
 * @author mattmann
 * @version 1.0
 */
class ParserConfig {
    /* a map to link mimeType to an ordered list of parsing plugins */
    private val mimeType2ParserClasses: MutableMap<String, List<String>> = LinkedHashMap()
    /* Aliases to class */
    var aliases: Map<String, String> = mapOf()

    fun setParsers(mimeType: String, classes: List<String>) {
        mimeType2ParserClasses[mimeType] = classes
    }

    val parsers: Map<String, List<String>>
        get() = mimeType2ParserClasses

    fun getParsers(mimeType: String): List<String> {
        return mimeType2ParserClasses[mimeType]?: listOf()
    }

    fun getClassName(aliase: String): String? {
        return aliases[aliase]
    }

    val supportedMimeTypes: List<String>
        get() = Lists.newArrayList(mimeType2ParserClasses.keys)

    override fun toString(): String {
        return mimeType2ParserClasses.toString()
    }
}