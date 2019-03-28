package ai.platon.pulsar.jobs.app.inject;

import ai.platon.pulsar.common.UrlUtil;
import ai.platon.pulsar.common.config.Params;
import ai.platon.pulsar.crawl.component.InjectComponent;
import ai.platon.pulsar.jobs.core.AppContextAwareMapper;
import ai.platon.pulsar.persist.gora.generated.GWebPage;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;

import java.io.IOException;

import static ai.platon.pulsar.common.CommonCounter.mPersist;
import static ai.platon.pulsar.common.CommonCounter.mRows;

/**
 * Created by vincent on 17-4-13.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
public class InjectMapper extends AppContextAwareMapper<LongWritable, Text, String, GWebPage> {
  private InjectComponent injectComponent;

  @Override
  protected void setup(Context context) throws IOException, InterruptedException {
    injectComponent = applicationContext.getBean(InjectComponent.class);

    Params.of("className", this.getClass().getSimpleName())
        .merge(injectComponent.getParams()).withLogger(LOG).info();
  }

  protected void map(LongWritable key, Text line, Context context) throws IOException, InterruptedException {
    metricsCounters.increase(mRows);

    String configuredUrl = StringUtils.stripToEmpty(line.toString());
    if (configuredUrl.isEmpty() || configuredUrl.startsWith("#")) {
      return;
    }

    injectComponent.inject(UrlUtil.splitUrlArgs(configuredUrl));
    metricsCounters.increase(mPersist);
  }
}
