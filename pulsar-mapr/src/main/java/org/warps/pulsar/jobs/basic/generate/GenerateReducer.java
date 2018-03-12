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
package org.warps.pulsar.jobs.basic.generate;

import org.slf4j.Logger;
import org.warps.pulsar.common.MetricsCounters;
import org.warps.pulsar.common.StringUtil;
import org.warps.pulsar.common.config.Params;
import org.warps.pulsar.jobs.basic.fetch.FetchMapper;
import org.warps.pulsar.jobs.common.SelectorEntry;
import org.warps.pulsar.jobs.core.GoraReducer;
import org.warps.pulsar.persist.WebPage;
import org.warps.pulsar.persist.gora.generated.GWebPage;
import org.warps.pulsar.persist.metadata.Mark;

import java.io.IOException;

import static org.warps.pulsar.common.PulsarConstants.ALL_BATCHES;
import static org.warps.pulsar.common.UrlUtil.reverseUrl;
import static org.warps.pulsar.common.config.CapabilityTypes.BATCH_ID;
import static org.warps.pulsar.common.config.CapabilityTypes.GENERATE_TOP_N;

/**
 * Reduce class for generate
 * <p>
 * The #reduce() method write a random integer to all generated URLs. This
 * random number is then used by {@link FetchMapper}.
 */
public class GenerateReducer extends GoraReducer<SelectorEntry, GWebPage, String, GWebPage> {

    public static final Logger LOG = GenerateJob.LOG;

    static {
        MetricsCounters.register(Counter.class);
    }

    private int limit;
    private String batchId;
    private int count = 0;

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        batchId = conf.get(BATCH_ID, ALL_BATCHES);

        // Generate top N links only
        limit = conf.getUint(GENERATE_TOP_N, Integer.MAX_VALUE);
        limit /= context.getNumReduceTasks();

        LOG.info(Params.format(
                "className", this.getClass().getSimpleName(),
                "batchId", batchId,
                "limit", limit
        ));
    }

    @Override
    protected void reduce(SelectorEntry key, Iterable<GWebPage> rows, Context context) throws IOException, InterruptedException {

        String url = key.getUrl();

        for (GWebPage row : rows) {
            WebPage page = WebPage.box(url, row);

            try {
                if (count >= limit) {
                    stop("Enough pages generated, quit");
                    break;
                }

                page.setBatchId(batchId);
                page.setGenerateTime(startTime);

                page.getMarks().remove(Mark.INJECT);
                page.getMarks().put(Mark.GENERATE, batchId);

                context.write(reverseUrl(url), page.unbox());

                ++count;
            } catch (Throwable e) {
                LOG.error(StringUtil.stringifyException(e));
            }
        } // for
    }

    private enum Counter {rHosts, rUrlMalformed, rSeeds, rFromSeed}
}
