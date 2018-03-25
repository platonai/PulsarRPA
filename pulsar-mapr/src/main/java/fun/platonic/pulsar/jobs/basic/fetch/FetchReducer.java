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
package fun.platonic.pulsar.jobs.basic.fetch;

import fun.platonic.pulsar.common.CommonCounter;
import fun.platonic.pulsar.common.StringUtil;
import fun.platonic.pulsar.common.UrlUtil;
import fun.platonic.pulsar.crawl.component.FetchComponent;
import fun.platonic.pulsar.crawl.component.IndexComponent;
import fun.platonic.pulsar.crawl.component.ParseComponent;
import fun.platonic.pulsar.crawl.fetch.TaskStatusTracker;
import fun.platonic.pulsar.crawl.index.IndexDocument;
import fun.platonic.pulsar.crawl.parse.ParseResult;
import fun.platonic.pulsar.crawl.protocol.ProtocolFactory;
import fun.platonic.pulsar.jobs.common.FetchEntry;
import org.apache.hadoop.io.IntWritable;
import fun.platonic.pulsar.jobs.core.GoraReducer;
import fun.platonic.pulsar.persist.CrawlMarks;
import fun.platonic.pulsar.persist.WebPage;
import fun.platonic.pulsar.persist.gora.db.WebDb;
import fun.platonic.pulsar.persist.gora.generated.GWebPage;
import fun.platonic.pulsar.persist.metadata.Mark;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static fun.platonic.pulsar.common.config.CapabilityTypes.INDEX_JIT;
import static fun.platonic.pulsar.common.config.CapabilityTypes.PARSE_PARSE;

public class FetchReducer extends GoraReducer<IntWritable, FetchEntry, String, GWebPage> {

    private WebDb webDb;
    private TaskStatusTracker statusTracker;
    private ProtocolFactory protocolFactory;
    private FetchComponent fetchComponent;
    private ParseComponent parseComponent;
    private IndexComponent indexComponent;
    private ExecutorService executorService;
    private boolean parse = false;
    private boolean index = false;
    private int maxThreads = 5;

    @Override
    public void setup(Context context) {
        // TODO : Components are not initialized properly
        protocolFactory = new ProtocolFactory(conf);
        fetchComponent = new FetchComponent(protocolFactory, statusTracker, conf);
        parseComponent = new ParseComponent(conf);
        indexComponent = new IndexComponent(conf);

        executorService = Executors.newWorkStealingPool(maxThreads);

        parse = conf.getBoolean(PARSE_PARSE, parse);
        index = conf.getBoolean(INDEX_JIT, index);
    }

    @Override
    protected void reduce(IntWritable key, Iterable<FetchEntry> entries, Context context) throws IOException, InterruptedException {
        List<Callable<Integer>> tasks = new LinkedList<>();
        for (FetchEntry entry : entries) {
            String url = UrlUtil.unreverseUrl(entry.getReservedUrl());
            tasks.add(() -> fetch(url));
            if (tasks.size() >= maxThreads) {
                executorService.invokeAll(tasks);
                executorService.awaitTermination(60 * maxThreads, TimeUnit.SECONDS);
                tasks.clear();
            }
        }
    }

    private int fetch(String url) {
        WebPage page = fetchComponent.fetch(url);
        if (page.getProtocolStatus().isFailed()) {
            return -1;
        }

        metricsCounters.increase(Counter.rFetched);

        CrawlMarks marks = page.getMarks();
        if (parse && marks.contains(Mark.FETCH)) {
            ParseResult parseResult = parseComponent.parse(page);
            if (parseResult.isSuccess()) {
                metricsCounters.increase(Counter.rParsed);
                if (index && marks.contains(Mark.PARSE)) {
                    IndexDocument doc = indexComponent.index(page);
                    if (doc != null) {
                        metricsCounters.increase(Counter.rIndexed);
                    }
                }
            } // if page not null
        }

        metricsCounters.increase(CommonCounter.rPersist);
        try {
            context.write(page.getKey(), page.unbox());
        } catch (IOException | InterruptedException e) {
            LOG.error(StringUtil.stringifyException(e));
            return -2;
        }

        return 0;
    }

    private enum Counter {rFetched, rParsed, rIndexed}
}
