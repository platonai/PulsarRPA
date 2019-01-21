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
package fun.platonic.pulsar.jobs.basic.generate;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import fun.platonic.pulsar.common.PulsarFiles;
import fun.platonic.pulsar.common.config.ImmutableConfig;
import fun.platonic.pulsar.common.config.Params;
import fun.platonic.pulsar.jobs.common.JobUtils;
import fun.platonic.pulsar.jobs.common.SelectorEntry;
import fun.platonic.pulsar.jobs.core.PulsarJob;
import fun.platonic.pulsar.persist.gora.generated.GWebPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

import static fun.platonic.pulsar.common.PulsarParams.*;
import static fun.platonic.pulsar.common.config.CapabilityTypes.*;
import static fun.platonic.pulsar.common.config.PulsarConstants.DISTANCE_INFINITE;

public final class GenerateJob extends PulsarJob {

    public static final Logger LOG = LoggerFactory.getLogger(GenerateJob.class);

    @Parameter(names = ARG_CRAWL_ID, description = "The crawl id, (default : \"storage.crawl.id\").")
    private String crawlId = null;
    @Parameter(names = ARG_BATCH_ID, description = "The batch id")
    private String batchId = JobUtils.generateBatchId();
    @Parameter(names = ARG_REGENERATE, description = "Re-generate")
    private boolean reGenerate = false;
    @Parameter(names = ARG_TOPN, description = "Number of top URLs to be selected")
    private int topN = Integer.MAX_VALUE;
    @Parameter(names = ARG_NO_NORMALIZER, description = "Activate the normalizer plugin to normalize the url")
    private boolean noNormalizer = false;
    @Parameter(names = ARG_NO_FILTER, description = "Activate the filter plugin to filter the url")
    private boolean noFilter = false;
    @Parameter(names = {"-help", "-h"}, help = true, description = "print the help information")
    private boolean help;

    public GenerateJob(ImmutableConfig conf) {
        setConf(conf);
    }

    public static void main(String args[]) throws Exception {
        GenerateJob job = new GenerateJob(new ImmutableConfig());
        JCommander jc = new JCommander(job, args);
        if (job.help) {
            jc.usage();
            return;
        }
        job.run();
    }

    @Override
    public void setup(Params params) throws Exception {
        conf.set(CRAWL_ID, crawlId);
        conf.set(BATCH_ID, batchId);
        conf.setBoolean(GENERATE_REGENERATE, reGenerate);
        conf.setInt(GENERATE_TOP_N, topN);
        conf.setBoolean(GENERATE_FILTER, !noFilter);
        conf.setBoolean(GENERATE_NORMALISE, !noNormalizer);

        PulsarFiles.INSTANCE.writeBatchId(batchId);

        LOG.info(Params.format(
                "className", this.getClass().getSimpleName(),
                "round", conf.getInt(CRAWL_ROUND, 0),
                "crawlId", crawlId,
                "batchId", batchId,
                "filter", !noFilter,
                "normaliser", !noNormalizer,
                "topN", topN,
                "maxDistance", conf.getUint(CRAWL_MAX_DISTANCE, DISTANCE_INFINITE),
                "reGenerate", reGenerate
        ));
    }

    @Override
    public void initJob() throws Exception {
        initMapper(currentJob, Collections.emptyList(), SelectorEntry.class, GWebPage.class, GenerateMapper.class);
        initReducer(currentJob, GenerateReducer.class);
    }
}
