/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.platon.pulsar.gora.mongodb.filters;

import java.util.LinkedHashMap;
import java.util.Map;

import ai.platon.pulsar.gora.mongodb.store.MongoStore;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.gora.filter.Filter;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.util.GoraException;
import org.apache.gora.util.ReflectionUtils;
import org.apache.hadoop.conf.Configuration;

import com.mongodb.DBObject;

/**
 * Manage creation of filtering {@link org.apache.gora.query.Query} using
 * configured factories.
 * <p>
 * You can use <tt>{@value #MONGO_FILTER_FACTORIES_PARAMETER}</tt> parameter to
 * change factories implementations used.
 * </p>
 * 
 * @author Damien Raude-Morvan draudemorvan@dictanova.com
 * @see #setFilter(DBObject, Filter,
 *      MongoStore)
 */
public class MongoFilterUtil<K, T extends PersistentBase> {

  /**
   * Default implementation class for FilterFactory.
   */
  public static final String MONGO_FILTERS_DEFAULT_FACTORY = "ai.platon.pulsar.gora.mongodb.filters.DefaultFactory";

  /**
   * Configuration parameter which allow override of FilterFactory used.
   */
  public static final String MONGO_FILTER_FACTORIES_PARAMETER = "gora.mongodb.filter.factories";

  /**
   * Logger.
   */
  private static final Log LOG = LogFactory.getLog(MongoFilterUtil.class);

  private Map<String, FilterFactory<K, T>> factories = new LinkedHashMap<>();

  public MongoFilterUtil(final Configuration conf) throws GoraException {
    String[] factoryClassNames = conf.getStrings(
        MONGO_FILTER_FACTORIES_PARAMETER, MONGO_FILTERS_DEFAULT_FACTORY);

    for (String factoryClass : factoryClassNames) {
      try {
        FilterFactory<K, T> factory = (FilterFactory<K, T>) ReflectionUtils.newInstance(factoryClass);
        for (String filterClass : factory.getSupportedFilters()) {
          factories.put(filterClass, factory);
        }
        factory.setFilterUtil(this);
      } catch (Exception e) {
        throw new GoraException(e);
      }
    }
  }

  public FilterFactory<K, T> getFactory(final Filter<K, T> filter) {
    return factories.get(filter.getClass().getCanonicalName());
  }

  /**
   * Set a filter on the <tt>query</tt>. It translates a Gora filter to a
   * MongoDB filter.
   * 
   * @param query
   *          The Mongo Query
   * @param filter
   *          The Gora filter.
   * @param store
   *          The MongoStore.
   * @return if remote filter is successfully applied.
   */
  public boolean setFilter(final DBObject query, final Filter<K, T> filter,
      final MongoStore<K, T> store) {

    FilterFactory<K, T> factory = getFactory(filter);
    if (factory == null) {
      LOG.warn("MongoDB remote filter factory not yet implemented for "
          + filter.getClass().getCanonicalName());
      return false;
    } else {
      DBObject mongoFilter = factory.createFilter(filter, store);
      if (mongoFilter == null) {
        LOG.warn("MongoDB remote filter not yet implemented for "
            + filter.getClass().getCanonicalName());
        return false;
      } else {
        query.putAll(mongoFilter);
        return true;
      }
    }
  }

}
