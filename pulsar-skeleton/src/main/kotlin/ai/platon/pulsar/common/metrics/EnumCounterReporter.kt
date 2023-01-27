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
package ai.platon.pulsar.common.metrics

import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.concurrent.GracefulScheduledExecutor
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.readableClassName
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

class EnumCounterReporter(
    private val counter: EnumCounterRegistry,
    initialDelay: Duration = Duration.ofMinutes(3),
    watchInterval: Duration = Duration.ofSeconds(30),
    private val conf: ImmutableConfig
): GracefulScheduledExecutor(initialDelay, watchInterval) {
    private var log = LoggerFactory.getLogger(EnumCounterReporter::class.java)
    private val jobName get() = conf.get(CapabilityTypes.PARAM_JOB_NAME, "UNNAMED JOB")
    private var lastStatus = ""
    private val tick = AtomicInteger()

    fun outputTo(log: Logger) {
        this.log = log
    }

    override fun run() {
        if (!AppContext.isActive) {
            close()
            return
        }

        if (tick.getAndIncrement() == 0) {
            init()
        }

        val status = counter.getStatus(true)
        if (status.isNotEmpty() && status != lastStatus) {
            log.info(status)
            lastStatus = status
        }
    }

    private fun init() {
        log.info("Counter reporter is started [ " + DateTimes.now() + " ] [ " + jobName + " ]")
        counter.registeredCounters.map { readableClassName(it) }
                .joinToString(", ", "All registered counters : ") { it }
                .also { log.info(it) }
    }
}
