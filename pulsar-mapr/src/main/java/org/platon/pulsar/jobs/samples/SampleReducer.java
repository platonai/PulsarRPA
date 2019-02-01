/*******************************************************************************
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
 ******************************************************************************/
package org.platon.pulsar.jobs.samples;

import ai.platon.pulsar.common.CommonCounter;
import ai.platon.pulsar.common.StringUtil;
import ai.platon.pulsar.common.UrlUtil;
import ai.platon.pulsar.common.config.Params;
import ai.platon.pulsar.persist.WebPage;
import ai.platon.pulsar.persist.gora.generated.GWebPage;
import ai.platon.pulsar.crawl.scoring.NamedScoreVector;
import ai.platon.pulsar.crawl.scoring.ScoreVector;
import org.apache.hadoop.io.Text;
import org.platon.pulsar.jobs.core.GoraReducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;

import static ai.platon.pulsar.common.config.CapabilityTypes.BATCH_ID;
import static ai.platon.pulsar.common.config.CapabilityTypes.CRAWL_ID;

public class SampleReducer extends GoraReducer<Text, GWebPage, String, GWebPage> {

    public static final Logger LOG = LoggerFactory.getLogger(SampleJob.class);

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        String crawlId = conf.get(CRAWL_ID);
        String batchId = conf.get(BATCH_ID);

        Params.of(
                "className", this.getClass().getSimpleName(),
                "crawlId", crawlId,
                "batchId", batchId
        ).withLogger(LOG).info();
    }

    @Override
    protected void reduce(Text key, Iterable<GWebPage> values, Context context) {
        try {
            doReduce(key, values, context);
        } catch (Throwable e) {
            LOG.error(StringUtil.stringifyException(e));
        }
    }

    private void doReduce(Text key, Iterable<GWebPage> values, Context context) throws IOException, InterruptedException {
        metricsCounters.increase(CommonCounter.rRows);

        String reversedUrl = key.toString();
        String url = UrlUtil.unreverseUrl(reversedUrl);

        for (GWebPage row : values) {
            WebPage page = WebPage.box(url, row);

            fixContentScoreBugs(page);

            context.write(reversedUrl, page.unbox());
        }
    }

    private void fixContentScoreBugs(WebPage page) {
        String sortScore = page.getSortScore();
        ScoreVector score = NamedScoreVector.parse(sortScore);
        score.setValue(NamedScoreVector.Name.createTime.ordinal(), 0);
    }

    private void fixDateTimeBugs(WebPage page) {
        if (page.getCreateTime().equals(Instant.EPOCH)) {
            page.setCreateTime(startTime.minusSeconds(3600 * 4));
        }

        if (page.getModifiedTime().isAfter(startTime)) {
            page.setPrevModifiedTime(startTime);
            page.setModifiedTime(startTime);
        }

        if (page.getContentPublishTime().isAfter(startTime)) {
            page.setPrevContentPublishTime(startTime);
            page.setContentPublishTime(startTime);
        }

        if (page.getContentModifiedTime().isAfter(startTime)) {
            page.setPrevContentModifiedTime(startTime);
            page.setContentModifiedTime(startTime);
        }

        if (page.getRefContentPublishTime().isAfter(startTime)) {
            page.setPrevRefContentPublishTime(startTime);
            page.setRefContentPublishTime(startTime);
        }
    }

    private void fixDataBugs(WebPage page) {
        if (page.getDistance() > 1 && page.getDistance() < 10) {
            page.setDistance(1);
        }

        if (page.getScore() > 1000000) {
            page.setScore(0.0f);
        }
        if (page.getCash() > 1000000) {
            page.setCash(0.0f);
        }
    }

}
