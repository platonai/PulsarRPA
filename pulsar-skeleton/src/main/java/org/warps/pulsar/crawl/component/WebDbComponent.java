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

package org.warps.pulsar.crawl.component;

import org.apache.gora.util.GoraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.warps.pulsar.common.config.ImmutableConfig;
import org.warps.pulsar.common.config.Params;
import org.warps.pulsar.persist.WebPage;
import org.warps.pulsar.persist.WebPageFormatter;
import org.warps.pulsar.persist.gora.db.DbIterator;
import org.warps.pulsar.persist.gora.db.DbQuery;
import org.warps.pulsar.persist.gora.db.DbQueryResult;
import org.warps.pulsar.persist.gora.db.WebDb;

import static org.warps.pulsar.common.PulsarConstants.ALL_BATCHES;
import static org.warps.pulsar.common.config.CapabilityTypes.CRAWL_ID;

/**
 * Parser checker, useful for testing parser. It also accurately reports
 * possible fetching and parsing failures and presents protocol status signals
 * to aid debugging. The tool enables us to retrieve the following data from any
 */
public class WebDbComponent implements AutoCloseable {

    public static final Logger LOG = LoggerFactory.getLogger(WebDbComponent.class);

    private ImmutableConfig conf;
    private WebDb webDb;

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

        DbIterator iterator = webDb.query(query);

        while (iterator.hasNext()) {
            result.addValue(new WebPageFormatter(iterator.next()).toMap(query.getFields()));
        }

        return result;
    }

    public DbQueryResult query(DbQuery query) {
        DbQueryResult result = new DbQueryResult();
        DbIterator iterator = webDb.query(query);

        while (iterator.hasNext()) {
            result.addValue(new WebPageFormatter(iterator.next()).toMap(query.getFields()));
        }

        return result;
    }

    @Override
    public void close() {
        webDb.close();
    }
}
