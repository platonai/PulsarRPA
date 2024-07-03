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
package ai.platon.pulsar.parse.js

import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.skeleton.crawl.parse.AbstractParseFilter
import ai.platon.pulsar.skeleton.crawl.parse.FilterResult
import ai.platon.pulsar.skeleton.crawl.parse.ParseResult
import ai.platon.pulsar.skeleton.crawl.parse.Parser
import ai.platon.pulsar.skeleton.crawl.parse.html.ParseContext
import ai.platon.pulsar.persist.HyperlinkPersistable
import ai.platon.pulsar.persist.WebPage

/**
 * This class is a heuristic link extractor for JavaScript files and code
 * snippets. The general idea of a two-pass regex matching comes from Heritrix.
 * Parts of the code come from OutlinkExtractor.java by Stephan Strittmatter.
 *
 * @author Andrzej Bialecki &lt;ab@getopt.org&gt;
 */
class JSParseFilter(val conf: ImmutableConfig) : AbstractParseFilter(), Parser {
    override val timeout = conf.getDuration(CapabilityTypes.PARSE_TIMEOUT, AppConstants.DEFAULT_MAX_PARSE_TIME)!!

    /**
     * Scan the JavaScript looking for possible [HyperlinkPersistable]'s
     *
     * @param parseContext Context of parse.
     */
    override fun doFilter(parseContext: ParseContext): FilterResult {
        return FilterResult.success()
    }

    override fun parse(page: WebPage): ParseResult {
        TODO("Not yet implemented")
    }
}
