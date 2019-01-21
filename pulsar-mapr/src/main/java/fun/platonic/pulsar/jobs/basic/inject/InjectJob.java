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
package fun.platonic.pulsar.jobs.basic.inject;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import fun.platonic.pulsar.common.DateTimeUtil;
import fun.platonic.pulsar.common.config.ImmutableConfig;
import fun.platonic.pulsar.common.config.Params;
import fun.platonic.pulsar.jobs.core.PulsarJob;
import fun.platonic.pulsar.persist.gora.generated.GWebPage;
import org.apache.gora.mapreduce.GoraOutputFormat;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static fun.platonic.pulsar.common.PulsarParams.ARG_CRAWL_ID;

public final class InjectJob extends PulsarJob {

    public static final Logger LOG = LoggerFactory.getLogger(InjectJob.class);

    @Parameter(required = true, description = "<seeds> \nSeed urls. Use {@code @FILE} syntax to read from file.")
    private List<String> seeds = new ArrayList<>();
    @Parameter(names = {ARG_CRAWL_ID}, description = "crawl id, (default : \"storage.crawl.id\").")
    private String crawlId = null;
    @Parameter(names = {"-help", "-h"}, help = true, description = "print the help information")
    private boolean help;

    private File seedFile;

    public InjectJob() {
    }

    public InjectJob(ImmutableConfig conf) {
        setConf(conf);
    }

    public static void main(String[] args) throws Exception {
        InjectJob job = new InjectJob(new ImmutableConfig());
        JCommander jc = new JCommander(job, args);
        if (job.help) {
            jc.usage();
            return;
        }
        job.run();
    }

    @Override
    public void setup(Params params) throws Exception {
        seedFile = File.createTempFile("seed", ".txt");
        Files.write(seedFile.toPath(), seeds.get(0).getBytes());

        LOG.info(Params.format(
                "className", this.getClass().getSimpleName(),
                "crawlId", crawlId,
                "seedFile", seedFile,
                "jobStartTime", DateTimeUtil.format(startTime)
        ));
    }

    @Override
    public void initJob() throws Exception {
        FileInputFormat.addInputPath(currentJob, new Path(seedFile.getAbsolutePath()));
        currentJob.setMapperClass(InjectMapper.class);
        currentJob.setMapOutputKeyClass(String.class);
        currentJob.setMapOutputValueClass(GWebPage.class);
        currentJob.setOutputFormatClass(GoraOutputFormat.class);

        currentJob.setNumReduceTasks(0);
    }
}
