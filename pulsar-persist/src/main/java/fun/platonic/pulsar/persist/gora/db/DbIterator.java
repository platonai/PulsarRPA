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
package fun.platonic.pulsar.persist.gora.db;

import fun.platonic.pulsar.persist.WebPage;
import fun.platonic.pulsar.persist.gora.generated.GWebPage;
import org.apache.gora.query.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.Predicate;

public class DbIterator implements Iterator<WebPage> {

    public static final Logger LOG = LoggerFactory.getLogger(WebDb.class);

    private Result<String, GWebPage> result;
    private WebPage nextPage;
    private Predicate<WebPage> filter;

    public DbIterator(Result<String, GWebPage> result) {
        Objects.requireNonNull(result);

        this.result = result;
        try {
            moveToNext();
        } catch (Exception e) {
            LOG.error("Failed to create read iterator!" + e);
        }
    }

    public void setFilter(Predicate<WebPage> filter) {
        this.filter = filter;
    }

    @Override
    public boolean hasNext() {
        return nextPage != null;
    }

    @Override
    public WebPage next() {
        WebPage page = nextPage;

        try {
            moveToNext();

            if (!hasNext()) {
                result.close();
            }
        } catch (Exception e) {
            LOG.error("Failed to move to the next record" + e);
        }

        return page;
    }

    private void moveToNext() throws Exception {
        nextPage = null;
        while (nextPage == null && result.next()) {
            WebPage page = WebPage.box(result.getKey(), result.get(), true);
            if (filter == null || filter.test(page)) {
                nextPage = page;
            }
        }
    }
}
