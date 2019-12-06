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
package ai.platon.pulsar.jobs.core;

import ai.platon.pulsar.common.DateTimeUtil;
import ai.platon.pulsar.common.PulsarJobBase;
import ai.platon.pulsar.common.StringUtil;
import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.common.config.MutableConfig;
import ai.platon.pulsar.common.config.Params;
import ai.platon.pulsar.jobs.common.JobUtils;
import ai.platon.pulsar.persist.WebDb;
import ai.platon.pulsar.persist.WebPage;
import ai.platon.pulsar.persist.gora.GoraStorage;
import ai.platon.pulsar.persist.gora.generated.GWebPage;
import ai.platon.pulsar.persist.metadata.Mark;
import com.google.common.collect.Maps;
import org.apache.gora.filter.Filter;
import org.apache.gora.filter.FilterOp;
import org.apache.gora.filter.MapFieldValueFilter;
import org.apache.gora.mapreduce.GoraOutputFormat;
import org.apache.gora.query.Query;
import org.apache.gora.store.DataStore;
import org.apache.gora.util.GoraException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import static ai.platon.pulsar.common.config.CapabilityTypes.*;
import static ai.platon.pulsar.common.config.PulsarConstants.ALL_BATCHES;
import static ai.platon.pulsar.common.config.PulsarConstants.YES_STRING;

public abstract class PulsarJob implements PulsarJobBase {

    public static final Logger LOG = LoggerFactory.getLogger(PulsarJob.class);
    protected final Map<String, Object> status = Collections.synchronizedMap(Maps.newLinkedHashMap());
    protected final Map<String, Object> results = Collections.synchronizedMap(Maps.newLinkedHashMap());
    protected MutableConfig conf;
    protected long startTime = System.currentTimeMillis();
    protected Job currentJob;
    protected WebDb webDb;

    protected int numJobs = 1;
    protected int currentJobNum = 0;

    public static int run(ImmutableConfig conf, PulsarJob job, String[] args) throws Exception {
        job.setConf(conf);

        // Strip hadoop reserved args
        GenericOptionsParser parser = new GenericOptionsParser(conf.unbox(), args);
        String[] jobArgs = parser.getRemainingArgs();
        return job.run(jobArgs);
    }

    public static <K, V> void initMapper(Job job,
                                         Collection<GWebPage.Field> fields, Class<K> outKeyClass,
                                         Class<V> outValueClass,
                                         Class<? extends GoraMapper<String, GWebPage, K, V>> mapperClass)
            throws ClassNotFoundException, IOException {
        initMapper(job, fields, outKeyClass, outValueClass, mapperClass, null, true);
    }

    public static <K, V> void initMapper(Job job,
                                         Collection<GWebPage.Field> fields, Class<K> outKeyClass,
                                         Class<V> outValueClass,
                                         Class<? extends GoraMapper<String, GWebPage, K, V>> mapperClass,
                                         Class<? extends Partitioner<K, V>> partitionerClass)
            throws ClassNotFoundException, IOException {
        initMapper(job, fields, outKeyClass, outValueClass, mapperClass, partitionerClass, true);
    }

    public static <K, V> void initMapper(Job job,
                                         Collection<GWebPage.Field> fields, Class<K> outKeyClass,
                                         Class<V> outValueClass,
                                         Class<? extends GoraMapper<String, GWebPage, K, V>> mapperClass,
                                         Class<? extends Partitioner<K, V>> partitionerClass, boolean reuseObjects)
            throws ClassNotFoundException, IOException {
        initMapper(job, fields, outKeyClass, outValueClass, mapperClass, partitionerClass, null, reuseObjects);
    }

    public static <K, V> void initMapper(Job job,
                                         Collection<GWebPage.Field> fields,
                                         Class<K> outKeyClass,
                                         Class<V> outValueClass,
                                         Class<? extends GoraMapper<String, GWebPage, K, V>> mapperClass,
                                         Class<? extends Partitioner<K, V>> partitionerClass,
                                         Filter<String, GWebPage> filter, boolean reuseObjects)
            throws ClassNotFoundException, IOException {
        DataStore<String, GWebPage> store = GoraStorage.createDataStore(job.getConfiguration(), String.class, GWebPage.class);
        if (store == null) {
            throw new RuntimeException("Could not create datastore");
        }

        Query<String, GWebPage> query = store.newQuery();
        query.setFields(toStringArray(fields));
        if (filter != null) {
            query.setFilter(filter);
        }

        GoraMapper.initMapperJob(job, query, outKeyClass, outValueClass, mapperClass, partitionerClass, reuseObjects);
        GoraOutputFormat.setOutput(job, store, true);
    }

