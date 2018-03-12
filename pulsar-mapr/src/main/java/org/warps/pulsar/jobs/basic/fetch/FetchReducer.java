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
package org.warps.pulsar.jobs.basic.fetch;

import org.apache.hadoop.io.IntWritable;
import org.warps.pulsar.common.CommonCounter;
import org.warps.pulsar.common.StringUtil;
import org.warps.pulsar.common.UrlUtil;
import org.warps.pulsar.crawl.component.FetchComponent;
import org.warps.pulsar.crawl.component.IndexComponent;
import org.warps.pulsar.crawl.component.ParseComponent;
import org.warps.pulsar.crawl.fetch.TaskStatusTracker;
import org.warps.pulsar.crawl.index.IndexDocument;
import org.warps.pulsar.crawl.parse.ParseResult;
import org.warps.pulsar.crawl.protocol.ProtocolFactory;
import org.warps.pulsar.jobs.common.FetchEntry;
import org.warps.pulsar.jobs.core.GoraReducer;
import org.warps.pulsar.persist.CrawlMarks;
import org.warps.pulsar.persist.WebPage;
import org.warps.pulsar.persist.gora.db.WebDb;
import org.warps.pulsar.persist.gora.generated.GWebPage;
import org.warps.pulsar.persist.metadata.Mark;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.warps.pulsar.common.config.CapabilityTypes.INDEX_JIT;
import static org.warps.pulsar.common.config.CapabilityTypes.PARSE_PARSE;

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
