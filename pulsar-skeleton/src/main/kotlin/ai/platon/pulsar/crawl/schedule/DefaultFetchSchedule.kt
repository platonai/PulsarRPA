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
package ai.platon.pulsar.crawl.schedule

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.message.MiscMessageWriter
import ai.platon.pulsar.common.persist.ext.options
import ai.platon.pulsar.crawl.component.FetchComponent
import ai.platon.pulsar.persist.WebPage
import java.time.Duration
import java.time.Instant

/**
 * This class implements the default re-fetch schedule. That is, no matter if
 * the page was changed or not, the `fetchInterval` remains
 * unchanged, and the updated page fetchTime will always be set to
 * `fetchTime + fetchInterval * 1000`.
 *
 * @author Andrzej Bialecki
 */
class DefaultFetchSchedule(
        conf: ImmutableConfig,
        messageWriter: MiscMessageWriter? = null
) : AbstractFetchSchedule(conf, messageWriter) {

}
