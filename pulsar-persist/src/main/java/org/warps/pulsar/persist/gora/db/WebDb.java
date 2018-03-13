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
package org.warps.pulsar.persist.gora.db;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.store.DataStore;
import org.apache.gora.util.GoraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.warps.pulsar.common.UrlUtil;
import org.warps.pulsar.common.config.ImmutableConfig;
import org.warps.pulsar.persist.WebPage;
import org.warps.pulsar.persist.gora.GoraStorage;
import org.warps.pulsar.persist.gora.generated.GWebPage;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.warps.pulsar.common.PulsarConstants.UNICODE_LAST_CODE_POINT;
import static org.warps.pulsar.common.UrlUtil.reverseUrlOrNull;

public class WebDb implements AutoCloseable {

    public static final Logger LOG = LoggerFactory.getLogger(WebDb.class);

    private final ImmutableConfig conf;
    private final DataStore<String, GWebPage> store;
    private AtomicBoolean closed = new AtomicBoolean(false);

    public WebDb(ImmutableConfig conf) throws RuntimeException {
        this.conf = conf;
        try {
            store = GoraStorage.createDataStore(conf.unbox(), String.class, GWebPage.class);
        } catch (ClassNotFoundException | GoraException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Nullable
    public static WebDb create(ImmutableConfig conf) {
        try (WebDb webDb = new WebDb(conf)) {
            return webDb;
        } catch (Throwable e) {
            LOG.error(e.getMessage());
        }
        return null;
    }

    public DataStore<String, GWebPage> getStore() {
        return store;
    }

    public String getSchemaName() {
        return store.getSchemaName();
    }


    /**
     * Returns the WebPage corresponding to the given url.
     *
     * @param url the url of the WebPage
     * @return the WebPage corresponding to the key or null if it cannot be found
     */
    public WebPage get(String url) {
        return get(url, null);
    }

    /**
     * Returns the WebPage corresponding to the given url.
     *
     * @param url    the url of the WebPage
     * @param fields the fields required in the WebPage. Pass null, to retrieve all fields
     * @return the WebPage corresponding to the key or null if it cannot be found
     */
    public WebPage get(String url, @Nullable String[] fields) {
        Objects.requireNonNull(url);
        String key = UrlUtil.reverseUrlOrEmpty(url);

        if (LOG.isTraceEnabled()) {
            LOG.trace("Getting " + key);
        }
        GWebPage goraWebPage = store.get(key, fields);
        if (goraWebPage != null) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Got " + key);
            }

            return WebPage.box(url, key, goraWebPage);
        }

        return null;
    }

    /**
     * Returns the WebPage corresponding to the given url.
     *
     * @param url the url of the WebPage
     * @return the WebPage corresponding to the key or WebPage.NIL if it cannot be found
     */
    public WebPage getOrNil(String url) {
        return getOrNil(url, null);
    }

    /**
     * Returns the WebPage corresponding to the given url.
     *
     * @param url the url of the WebPage
     * @return the WebPage corresponding to the key or WebPage.NIL if it cannot be found
     */
    public WebPage getOrNil(String url, @Nullable String[] fields) {
        WebPage page = get(url, null);
        return page == null ? WebPage.NIL : page;
    }

    public boolean put(String url, WebPage page) {
        return put(url, page, false);
    }

    public boolean put(String url, WebPage page, boolean replaceIfExists) {
        if (!url.equals(page.getUrl())) {
            LOG.warn("Url and page.getUrl() does not match. {} <-> {}", url, page.getUrl());
        }

        return put(page, replaceIfExists);
    }

    /**
     * Notice:
     * There are comments in gora-hbase-0.6.1, HBaseStore.java, line 259:
     * "HBase sometimes does not delete arbitrarily"
     */
    private boolean put(WebPage page, boolean replaceIfExists) {
        Objects.requireNonNull(page);

        // Never update NIL page
        if (page.isNil()) {
            return false;
        }

        String key = page.getReversedUrl();
        if (key.isEmpty()) {
            return false;
        }

        if (replaceIfExists) {
            store.delete(key);
        }

        if (LOG.isTraceEnabled()) {
            LOG.trace("Putting " + key);
        }

        store.put(key, page.unbox());
        return true;
    }

    public void putAll(Iterable<WebPage> pages) {
        pages.forEach(page -> put(page.getUrl(), page));
    }

    public boolean delete(String url) {
        String reversedUrl = reverseUrlOrNull(url);
        if (reversedUrl != null) {
            return store.delete(reversedUrl);
        }

        return false;
    }

    public boolean truncate() {
        return truncate(false);
    }

    public boolean truncate(boolean force) {
        String schemaName = store.getSchemaName();
        if (force) {
            store.truncateSchema();
            LOG.info("Schema " + schemaName + " is truncated");
            return true;
        }

        if (schemaName.startsWith("tmp_") || schemaName.endsWith("_tmp_webpage")) {
            store.truncateSchema();
            LOG.info("Schema " + schemaName + " is truncated");
            return true;
        } else {
            LOG.info("Only schema name starts with tmp_ or ends with _tmp_webpage can be truncated using this API, " +
                    "bug got " + schemaName);
            return false;
        }
    }

    /**
     * Scan all pages who's url starts with {@param baseUrl}
     * @param baseUrl The base url
     * @return The iterator to retrieve pages
     * */
    public DbIterator scan(String baseUrl) {
        Query<String, GWebPage> query = store.newQuery();
        query.setKeyRange(reverseUrlOrNull(baseUrl), reverseUrlOrNull(baseUrl + UNICODE_LAST_CODE_POINT));

        Result<String, GWebPage> result = store.execute(query);
        return new DbIterator(result);
    }

    /**
     * Scan all pages who's url starts with {@param baseUrl}
     * @param baseUrl The base url
     * @return The iterator to retrieve pages
     * */
    public DbIterator scan(String baseUrl, String[] fields) {
        Query<String, GWebPage> query = store.newQuery();
        query.setKeyRange(reverseUrlOrNull(baseUrl), reverseUrlOrNull(baseUrl + UNICODE_LAST_CODE_POINT));
        query.setFields(fields);

        Result<String, GWebPage> result = store.execute(query);
        return new DbIterator(result);
    }

    /**
     * Scan all pages matches the {@param query}
     * @param query The query
     * @return The iterator to retrieve pages
     * */
    public DbIterator query(DbQuery query) {
        Query<String, GWebPage> goraQuery = store.newQuery();

        String startKey = reverseUrlOrNull(query.getStartUrl());
        String endKey = reverseUrlOrNull(query.getEndUrl());

        if (endKey != null) {
            endKey = endKey.replaceAll("\\uFFFF", UNICODE_LAST_CODE_POINT.toString());
            endKey = endKey.replaceAll("\\\\uFFFF", UNICODE_LAST_CODE_POINT.toString());
        }

        goraQuery.setStartKey(startKey);
        goraQuery.setEndKey(endKey);

        String[] fields = prepareFields(query.getFields());
        goraQuery.setFields(fields);
        Result<String, GWebPage> result = store.execute(goraQuery);

        return new DbIterator(result);
    }

    public void flush() {
        store.flush();
    }

    @Override
    public void close() {
        if (closed.getAndSet(true)) {
            return;
        }

        store.flush();
        store.close();
    }

    private String[] prepareFields(Set<String> fields) {
        if (CollectionUtils.isEmpty(fields)) {
            return GWebPage._ALL_FIELDS;
        }
        fields.remove("url");
        return fields.toArray(new String[fields.size()]);
    }
}
