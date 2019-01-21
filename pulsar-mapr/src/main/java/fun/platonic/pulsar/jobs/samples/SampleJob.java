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

import fun.platonic.pulsar.common.config.CapabilityTypes;
import fun.platonic.pulsar.common.config.ImmutableConfig;
import fun.platonic.pulsar.common.config.MutableConfig;
import fun.platonic.pulsar.common.config.Params;
import fun.platonic.pulsar.jobs.core.PulsarJob;
import fun.platonic.pulsar.persist.gora.generated.GWebPage;
import org.apache.hadoop.io.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static fun.platonic.pulsar.common.PulsarParams.*;
import static fun.platonic.pulsar.common.config.CapabilityTypes.CRAWL_ID;
import static fun.platonic.pulsar.common.config.PulsarConstants.ALL_BATCHES;

public class SampleJob extends PulsarJob {

    public static final Logger LOG = LoggerFactory.getLogger(SampleJob.class);

    private static final Set<GWebPage.Field> FIELDS = new HashSet<>();

    static {
        Collections.addAll(FIELDS, GWebPage.Field.values());
    }

    public SampleJob() {
    }

    public SampleJob(ImmutableConfig conf) {
        setConf(conf);
    }

    public static void main(String[] args) throws Exception {
        int res = PulsarJob.run(new ImmutableConfig(), new SampleJob(), args);
        System.exit(res);
    }

    @Override
    public void setup(Params params) throws Exception {
        MutableConfig conf = getConf();

        String crawlId = params.get(ARG_CRAWL_ID, conf.get(CRAWL_ID));
        String batchId = params.get(ARG_BATCH_ID, ALL_BATCHES);
        int limit = params.getInt(ARG_LIMIT, -1);

        conf.set(CRAWL_ID, crawlId);
        conf.set(CapabilityTypes.BATCH_ID, batchId);
        conf.setInt(CapabilityTypes.LIMIT, limit);

        LOG.info(Params.format(
                "className", this.getClass().getSimpleName(),
                "crawlId", crawlId,
                "batchId", batchId,
                "limit", limit
        ));
    }

    @Override
    public void initJob() throws Exception {
        initMapper(currentJob, FIELDS, Text.class, GWebPage.class, SampleMapper.class, getInactiveFilter());
        initReducer(currentJob, SampleReducer.class);
    }

    @Override
    public int run(String[] args) {
        String crawlId = null;
        String batchId = null;
        int limit = -1;

        for (int i = 0; i < args.length; i++) {
            if ("-crawlId".equals(args[i])) {
                crawlId = args[++i];
            } else if ("-batchId".equals(args[i])) {
                batchId = args[++i].toUpperCase();
            } else if ("-limit".equals(args[i])) {
                limit = Integer.parseInt(args[++i]);
            } else {
                throw new IllegalArgumentException("arg " + args[i] + " not recognized");
            }
        }

        run(Params.of(ARG_CRAWL_ID, crawlId, ARG_BATCH_ID, batchId, ARG_LIMIT, limit));

        return 0;
    }
}
