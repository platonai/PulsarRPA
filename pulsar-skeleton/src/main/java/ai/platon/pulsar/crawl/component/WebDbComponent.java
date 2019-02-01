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

package ai.platon.pulsar.crawl.component;

import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.common.config.Params;
import ai.platon.pulsar.persist.WebDb;
import ai.platon.pulsar.persist.WebPage;
import ai.platon.pulsar.persist.WebPageFormatter;
import ai.platon.pulsar.persist.gora.db.DbQuery;
import ai.platon.pulsar.persist.gora.db.DbQueryResult;
import org.apache.gora.util.GoraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

import static ai.platon.pulsar.common.config.CapabilityTypes.CRAWL_ID;
import static ai.platon.pulsar.common.config.PulsarConstants.ALL_BATCHES;

/**
 * Parser checker, useful for testing parser. It also accurately reports
 * possible fetching and parsing failures and presents protocol status signals
 * to aid debugging. The tool enables us to retrieve the following data from any
 */
public class WebDbComponent implements AutoCloseable {

    public static final Logger LOG = LoggerFactory.getLogger(WebDbComponent.class);

    private ImmutableConfig conf;
    private WebDb webDb;
    private AtomicBoolean isClosed = new AtomicBoolean();

    public WebDbComponent(ImmutableConfig conf) throws GoraException, ClassNotFoundException {
        this(new WebDb(conf), conf);
    }

    public WebDbComponent(WebDb webDb, ImmutableConfig conf) {
        this.webDb = webDb;
        this.conf = conf;
    }

    public void put(String url, WebPage page) {
        webDb.put(url, page);
    }

    public void put(WebPage page) {
        webDb.put(page.getUrl(), page);
    }

    public void flush() {
        webDb.flush();
    }

    public WebPage get(String url) {
        return webDb.getOrNil(url);
    }

    public boolean delete(String url) {
        return webDb.delete(url);
    }

    public boolean truncate() {
        return webDb.truncate();
    }

    public DbQueryResult scan(String startUrl, String endUrl) {
        DbQueryResult result = new DbQueryResult();
        DbQuery query = new DbQuery(conf.get(CRAWL_ID), ALL_BATCHES, startUrl, endUrl);

        Params.of("startUrl", startUrl, "endUrl", endUrl).withLogger(LOG).debug(true);

        Iterator<WebPage> iterator = webDb.query(query);

        while (iterator.hasNext()) {
            result.addValue(new WebPageFormatter(iterator.next()).toMap(query.getFields()));
        }

        return result;
    }

    public DbQueryResult query(DbQuery query) {
        DbQueryResult result = new DbQueryResult();
        Iterator<WebPage> iterator = webDb.query(query);

        while (iterator.hasNext()) {
            result.addValue(new WebPageFormatter(iterator.next()).toMap(query.getFields()));
        }

        return result;
    }

    @Override
    public void close() {
        if (isClosed.getAndSet(true)) {
            return;
        }

        webDb.close();
    }
}
