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
package fun.platonic.pulsar.jobs.samples;

import fun.platonic.pulsar.common.config.ImmutableConfig;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.chain.ChainMapper;
import org.apache.hadoop.mapreduce.lib.chain.ChainReducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SampleChainedJob extends Configured implements Tool {

    public static final Logger LOG = LoggerFactory.getLogger(SampleChainedJob.class);
    static Configuration cf;

    public SampleChainedJob() {
    }

    public SampleChainedJob(Configuration conf) {
        setConf(conf);
    }

    public static void main(String args[]) throws Exception {
        int res = ToolRunner.run(new ImmutableConfig().unbox(), new SampleChainedJob(), args);
        System.exit(res);
    }

    public int run(String args[]) throws IOException, InterruptedException, ClassNotFoundException {
        cf = new Configuration();

        Job job = Job.getInstance(getConf());
        job.setJobName("chain");
        job.setJarByClass(SampleChainedJob.class);

        job.setInputFormatClass(TextInputFormat.class);

        ChainMapper.addMapper(job, UpperCaseMapper.class, Object.class, Text.class, Text.class, Text.class, job.getConfiguration());
        ChainMapper.addMapper(job, SplitMapper.class, Text.class, Text.class, Text.class, IntWritable.class, job.getConfiguration());

        ChainReducer.setReducer(job, SumReducer.class, Text.class, IntWritable.class, Text.class, Text.class, job.getConfiguration());
        ChainReducer.addMapper(job, LowerCaseMapper.class, Text.class, Text.class, Text.class, Text.class, job.getConfiguration());

        //now proceeding with the normal delivery
        // conf.setCombinerClass(ChainMapReducer.class);
        // job.setReducerClass(SumReducer.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        job.setOutputFormatClass(TextOutputFormat.class);

        Path input = new Path("/home/vincent/workspace/warps-pulsar/LICENSE.txt");
        Path output = new Path("/tmp/pulsar-vincent/output/tmp");

        FileInputFormat.addInputPath(job, input);
        FileOutputFormat.setOutputPath(job, output);

        job.waitForCompletion(true);

        return 0;
    }

    //SPLIT MAPPER
    static class SplitMapper extends Mapper<Text, Text, Text, IntWritable> {
        private IntWritable dummyValue = new IntWritable(1);
        //private String content;
        private String tokens[];

        @Override
        protected void map(Text key, Text value, Context context) throws IOException, InterruptedException {
            tokens = value.toString().split(" ");
            for (String x : tokens) {
                context.write(new Text(x), dummyValue);
            }
        }
    }

    //UPPER CASE MAPPER
    static class UpperCaseMapper extends Mapper<Object, Text, Text, Text> {

        @Override
        protected void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            context.write(new Text(key.toString()), new Text(value.toString().toUpperCase()));
        }
    }

    //ChainMapReducer
    static class SumReducer extends Reducer<Text, IntWritable, Text, Text> {
        private int sum = 0;

        protected void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
            for (IntWritable value : values) {
                sum += value.get();
            }
            context.write(key, new Text(String.valueOf(key.toString() + "-" + sum)));
        }
    }

    //UPPER CASE MAPPER
    static class LowerCaseMapper extends Mapper<Text, Text, Text, Text> {
        @Override
        protected void map(Text key, Text value, Context context) throws IOException, InterruptedException {
            context.write(new Text(key.toString().toLowerCase()), new Text(value.toString().toLowerCase()));
        }
    }
}
