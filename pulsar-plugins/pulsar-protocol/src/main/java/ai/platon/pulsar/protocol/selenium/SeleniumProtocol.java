/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.platon.pulsar.protocol.selenium;

import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.common.config.MutableConfig;
import ai.platon.pulsar.persist.WebPage;
import ai.platon.pulsar.crawl.protocol.Response;
import ai.platon.pulsar.net.SeleniumEngine;
import ai.platon.pulsar.protocol.crowd.ForwardingProtocol;

import java.util.Collection;

public class SeleniumProtocol extends ForwardingProtocol {

    private SeleniumEngine engine;

    public SeleniumProtocol() {
    }

    @Override
    public void close() {
        if (engine != null) {
            engine.close();
            engine = null;
        }
    }

    @Override
    public void setConf(ImmutableConfig conf) {
        super.setConf(conf);
        engine = SeleniumEngine.getInstance(conf);
    }

    public boolean supportParallel() {
        return true;
    }

    @Override
    public Collection<Response> getResponses(Collection<WebPage> pages, MutableConfig mutableConfig) {
        return engine.parallelFetchAllPages(pages, mutableConfig);
    }

    @Override
    public Response getResponse(String url, WebPage page, boolean followRedirects) {
        Response response = super.getResponse(url, page, followRedirects);
        return response != null ? response : engine.fetchContent(page);
    }
}