    public static <K, V> void initMapper(Job job,
                                         Collection<GWebPage.Field> fields, Class<K> outKeyClass,
                                         Class<V> outValueClass,
                                         Class<? extends GoraMapper<String, GWebPage, K, V>> mapperClass,
                                         Filter<String, GWebPage> filter) throws ClassNotFoundException, IOException {
        initMapper(job, fields, outKeyClass, outValueClass, mapperClass, null, filter, true);
    }

    public static <K, V> void initReducer(Job job, Class<? extends GoraReducer<K, V, String, GWebPage>> reducerClass)
            throws ClassNotFoundException, GoraException {
        Configuration conf = job.getConfiguration();
        DataStore<String, GWebPage> store = GoraStorage.createDataStore(conf, String.class, GWebPage.class);
        GoraReducer.initReducerJob(job, store, reducerClass);
        GoraOutputFormat.setOutput(job, store, true);
    }

    public static String[] toStringArray(Collection<GWebPage.Field> fields) {
        String[] arr = new String[fields.size()];
        Iterator<GWebPage.Field> iter = fields.iterator();
        for (int i = 0; i < arr.length; i++) {
            arr[i] = iter.next().getName();
        }
        return arr;
    }

    public WebDb getWebDb() {
        return webDb;
    }

    public void setWebDb(WebDb webDb) {
        this.webDb = webDb;
    }

    protected void beforeSetup() throws Exception {
        String jobDescription = getJobName() + " - " + conf.get(PULSAR_CONFIG_ID, "");
        LOG.info("\n\n\n\n------------------------- " + jobDescription + " -------------------------");
        LOG.info("Job started at " + DateTimeUtil.format(startTime));
        conf.set(PARAM_JOB_NAME, getJobName());

        status.put("startTime", DateTimeUtil.format(startTime));
    }

    protected void setup(Params params) throws Exception {
    }

    protected void beforeInitJob() throws Exception {
        currentJob = Job.getInstance(conf.unbox(), getJobName());
        currentJob.setJarByClass(this.getClass());

        if (webDb == null) {
            webDb = new WebDb(conf);
        }

        // It seems failed to exit if hbase is lost
        Runtime.getRuntime().addShutdownHook(new Thread(this::killJob));

        LOG.info(Params.format(
                "className", this.getClass().getSimpleName(),
                "workingDir", currentJob.getWorkingDirectory(),
                "jobName", currentJob.getJobName(),
                "realSchema", webDb.getSchemaName(),
                "storeClass", webDb.getStore().getClass().getName()
        ));
    }

    protected abstract void initJob() throws Exception;

    protected void afterInitJob() throws Exception {
    }

    protected void launchJob() throws Exception {
        currentJob.waitForCompletion(true);
    }

    protected void cleanup() throws Exception {
    }

    protected void cleanupContext() throws Exception {
    }

    protected void afterCleanup() {
        try {
            updateStatus();
            updateResults();

            Params.of(status).withLogger(LOG).info();
            Params.of(results).withLogger(LOG).info();

        } catch (Throwable e) {
            LOG.error(StringUtils.stringifyException(e));
        }
    }

    public int run() throws Exception {
        run(Params.EMPTY_PARAMS);
        return 0;
    }

    public int run(String[] args) throws Exception {
        return run();
    }

    /**
     * Runs the job, may return results, or null
     */
    @Override
    public Map<String, Object> run(Params params) {
        try {
            beforeSetup();
            setup(params);

            beforeInitJob();
            initJob();
            afterInitJob();

            // Launch the job
            launchJob();

            cleanup();

            cleanupContext();
        } catch (Throwable e) {
            LOG.error(StringUtil.stringifyException(e));
        } finally {
            afterCleanup();
        }

        return results;
    }

