package org.platon.pulsar.jobs.basic.fetch;

import ai.platon.pulsar.common.CounterUtils;
import ai.platon.pulsar.persist.WebPage;
import ai.platon.pulsar.persist.gora.generated.GWebPage;
import org.apache.hadoop.io.IntWritable;
import org.platon.pulsar.jobs.common.FetchEntry;
import org.platon.pulsar.jobs.core.GoraMapper;

import java.io.IOException;
import java.util.Random;

public class FetchMapper extends GoraMapper<String, GWebPage, IntWritable, FetchEntry> {

    private Random random = new Random();

    @Override
    protected void map(String reversedUrl, GWebPage row, Context context) throws IOException, InterruptedException {
        WebPage page = WebPage.box(reversedUrl, row, true);

        // Higher priority comes first
        int shuffleOrder = random.nextInt(10000) - 10000 * page.getFetchPriority();
        context.write(new IntWritable(shuffleOrder), new FetchEntry(conf.unbox(), page.getKey(), page));
        CounterUtils.increaseMDepth(page.getDistance(), metricsCounters);
    }
}
