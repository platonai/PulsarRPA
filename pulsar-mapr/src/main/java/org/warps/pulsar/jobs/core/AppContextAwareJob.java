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
package org.warps.pulsar.jobs.core;

import org.apache.hadoop.util.GenericOptionsParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.warps.pulsar.common.config.ImmutableConfig;
import org.warps.pulsar.common.config.MutableConfig;
import org.warps.pulsar.persist.gora.db.WebDb;

import static org.warps.pulsar.common.PulsarConstants.JOB_CONTEXT_CONFIG_LOCATION;
import static org.warps.pulsar.common.config.CapabilityTypes.APPLICATION_CONTEXT_CONFIG_LOCATION;

public abstract class AppContextAwareJob extends PulsarJob {

    public static final Logger LOG = LoggerFactory.getLogger(AppContextAwareJob.class);

    protected String contextConfigLocation;
    protected ConfigurableApplicationContext applicationContext;

    public static int run(AppContextAwareJob job, String[] args) throws Exception {
        return run(JOB_CONTEXT_CONFIG_LOCATION, job, args);
    }

    public static int run(String contextConfigLocation, AppContextAwareJob job, String[] args) throws Exception {
        ConfigurableApplicationContext applicationContext = new ClassPathXmlApplicationContext(contextConfigLocation);
        applicationContext.registerShutdownHook();
        job.setContextConfigLocation(contextConfigLocation);
        return run(applicationContext, job, args);
    }

    public static int run(ConfigurableApplicationContext applicationContext, AppContextAwareJob job, String[] args) throws Exception {
        MutableConfig conf = new MutableConfig(applicationContext.getBean(ImmutableConfig.class));
        conf.set(APPLICATION_CONTEXT_CONFIG_LOCATION, job.getContextConfigLocation());

        job.setApplicationContext(applicationContext);
        job.setConf(conf);
        job.setWebDb(applicationContext.getBean(WebDb.class));

        // Strip hadoop reserved args
        GenericOptionsParser parser = new GenericOptionsParser(conf.unbox(), args);
        String[] jobArgs = parser.getRemainingArgs();
        return job.run(jobArgs);
    }

    public String getContextConfigLocation() {
        return contextConfigLocation;
    }

    public void setContextConfigLocation(String contextConfigLocation) {
        this.contextConfigLocation = contextConfigLocation;
    }

    public ConfigurableApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public void setApplicationContext(ConfigurableApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * Destroy all beans before PulsarJob do the cleanup
     */
    @Override
    protected void cleanupContext() {
        applicationContext.close();
    }
}
