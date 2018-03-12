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
package org.warps.pulsar.jobs.basic.index;

import com.beust.jcommander.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.warps.pulsar.common.PulsarParams;
import org.warps.pulsar.common.config.ImmutableConfig;
import org.warps.pulsar.common.config.Params;
import org.warps.pulsar.jobs.common.IdentityPageReducer;
import org.warps.pulsar.jobs.core.PulsarJob;
import org.warps.pulsar.persist.gora.generated.GWebPage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.warps.pulsar.common.PulsarParams.*;
import static org.warps.pulsar.common.config.CapabilityTypes.*;

public class IndexJob extends PulsarJob {

    public static final Logger LOG = LoggerFactory.getLogger(IndexJob.class);
    String batchId;
    @Parameter(description = "[batchId] \n If batchId is not specified, last generated batch(es) will be fetched.")
    private List<String> batchIds = new ArrayList<>();
    @Parameter(names = ARG_CRAWL_ID, description = "The id to prefix the schemas to operate on, (default: storage.crawl.id)")
    private String crawlId = null;
    @Parameter(names = ARG_RESUME, description = "Resume interrupted job")
    private boolean resume = false;
    @Parameter(names = ARG_REINDEX, description = "Force index")
    private boolean reindex = false;
    @Parameter(names = ARG_FORCE, description = "Force index")
    private boolean force = false;
    @Parameter(names = {"-help", "-h"}, help = true, description = "Print the help")
    private boolean help = false;

    public IndexJob() {
    }

    public static void main(String[] args) throws Exception {
        final int res = PulsarJob.run(new ImmutableConfig(), new IndexJob(), args);
        System.exit(res);
    }

    @Override
    public void setup(Params params) throws Exception {
        batchId = batchIds.get(0);
        int limit = params.getInt(PulsarParams.ARG_LIMIT, -1);

        conf.set(CRAWL_ID, crawlId);
        conf.set(BATCH_ID, batchId);
        conf.setInt(LIMIT, limit);
        conf.setBoolean(RESUME, resume);
        conf.setBoolean(FORCE, force);

        LOG.info(Params.format(
                "className", this.getClass().getSimpleName(),
                "crawlId", crawlId,
                "batchId", batchId,
                "resume", resume,
                "force", force,
                "reindex", reindex,
                "limit", limit
        ));
    }

    @Override
    public void initJob() throws Exception {
        initMapper(currentJob, Collections.emptyList(), String.class, GWebPage.class, IndexMapper.class, getBatchIdFilter(batchId));
        initReducer(currentJob, IdentityPageReducer.class);
    }
}
