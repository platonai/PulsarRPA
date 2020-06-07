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
package ai.platon.pulsar.crawl.parse

import ai.platon.pulsar.common.FlowState
import ai.platon.pulsar.persist.HypeLink
import ai.platon.pulsar.persist.ParseStatus
import ai.platon.pulsar.persist.metadata.ParseStatusCodes
import ai.platon.pulsar.persist.model.DomStatistics
import ai.platon.pulsar.persist.model.LabeledHyperLink
import java.util.*
import java.util.concurrent.ConcurrentSkipListSet
import kotlin.collections.HashSet

class ParseResult(
        majorCode: Short = NOTPARSED,
        minorCode: Int = SUCCESS_OK,
        message: String? = null
) : ParseStatus(majorCode, minorCode, message) {
    val hypeLinks = mutableSetOf<HypeLink>()
    var domStatistics: DomStatistics? = null
    var parser: Parser? = null
    var flowStatus = FlowState.CONTINUE

    val shouldContinue get() = flowStatus == FlowState.CONTINUE
    val shouldBreak get() = flowStatus == FlowState.BREAK

    companion object {
        val labeledHypeLinks = ConcurrentSkipListSet<LabeledHyperLink>()

        fun failed(minorCode: Int, message: String?): ParseResult {
            return ParseResult(ParseStatusCodes.FAILED, minorCode, message)
        }

        fun failed(e: Throwable): ParseResult {
            return ParseResult(ParseStatusCodes.FAILED, ParseStatusCodes.FAILED_EXCEPTION, e.message)
        }
    }
}
