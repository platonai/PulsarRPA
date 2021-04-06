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
package ai.platon.pulsar.normalizer

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.crawl.filter.CrawlUrlNormalizer

/**
 * This UrlNormalizer doesn't change urls. It is sometimes useful if for a given
 * scope at least one normalizer must be defined but no transformations are
 * required.
 *
 * @author Andrzej Bialecki
 */
class PassUrlNormalizer(conf: ImmutableConfig?) : CrawlUrlNormalizer {

    override fun normalize(url: String, scope: String): String? {
        return url
    }

    override fun valid(urlString: String, scope: String): Boolean {
        return normalize(urlString, scope) != null
    }
}
