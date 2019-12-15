/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.platon.pulsar.persist.graph

import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.MultiMetadata
import ai.platon.pulsar.persist.metadata.Name
import org.jgrapht.graph.DefaultWeightedEdge

class WebEdge(
        var anchor: String = "",
        var options: String = "",
        var order: Int = -1
) : DefaultWeightedEdge() {
    var metadata = MultiMetadata()

    fun putMetadata(k: Name, v: String) {
        metadata.put(k.text(), v)
    }

    fun putMetadata(k: String, v: String) {
        metadata.put(k, v)
    }

    fun getMetadata(k: Name, defaultValue: String): String {
        val value = metadata[k.text()]
        return value ?: defaultValue
    }

    fun hasMetadata(k: Name): Boolean {
        return metadata[k.text()] != null
    }

    val isLoop: Boolean
        get() = source == target

    /**
     * Retrieves the source of this edge.
     *
     * @return source of this edge
     */
    public override fun getSource(): WebVertex {
        return super.getSource() as WebVertex
    }

    val sourceUrl: String
        get() = source.url

    val sourceWebPage: WebPage?
        get() = source.page

    fun hasSourceWebPage(): Boolean {
        return source.hasWebPage()
    }

    /**
     * Retrieves the target of this edge.
     *
     * @return target of this edge
     */
    public override fun getTarget(): WebVertex {
        return super.getTarget() as WebVertex
    }

    val targetUrl: String
        get() = target.url

    val targetWebPage: WebPage?
        get() = target.page

    fun hasTargetWebPage(): Boolean {
        return target.hasWebPage()
    }

    override fun toString(): String {
        return "$sourceUrl -> $targetUrl"
    }
}