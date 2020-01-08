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

import ai.platon.pulsar.PulsarEnv;
import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.common.config.VolatileConfig;
import ai.platon.pulsar.crawl.component.SeleniumFetchComponent;
import ai.platon.pulsar.crawl.protocol.ForwardingResponse;
import ai.platon.pulsar.crawl.protocol.Response;
import ai.platon.pulsar.crawl.protocol.http.AbstractHttpProtocol;
import ai.platon.pulsar.persist.ProtocolStatus;
import ai.platon.pulsar.persist.WebPage;
import ai.platon.pulsar.protocol.crowd.ForwardingProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class SeleniumProtocol extends ForwardingProtocol {

    private Logger log = LoggerFactory.getLogger(SeleniumProtocol.class);

    private AtomicReference<SeleniumFetchComponent> fetchComponent = new AtomicReference<>();

    private AtomicBoolean closed = new AtomicBoolean();

    public SeleniumProtocol() {
    }

    @Override
    public void close() {
        closed.set(true);
    }

    /**
     * Called just after creation
     * @see ai.platon.pulsar.crawl.protocol.ProtocolFactory#ProtocolFactory
     * */
    @Override
    public void setConf(ImmutableConfig jobConf) {
        super.setConf(jobConf);
    }

    public boolean supportParallel() {
        return true;
    }

    @Override
    public Collection<Response> getResponses(Collection<WebPage> pages, VolatileConfig volatileConfig) {
        if (closed.get()) {
            return Collections.emptyList();
        }

        try {
            fetchComponent.compareAndSet(null, PulsarEnv.Companion.get().getBean(SeleniumFetchComponent.class));
            return fetchComponent.get().parallelFetchAllPages(pages, volatileConfig);
        } catch (Exception e) {
            log.warn("Unexpected exception", e);
        }

        return Collections.emptyList();
    }

    @Override
    public Response getResponse(String url, WebPage page, boolean followRedirects) {
        if (closed.get()) {
            return new ForwardingResponse(url, ProtocolStatus.STATUS_CANCELED);
        }

        try {
            fetchComponent.compareAndSet(null, PulsarEnv.Companion.get().getBean(SeleniumFetchComponent.class));
            Response response = super.getResponse(url, page, followRedirects);
            return response != null ? response : fetchComponent.get().fetchContent(page);
        } catch (Exception e) {
            log.warn("Unexpected exception", e);
            // Unexpected exception, cancel the request, hope to retry in CRAWL_SOLUTION scope
            return new ForwardingResponse(url, ProtocolStatus.STATUS_CANCELED);
        }
    }
}
