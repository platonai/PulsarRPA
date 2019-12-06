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
package ai.platon.pulsar.jobs.app.fetch;

import ai.platon.pulsar.common.ReducerContext;
import ai.platon.pulsar.common.StringUtil;
import ai.platon.pulsar.crawl.fetch.FetchMonitor;
import ai.platon.pulsar.jobs.common.FetchEntryWritable;
import ai.platon.pulsar.jobs.core.AppContextAwareGoraReducer;
import ai.platon.pulsar.jobs.core.HadoopReducerContext;
import ai.platon.pulsar.jobs.fetch.service.FetchServer;
import ai.platon.pulsar.persist.gora.generated.GWebPage;
import org.apache.hadoop.io.IntWritable;

import java.io.IOException;

public class FetchReducer extends AppContextAwareGoraReducer<IntWritable, FetchEntryWritable, String, GWebPage> {

  private FetchMonitor fetchMonitor;
  private FetchServer fetchServer;

  @Override
  protected void setup(Context context) throws IOException, InterruptedException {
    super.setup(context);
    // TODO: The mapper can change the Configuration will might affect components of reducer
    // Configuration conf = context.getConfiguration(); // the conf can be different from the one loaded by application context

    fetchMonitor = applicationContext.getBean(FetchMonitor.class);
    boolean crowdSourceModeIsEnabled = conf.getBoolean("fetch.crowd.source.mode.is.enabled", false);
    if (crowdSourceModeIsEnabled) {
      fetchServer = applicationContext.getBean("fetchServer", FetchServer.class);
      fetchServer.initialize(applicationContext);
    }
  }

  @Override
  protected void doRun(Context context) {
    if (fetchServer != null) {
      fetchServer.startAsDaemon();
    }
    ReducerContext<IntWritable, FetchEntryWritable, String, GWebPage> rc = new HadoopReducerContext<>(context);
    fetchMonitor.start(rc);
  }

  @Override
  protected void cleanup(Context context) {
    try {
      if (fetchServer != null) {
        fetchServer.shutdownNow();
      }
      fetchMonitor.close();
    }
    catch (Throwable e) {
      LOG.error(StringUtil.stringifyException(e));
    }
  }
}