    public String getJobName() {
        if (currentJob == null) {
            String readableTime = new SimpleDateFormat("MMdd.HHmmss").format(startTime);
            return getClass().getSimpleName() + "-" + readableTime;
        } else {
            return currentJob.getJobName();
        }
    }

    public MutableConfig getConf() {
        return conf;
    }

    public void setConf(ImmutableConfig conf) {
        this.conf = new MutableConfig(conf);
    }

    public void setConf(MutableConfig conf) {
        this.conf = conf;
    }

    public MapFieldValueFilter<String, GWebPage> getBatchIdFilter(String batchId) {
        if (batchId == null || batchId.equals(ALL_BATCHES)) {
            return null;
        }

        MapFieldValueFilter<String, GWebPage> filter = new MapFieldValueFilter<>();
        filter.setFieldName(GWebPage.Field.MARKERS.toString());
        filter.setFilterOp(FilterOp.EQUALS);
        filter.setFilterIfMissing(true);
        filter.setMapKey(WebPage.wrapKey(Mark.GENERATE));
        filter.getOperands().add(WebPage.u8((batchId)));

        return filter;
    }

    public MapFieldValueFilter<String, GWebPage> getInactiveFilter() {
        MapFieldValueFilter<String, GWebPage> filter = new MapFieldValueFilter<>();

        filter.setFieldName(GWebPage.Field.MARKERS.toString());
        filter.setFilterOp(FilterOp.NOT_EQUALS);
        filter.setFilterIfMissing(false);
        filter.setMapKey(WebPage.wrapKey(Mark.INACTIVE));
        filter.getOperands().add(WebPage.u8((YES_STRING)));

        return filter;
    }

    /**
     * Returns relative progress of the tool, a float in range [0,1].
     */
    public float getProgress() {
        if (currentJob == null) {
            return 0.0f;
        }

        float res = 0;
        try {
            res = (currentJob.mapProgress() + currentJob.reduceProgress()) / 2.0f;
        } catch (IOException | IllegalStateException e) {
            LOG.warn(e.toString());
            res = 0;
        }

        // take into account multiple jobs
        if (numJobs > 1) {
            res = (currentJobNum + res) / (float) numJobs;
        }

        return res;
    }

    /**
     * Returns current status of the running tool.
     */
    public Map<String, Object> getResults() {
        return results;
    }

    /**
     * Returns current status of the running tool.
     */
    @Override
    public Map<String, Object> getStatus() {
        return status;
    }

    /**
     * Return -1 if the counter is not found
     */
    public long getPulsarStatus(String name) {
        return JobUtils.getJobCounter(currentJob, STAT_PULSAR_STATUS, name);
    }

    public void updateStatus() {
        if (currentJob == null) {
            return;
        }

        try {
            if (currentJob.getStatus() == null || currentJob.isRetired()) {
                return;
            }

            status.putAll(JobUtils.getJobStatus(currentJob));
        } catch (Throwable e) {
            LOG.warn(e.toString());
        }
    }

    public void updateResults() {
        String finishTime = DateTimeUtil.format(System.currentTimeMillis());

        results.putAll(Params.toArgMap(
                "startTime", DateTimeUtil.format(startTime),
                "finishTime", finishTime,
                "timeElapsed", DateTimeUtil.elapsedTime(startTime)
        ));
    }

    public Job getJob() {
        return currentJob;
    }

    /**
     * Stop the job with the possibility to resume. Subclasses should override
     * this, since by default it calls {@link #killJob()}.
     *
     * @return true if succeeded, false otherwise
     */
    public boolean stopJob() throws Exception {
        return killJob();
    }

    /**
     * Kill the job immediately. Clients should assume that any results that the
     * job produced so far are in inconsistent state or missing.
     *
     * @return true if succeeded, false otherwise.
     */
    public boolean killJob() {
        try {
            if (currentJob != null && !currentJob.isComplete()) {
                LOG.info("Killing job " + currentJob.getJobName());
                currentJob.killJob();
            }

            return true;
        } catch (Exception e) {
            LOG.error(e.toString());
        }

        return false;
    }
}
