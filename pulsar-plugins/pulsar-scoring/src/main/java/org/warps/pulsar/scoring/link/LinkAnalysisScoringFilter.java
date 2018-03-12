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
package org.warps.pulsar.scoring.link;

import org.warps.pulsar.common.config.ImmutableConfig;
import org.warps.pulsar.common.config.Params;
import org.warps.pulsar.crawl.index.IndexDocument;
import org.warps.pulsar.crawl.scoring.ScoreVector;
import org.warps.pulsar.crawl.scoring.ScoringFilter;
import org.warps.pulsar.persist.WebPage;

public class LinkAnalysisScoringFilter implements ScoringFilter {

    private ImmutableConfig conf;
    private float normalizedScore = 1.00f;

    public LinkAnalysisScoringFilter() {
    }

    public LinkAnalysisScoringFilter(ImmutableConfig conf) {
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
        normalizedScore = conf.getFloat("link.analyze.normalize.score", 1.00f);
    }

    @Override
    public ScoreVector generatorSortValue(WebPage page, float initSort) {
        return new ScoreVector("1", (int) (page.getScore() * initSort));
    }

    @Override
    public float indexerScore(String url, IndexDocument doc, WebPage page, float initScore) {
        return (normalizedScore * page.getScore());
    }
}
