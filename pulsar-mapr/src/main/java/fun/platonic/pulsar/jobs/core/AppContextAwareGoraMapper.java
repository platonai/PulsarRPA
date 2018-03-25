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
package fun.platonic.pulsar.jobs.core;

import fun.platonic.pulsar.jobs.common.ConfigurableAppContextAware;
import org.apache.gora.persistency.Persistent;
import org.springframework.beans.BeansException;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;

public class AppContextAwareGoraMapper<K1, V1 extends Persistent, K2, V2>
        extends GoraMapper<K1, V1, K2, V2> implements ConfigurableAppContextAware {

    protected ConfigurableApplicationContext applicationContext;

    @Override
    protected void beforeSetup(Context context) throws IOException, InterruptedException {
        super.beforeSetup(context);
        initApplicationContext(context.getConfiguration());
    }

    @Override
    public ConfigurableApplicationContext getApplicationContext() {
        return applicationContext;
    }

    @Override
    public void setApplicationContext(ConfigurableApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * Destroy all beans before PulsarJob do the cleanup
     */
    @Override
    protected void cleanupContext(Context context) {
        applicationContext.close();
    }

    public Object getBean(String name) throws BeansException {
        return applicationContext.getBean(name);
    }

    public <T> T getBean(Class<T> requiredType) throws BeansException {
        return applicationContext.getBean(requiredType);
    }

}
