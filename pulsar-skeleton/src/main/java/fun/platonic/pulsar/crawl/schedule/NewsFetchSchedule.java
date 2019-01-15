/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fun.platonic.pulsar.crawl.schedule;

import org.slf4j.Logger;
import fun.platonic.pulsar.persist.WebPage;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static fun.platonic.pulsar.common.config.PulsarConstants.TCP_IP_STANDARDIZED_TIME;
import static fun.platonic.pulsar.common.config.PulsarConstants.YES_STRING;
import static fun.platonic.pulsar.persist.metadata.Mark.INACTIVE;

/**
 * This class implements an adaptive re-fetch algorithm.
 * <p>
 * NOTE: values of DEC_FACTOR and INC_FACTOR higher than 0.4f may destabilize
 * the algorithm, so that the fetch interval either increases or decreases
 * infinitely, with little relevance to the page changes. Please use
 * </p>
 *
 * @author Vincent Zhang
 */
public class NewsFetchSchedule extends AdaptiveFetchSchedule {
    public static final Logger LOG = AbstractFetchSchedule.LOG;

    @Override
    public void setFetchSchedule(WebPage page,
                                 Instant prevFetchTime, Instant prevModifiedTime,
                                 Instant fetchTime, Instant modifiedTime, int state) {
        if (modifiedTime.isBefore(TCP_IP_STANDARDIZED_TIME)) {
            modifiedTime = fetchTime;
        }

        Duration interval = Duration.ofDays(365 * 10);
        if (page.isSeed()) {
            interval = adjustSeedFetchInterval(page, fetchTime, modifiedTime);
        } else {
            page.getMarks().put(INACTIVE, YES_STRING);
        }

        updateRefetchTime(page, interval, fetchTime, prevModifiedTime, modifiedTime);
    }

    private Duration adjustSeedFetchInterval(WebPage page, Instant fetchTime, Instant modifiedTime) {
        Instant publishTime = page.getContentPublishTime();
        if (publishTime.isAfter(modifiedTime)) {
            modifiedTime = publishTime;
        }
        long days = ChronoUnit.DAYS.between(modifiedTime, fetchTime);

        if (days > 7) {
            metricsSystem.reportFetchSchedule(page, false);
            return Duration.ofHours(1);
        }

        return MIN_INTERVAL;
    }
}
