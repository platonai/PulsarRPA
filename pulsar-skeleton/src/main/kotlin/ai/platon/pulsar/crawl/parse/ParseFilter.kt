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

import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.crawl.parse.html.ParseContext
import java.util.concurrent.atomic.AtomicInteger

/**
 * Extension point for DOM-based parsers. Permits one to add additional metadata
 * to parses provided by the html or tika plugins. All plugins found which
 * implement this extension point are run sequentially on the parse.
 */
interface ParseFilter : Parameterized, AutoCloseable {
    val id: Int

    var parent: ParseFilter?
    val children: List<ParseFilter>

    val parentId get() = parent?.id?:0
    val isRoot get() = parent == null
    val isLeaf get() = children.isEmpty()

    /**
     * Adds metadata or otherwise modifies a parseResult, given the DOM tree of a page.
     */
    fun filter(parseContext: ParseContext): ParseResult

    fun addFirst(child: ParseFilter)

    fun addLast(child: ParseFilter)

    override fun close() {}
}
