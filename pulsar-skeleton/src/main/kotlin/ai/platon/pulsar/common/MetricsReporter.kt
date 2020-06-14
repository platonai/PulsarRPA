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
package ai.platon.pulsar.common

import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

/**
 * TODO: use ScheduledExecutorService, @see [com.codahale.metrics.ScheduledReporter]
 * */
class MetricsReporter(
        private val counter: MetricsCounters,
        private val conf: ImmutableConfig
): Thread() {
    private var log = LoggerFactory.getLogger(MetricsReporter::class.java)
    private val running = AtomicBoolean(false)
    private val silent = AtomicBoolean(false)
    private val reportInterval = conf.getDuration(CapabilityTypes.REPORTER_REPORT_INTERVAL, Duration.ofSeconds(30))
    private val jobName get() = conf.get(CapabilityTypes.PARAM_JOB_NAME, "UNNAMED JOB")
    private var lastStatus = ""

    val isActive get() = running.get()

    init {
        name = "Reporter-" + counter.id
        isDaemon = true
    }

    fun silent() {
        silent.set(true)
    }

    fun outputTo(log: Logger) {
        this.log = log
    }

    fun startReporter() {
        if (running.compareAndSet(false, true)) {
            start()
        }
    }

    fun stopReporter() {
        running.set(false)
        silent.set(false)
        try {
            join()
        } catch (e: InterruptedException) {
            log.error(e.toString())
        }
    }

    override fun run() {
        val outerBorder = StringUtils.repeat('-', 100)
        val innerBorder = StringUtils.repeat('.', 100)
        log.info(outerBorder)
        log.info(innerBorder)
        log.info("== Reporter is started [ " + DateTimes.now() + " ] [ " + jobName + " ] ==")
        counter.registeredCounters.map { readableClassName(it) }
                .joinToString(", ", "All registered counters : ") { it }
                .also { log.info(it) }

        do {
            sleepSeconds(reportInterval.seconds)
            report()
        } while (isActive)

        log.info("== Reporter is stopped [ " + DateTimes.now() + " ] ==")
    }

    private fun report() {
        if (!silent.get()) {
            val status = counter.getStatus(false)
            if (status.isNotEmpty() && status != lastStatus) {
                log.info(status)
                lastStatus = status
            }
        }
    }
}
