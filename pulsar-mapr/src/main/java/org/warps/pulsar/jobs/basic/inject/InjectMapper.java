package org.warps.pulsar.jobs.basic.inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.warps.pulsar.common.UrlUtil;
import org.warps.pulsar.crawl.inject.SeedBuilder;
import org.warps.pulsar.jobs.core.Mapper;
import org.warps.pulsar.persist.WebPage;
import org.warps.pulsar.persist.gora.generated.GWebPage;

import java.io.IOException;

/**
 * Created by vincent on 17-4-13.
 * Copyright @ 2013-2017 Warpspeed Information. All rights reserved
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
