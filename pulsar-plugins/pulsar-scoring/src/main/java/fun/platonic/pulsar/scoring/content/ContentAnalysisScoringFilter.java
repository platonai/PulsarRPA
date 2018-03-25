/*
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
 */
package fun.platonic.pulsar.scoring.content;

import fun.platonic.pulsar.common.config.ImmutableConfig;
import fun.platonic.pulsar.common.config.Params;
import fun.platonic.pulsar.crawl.scoring.ScoreVector;
import fun.platonic.pulsar.crawl.scoring.ScoringFilter;
import fun.platonic.pulsar.persist.PageCounters;
import fun.platonic.pulsar.persist.WebPage;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class ContentAnalysisScoringFilter implements ScoringFilter {

    private ImmutableConfig conf;

    public ContentAnalysisScoringFilter() {
    }

    public ContentAnalysisScoringFilter(ImmutableConfig conf) {
        reload(conf);
    }

    @Override
    public Params getParams() {
        return new Params();
    }

    @Override
    public ImmutableConfig getConf() {
        return conf;
    }

    @Override
    public void reload(ImmutableConfig conf) {
        this.conf = conf;
    }

    @Override
    public ScoreVector generatorSortValue(WebPage page, float initSort) {
        return new ScoreVector("1", (int) (page.getScore() * initSort));
    }

    /**
     * Increase the score by a sum of inlinked scores.
     */
    @Override
    public void updateContentScore(WebPage page) {
        page.setContentScore(calculateContentScore(page));
    }

    protected float calculateContentScore(WebPage page) {
        float f1 = 1.2f;
        float f2 = 1.0f;
        float f3 = 1.2f;

        PageCounters pageCounters = page.getPageCounters();
        int re = pageCounters.get(PageCounters.Ref.entity);
        int ra = pageCounters.get(PageCounters.Ref.article);
        int rc = pageCounters.get(PageCounters.Ref.ch);
        long days = ChronoUnit.DAYS.between(page.getRefContentPublishTime(), Instant.now());
        if (days > 10 * 365 || days < 0) {
            // Invalid ref publish time, ignore this parameter
            days = 0;
        }

        float score = f1 * ra + f2 * (rc / 1000) - f3 * days;

        // LOG.info(Params.of("score", score, "re", re, "ra", ra, "rc", rc, "days", days).formatAsLine());

        return score;
    }
}
