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
package fun.platonic.pulsar.jobs.basic.update;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import fun.platonic.pulsar.common.config.ImmutableConfig;
import fun.platonic.pulsar.jobs.core.PulsarJob;
import fun.platonic.pulsar.persist.gora.generated.GWebPage;
import fun.platonic.pulsar.persist.graph.GraphGroupKey;
import fun.platonic.pulsar.persist.graph.GraphGroupKey.GraphKeyComparator;
import fun.platonic.pulsar.persist.graph.GraphGroupKey.UrlOnlyComparator;
import fun.platonic.pulsar.persist.graph.GraphGroupKey.UrlOnlyPartitioner;
import fun.platonic.pulsar.persist.io.WebGraphWritable;
import org.apache.gora.filter.MapFieldValueFilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static fun.platonic.pulsar.common.PulsarParams.ARG_CRAWL_ID;

public class OutGraphUpdateJob extends PulsarJob {

    @Parameter(description = "Batch id to fetch")
    List<String> batchId = new ArrayList<>();
    @Parameter(names = ARG_CRAWL_ID, description = "The id to prefix the schemas to operate on, (default: storage.crawl.id)")
    String crawlId = null;
    @Parameter(names = {"-help", "-h"}, help = true, description = "Print the help")
    boolean help = false;

    public OutGraphUpdateJob(ImmutableConfig conf) {
        setConf(conf);
    }

    public static void main(String[] args) throws Exception {
        OutGraphUpdateJob job = new OutGraphUpdateJob(new ImmutableConfig());
        JCommander jc = new JCommander(job, args);
        if (job.help) {
            jc.usage();
            return;
        }
        job.run();
    }

    @Override
    public void initJob() throws Exception {
        // Partition by {url}, sort by {url,score} and group by {url}.
        // This ensures that the inlinks are sorted by score when they enter the reducer.
        currentJob.setPartitionerClass(UrlOnlyPartitioner.class);
        currentJob.setSortComparatorClass(GraphKeyComparator.class);
        currentJob.setGroupingComparatorClass(UrlOnlyComparator.class);

        MapFieldValueFilter<String, GWebPage> batchIdFilter = getBatchIdFilter(batchId.get(0));
        initMapper(currentJob, Collections.emptyList(), GraphGroupKey.class, WebGraphWritable.class, OutGraphUpdateMapper.class, batchIdFilter);
        initReducer(currentJob, OutGraphUpdateReducer.class);
    }
}
