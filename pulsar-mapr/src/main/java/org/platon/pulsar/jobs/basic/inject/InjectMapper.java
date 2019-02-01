package org.platon.pulsar.jobs.basic.inject;

import ai.platon.pulsar.common.UrlUtil;
import ai.platon.pulsar.persist.WebPage;
import ai.platon.pulsar.persist.gora.generated.GWebPage;
import ai.platon.pulsar.crawl.inject.SeedBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.platon.pulsar.jobs.core.Mapper;

import java.io.IOException;

/**
 * Created by vincent on 17-4-13.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
public class InjectMapper extends Mapper<LongWritable, Text, String, GWebPage> {
    private SeedBuilder seedBuiler = new SeedBuilder();

    protected void map(LongWritable key, Text line, Context context) throws IOException, InterruptedException {
        String configuredUrl = StringUtils.stripToEmpty(line.toString());
        if (configuredUrl.isEmpty() || configuredUrl.startsWith("#")) {
            return;
        }

        WebPage page = seedBuiler.create(UrlUtil.splitUrlArgs(configuredUrl));
        if (!page.isNil()) {
            context.write(page.getKey(), page.unbox());
        }
    }
}
