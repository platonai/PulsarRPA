/*******************************************************************************
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
 ******************************************************************************/
package fun.platonic.pulsar.jobs.basic.fetch;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import fun.platonic.pulsar.common.config.ImmutableConfig;
import fun.platonic.pulsar.common.config.Params;
import fun.platonic.pulsar.jobs.common.FetchEntry;
import fun.platonic.pulsar.jobs.common.URLPartitioner;
import fun.platonic.pulsar.jobs.core.PulsarJob;
import org.apache.hadoop.io.IntWritable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static fun.platonic.pulsar.common.PulsarParams.ARG_CRAWL_ID;
import static fun.platonic.pulsar.common.PulsarParams.ARG_RESUME;
import static fun.platonic.pulsar.common.config.CapabilityTypes.*;

/**
 * Fetch job
 */
public final class FetchJob extends PulsarJob {

    public static final Logger LOG = LoggerFactory.getLogger(FetchJob.class);

    @Parameter(description = "[batchId] \n If batchId is not specified, last generated batch(es) will be fetched.")
    private List<String> batchIds = new ArrayList<>();
    @Parameter(names = ARG_CRAWL_ID, description = "The id to prefix the schemas to operate on")
    private String crawlId = null;
    @Parameter(names = ARG_RESUME, description = "Resume interrupted job")
    private boolean resume = false;
    @Parameter(names = {"-help", "-h"}, help = true, description = "Print this help text")
    private boolean help;

    public FetchJob(ImmutableConfig conf) {
        setConf(conf);
    }

    public static void main(String[] args) throws Exception {
        FetchJob job = new FetchJob(new ImmutableConfig());
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
        conf.set(BATCH_ID, batchIds.get(0));
        conf.setBoolean(RESUME, resume);

        LOG.info(Params.format(
                "className", this.getClass().getSimpleName(),
                "crawlId", crawlId,
                "batchId", batchIds.get(0),
                "resume", resume
        ));
    }

    @Override
    public void initJob() throws Exception {
        String batchId = conf.get(BATCH_ID);

        initMapper(currentJob, Collections.emptyList(),
                IntWritable.class, FetchEntry.class, FetchMapper.class,
                URLPartitioner.FetchEntryPartitioner.class, getBatchIdFilter(batchId), false);
        initReducer(currentJob, FetchReducer.class);
    }
}
