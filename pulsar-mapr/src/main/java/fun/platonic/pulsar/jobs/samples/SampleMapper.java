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
package fun.platonic.pulsar.jobs.samples;

import fun.platonic.pulsar.common.CommonCounter;
import fun.platonic.pulsar.common.config.Params;
import fun.platonic.pulsar.jobs.core.GoraMapper;
import fun.platonic.pulsar.persist.WebPage;
import fun.platonic.pulsar.persist.gora.generated.GWebPage;
import org.apache.hadoop.io.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static fun.platonic.pulsar.common.config.CapabilityTypes.*;

public class SampleMapper extends GoraMapper<String, GWebPage, Text, GWebPage> {

    public static final Logger LOG = LoggerFactory.getLogger(SampleJob.class);

    private int limit = -1;
    private int count = 0;

    @Override
    public void setup(Context context) throws IOException, InterruptedException {
        String crawlId = conf.get(CRAWL_ID);
        String batchId = conf.get(BATCH_ID);
        limit = conf.getInt(LIMIT, -1);

        Params.of(
                "className", this.getClass().getSimpleName(),
                "crawlId", crawlId,
                "batchId", batchId,
                "limit", limit
        ).withLogger(LOG).info();
    }

    /**
     * One row map to several rows
     */
    @Override
    public void map(String reversedUrl, GWebPage row, Context context) throws IOException, InterruptedException {
        metricsCounters.increase(CommonCounter.mRows);

        if (limit > 0 && count++ > limit) {
            stop("Hit limit, stop");
            return;
        }

        WebPage page = WebPage.box(reversedUrl, row, true);
//    log.debug("Map : " + page.url());

//    if (!page.hasMark(Mark.FETCH)) {
//      metricsCounters.increase(Counter.notFetched);
//      return;
//    }

//    boolean shouldProcess = false;
//    long days = Duration.between(page.getCreateTime(), Instant.now()).toDays();
//    if (days < 2 && page.getDistance() > 100) {
//      shouldProcess = true;
//    }

//    if (!shouldProcess) {
//      return;
//    }

        context.write(new Text(reversedUrl), page.unbox());
    }
}
