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
package ai.platon.pulsar.scoring.link

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.skeleton.crawl.index.IndexDocument
import ai.platon.pulsar.skeleton.crawl.scoring.ScoringFilter
import ai.platon.pulsar.persist.WebPage

class LinkAnalysisScoringFilter(conf: ImmutableConfig) : ScoringFilter {
    private val normalizedScore = conf.getFloat("link.analyze.normalize.score", 1.00f)
    override fun getParams(): Params {
        return Params()
    }

    override fun indexerScore(url: String, doc: IndexDocument, page: WebPage, initScore: Float): Float {
        return normalizedScore * page.score
    }
}
