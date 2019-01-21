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
package `fun`.platonic.pulsar.persist.experimental

import `fun`.platonic.pulsar.persist.*
import `fun`.platonic.pulsar.persist.WebPage.impreciseNow
import `fun`.platonic.pulsar.persist.metadata.PageCategory
import java.nio.ByteBuffer
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

val impreciseNow = Instant.now()
val impreciseTomorrow = impreciseNow.plus(1, ChronoUnit.DAYS)
val imprecise2DaysAhead = impreciseNow.plus(2, ChronoUnit.DAYS)
val middleNight = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS)
val middleNightInstant = Instant.now().truncatedTo(ChronoUnit.DAYS)
val defaultZoneId = ZoneId.systemDefault()

/**
 * The core data structure across the whole program execution
 *
 *
 * Notice: Use a build-in java string or a Utf8 to serialize strings?
 *
 * @see org.apache.gora.hbase.util.HBaseByteInterface.fromBytes
 *
 *
 * In serializetion phrase, a byte array created by s.getBytes
 */
/**
 * Experimental, do not use this class
 * We are looking for a better way to represent a WebPage
 * */
interface IWebPage {
    val url: String
    var createTime: Instant
    var distance: Int
    var fetchCount: Int
    var fetchPriority: Int
    var fetchInterval: Duration
    var zoneId: ZoneId
    var options: String?
    var batchId: String
    var crawlStatus: CrawlStatus
    var prevFetchTime: Instant
    var fetchTime: Instant
    var fetchRetries: Int
    var reprUrl: String?
    var prevModifiedTime: Instant
    var modifiedTime: Instant
    var protocolStatus: ProtocolStatus
    var encoding: String?
    var contentType: String?
    /** The entire raw document content e.g. raw XHTML  */
    var content: ByteBuffer?
    var baseUrl: String?
    var referrer: String?
    var anchor: String?
    var anchorOrder: Int
    var parseStatus: ParseStatus
    var pageTitle: String?
    var pageText: String?
    var contentTitle: String?
    var contentText: String?
    var contentTextLen: Int
    var pageCategory: PageCategory
    var contentModifiedTime: Instant
    var prevContentModifiedTime: Instant
    var contentPublishTime: Instant
    var prevContentPublishTime: Instant
    var refContentPublishTime: Instant
    var prevRefContentPublishTime: Instant
    var prevSignature: ByteBuffer?
    var signature: ByteBuffer?
    var contentScore: Float
    var score: Float
    var sortScore: String?
    var pageCounters: PageCounters
    var headers: Map<String, String>?
    var links: List<String>?
    var liveLinks: Map<String, HypeLink>?
    var vividLinks: Map<String, String>?
    var deadLinks: List<String>?
    var inlinks: Map<String, String>?
    var markers: CrawlMarks
    var metadata: Metadata
    var pageModel: PageModel
}
