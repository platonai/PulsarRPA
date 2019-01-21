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
package fun.platonic.pulsar.jobs.basic.parse;

import com.beust.jcommander.Parameter;
import fun.platonic.pulsar.common.PulsarParams;
import fun.platonic.pulsar.common.config.ImmutableConfig;
import fun.platonic.pulsar.common.config.MutableConfig;
import fun.platonic.pulsar.common.config.Params;
import fun.platonic.pulsar.jobs.common.IdentityPageReducer;
import fun.platonic.pulsar.jobs.core.PulsarJob;
import fun.platonic.pulsar.persist.gora.generated.GWebPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static fun.platonic.pulsar.common.PulsarParams.*;
import static fun.platonic.pulsar.common.config.CapabilityTypes.*;

public class ParserJob extends PulsarJob {

    public static final Logger LOG = LoggerFactory.getLogger(ParserJob.class);

    @Parameter(description = "[batchId] \n If batchId is not specified, last generated batch(es) will be fetched.")
    private List<String> batchIds = new ArrayList<>();
    @Parameter(names = ARG_CRAWL_ID, description = "The id to prefix the schemas to operate on, (default: storage.crawl.id)")
    private String crawlId = null;
    @Parameter(names = ARG_RESUME, description = "Resume interrupted job")
    private boolean resume = false;
    @Parameter(names = ARG_REPARSE, description = "Reparse")
    private boolean reparse = false;
    @Parameter(names = ARG_FORCE, description = "Force index")
    private boolean force = false;
    @Parameter(names = {"-help", "-h"}, help = true, description = "Print the help")
    private boolean help = false;

    private String batchId;

    public ParserJob() {
    }

    public ParserJob(ImmutableConfig conf) {
        setConf(conf);
    }

    public static void main(String[] args) throws Exception {
        final int res = PulsarJob.run(new ImmutableConfig(), new ParserJob(), args);
        System.exit(res);
    }

    @Override
    public void setup(Params params) throws Exception {
        conf.set(CRAWL_ID, crawlId);
        conf.set(BATCH_ID, batchId);
        conf.setBoolean(RESUME, resume);
        conf.setBoolean(FORCE, force);
        conf.setBoolean(PARSE_REPARSE, reparse);

        LOG.info(Params.format(
                "className", this.getClass().getSimpleName(),
                "crawlId", crawlId,
                "batchId", batchId,
                "resume", resume,
                "force", force,
                "reparse", reparse
        ));
    }

    @Override
    public void initJob() throws Exception {
        initMapper(currentJob, Collections.emptyList(), String.class, GWebPage.class, ParserMapper.class, getBatchIdFilter(batchId));
        initReducer(currentJob, IdentityPageReducer.class);
        currentJob.setNumReduceTasks(0);
    }

    private void printUsage() {
        System.err.println("Usage: ParserJob (<batchId> | -reparse) [-crawlId <id>] [-resume] [-force]");
        System.err.println("    <batchId>     - symbolic batch ID created by Generator");
        System.err.println("    -reparse      - reparse pages from all crawl jobs");
        System.err.println("    -crawlId <id> - the id to prefix the schemas to operate on, \n \t \t    (default: storage.crawl.id)");
        System.err.println("    -limit        - limit");
        System.err.println("    -resume       - resume a previous incomplete job");
        System.err.println("    -force        - force re-parsing even if a page is already parsed");
    }

    public int run(String[] args) throws Exception {
        if (args.length < 1) {
            printUsage();
            return -1;
        }

        MutableConfig conf = getConf();

        String batchId = args[0];
        if (batchId.startsWith("-")) {
            printUsage();
            return -1;
        }

        String crawlId = conf.get(CRAWL_ID, "");

        int limit = -1;
        boolean resume = false;
        boolean force = false;

        for (int i = 0; i < args.length; i++) {
            if ("-crawlId".equals(args[i])) {
                crawlId = args[++i];
                // getConf().set(CRAWL_ID, args[++i]);
            } else if ("-limit".equals(args[i])) {
                limit = Integer.parseInt(args[++i]);
            } else if ("-resume".equals(args[i])) {
                resume = true;
            } else if ("-force".equals(args[i])) {
                force = true;
            }
        }

        parse(crawlId, batchId, limit, resume, force);

        return 0;
    }

    public void parse(String crawlId, String batchId, int limit, boolean resume, boolean force) {
        run(Params.of(
                PulsarParams.ARG_CRAWL_ID, crawlId,
                PulsarParams.ARG_BATCH_ID, batchId,
                PulsarParams.ARG_RESUME, resume,
                PulsarParams.ARG_FORCE, force,
                PulsarParams.ARG_LIMIT, limit > 0 ? limit : null
        ));
    }
}
