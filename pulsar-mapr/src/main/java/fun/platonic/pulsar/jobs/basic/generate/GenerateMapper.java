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
package fun.platonic.pulsar.jobs.basic.generate;

import fun.platonic.pulsar.common.config.Params;
import fun.platonic.pulsar.crawl.filter.UrlFilters;
import fun.platonic.pulsar.crawl.filter.UrlNormalizers;
import fun.platonic.pulsar.crawl.schedule.DefaultFetchSchedule;
import fun.platonic.pulsar.crawl.schedule.FetchSchedule;
import fun.platonic.pulsar.crawl.scoring.ScoreVector;
import fun.platonic.pulsar.crawl.scoring.ScoringFilters;
import fun.platonic.pulsar.jobs.common.SelectorEntry;
import fun.platonic.pulsar.jobs.core.GoraMapper;
import fun.platonic.pulsar.persist.WebPage;
import fun.platonic.pulsar.persist.gora.generated.GWebPage;
import org.slf4j.Logger;

import java.io.IOException;

public class GenerateMapper extends GoraMapper<String, GWebPage, SelectorEntry, GWebPage> {
    public static final Logger LOG = GenerateJob.LOG;

    private UrlFilters urlFilters = new UrlFilters();
    private UrlNormalizers urlNormalizers = new UrlNormalizers();
    private ScoringFilters scoringFilters = new ScoringFilters();
    private FetchSchedule fetchSchedule = new DefaultFetchSchedule();

    @Override
    public void setup(Context context) throws IOException, InterruptedException {
        Params.of(
                "className", this.getClass().getSimpleName(),
                "scoringFilters", scoringFilters,
                "urlNormalizers", urlNormalizers,
                "urlFilters", urlFilters
        ).withLogger(LOG).info();
    }

    @Override
    public void map(String reversedUrl, GWebPage row, Context context) throws IOException, InterruptedException {
        WebPage page = WebPage.box(reversedUrl, row, true);
        String url = page.getUrl();

        url = urlNormalizers.normalize(url, UrlNormalizers.SCOPE_GENERATE_HOST_COUNT);
        url = urlFilters.filter(url);
        if (url == null) {
            return;
        }

        if (!fetchSchedule.shouldFetch(page, startTime)) {
            return;
        }

        ScoreVector sortScore = scoringFilters.generatorSortValue(page, 1.0f);

        context.write(new SelectorEntry(url, sortScore), page.unbox());
    }
}
