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
package org.apache.gora.mongodb.filters;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.gora.filter.*;
import org.apache.gora.mongodb.query.QueryBuilder;
import org.apache.gora.mongodb.store.MongoMapping;
import org.apache.gora.mongodb.store.MongoStore;
import org.apache.gora.persistency.impl.PersistentBase;

import org.bson.Document;

public class DefaultFactory<K, T extends PersistentBase> extends
    BaseFactory<K, T> {
  private static final Log LOG = LogFactory.getLog(DefaultFactory.class);

  @Override
  public List<String> getSupportedFilters() {
    List<String> filters = new ArrayList<>();
    filters.add(SingleFieldValueFilter.class.getCanonicalName());
    filters.add(MapFieldValueFilter.class.getCanonicalName());
    filters.add(FilterList.class.getCanonicalName());
    return filters;
  }

  @Override
  public Document createFilter(final Filter<K, T> filter,
      final MongoStore<K, T> store) {

    if (filter instanceof FilterList) {
      FilterList<K, T> filterList = (FilterList<K, T>) filter;
      return transformListFilter(filterList, store);
    } else if (filter instanceof SingleFieldValueFilter) {
      SingleFieldValueFilter<K, T> fieldFilter = (SingleFieldValueFilter<K, T>) filter;
      return transformFieldFilter(fieldFilter, store);
    } else if (filter instanceof MapFieldValueFilter) {
      MapFieldValueFilter<K, T> mapFilter = (MapFieldValueFilter<K, T>) filter;
      return transformMapFilter(mapFilter, store);
    } else {
      LOG.warn("MongoDB remote filter not yet implemented for "
          + filter.getClass().getCanonicalName());
      return null;
    }
  }

  protected Document transformListFilter(final FilterList<K, T> filterList,
      final MongoStore<K, T> store) {
    Document query = new Document();
    for (Filter<K, T> filter : filterList.getFilters()) {
      boolean succeeded = getFilterUtil().setFilter(query, filter, store);
      if (!succeeded) {
        return null;
      }
    }
    return query;
  }

  protected Document transformFieldFilter(
      final SingleFieldValueFilter<K, T> fieldFilter,
      final MongoStore<K, T> store) {
    MongoMapping mapping = store.getMapping();
    String dbFieldName = mapping.getDocumentField(fieldFilter.getFieldName());

    FilterOp filterOp = fieldFilter.getFilterOp();
    List<Object> operands = fieldFilter.getOperands();

//    MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017");
//    MongoDatabase database = mongoClient.getDatabase("my-database");
//    MongoCollection<Document> collection = database.getCollection("test-collection");

    QueryBuilder builder = QueryBuilder.start(dbFieldName);
    builder = appendToBuilder(builder, filterOp, operands);
    if (!fieldFilter.isFilterIfMissing()) {
      // If false, the find query will pass if the column is not found.
      Document notExist = QueryBuilder.start(dbFieldName).exists(false).get();
      builder = QueryBuilder.start().or(notExist, builder.get());
    }

    return builder.get();
  }

  protected Document transformMapFilter(
      final MapFieldValueFilter<K, T> mapFilter, final MongoStore<K, T> store) {
    MongoMapping mapping = store.getMapping();
    String dbFieldName = mapping.getDocumentField(mapFilter.getFieldName())
        + "." + store.encodeFieldKey(mapFilter.getMapKey().toString());

    FilterOp filterOp = mapFilter.getFilterOp();
    List<Object> operands = mapFilter.getOperands();

    QueryBuilder builder = QueryBuilder.start(dbFieldName);
    builder = appendToBuilder(builder, filterOp, operands);
    if (!mapFilter.isFilterIfMissing()) {
      // If false, the find query will pass if the column is not found.
      Document notExist = QueryBuilder.start(dbFieldName).exists(false).get();
      builder = QueryBuilder.start().or(notExist, builder.get());
    }
    return builder.get();
  }

  protected QueryBuilder appendToBuilder(
          QueryBuilder builder, FilterOp filterOp, List<Object> rawOperands) {
    List<String> operands = convertOperandsToString(rawOperands);
    switch (filterOp) {
    case EQUALS:
      if (operands.size() == 1) {
        builder.is(operands.iterator().next());
      } else {
        builder.in(operands);
      }
      break;
    case NOT_EQUALS:
      if (operands.size() == 1) {
        builder.notEquals(operands.iterator().next());
      } else {
        builder.notIn(operands);
      }
      break;
    case LESS:
      builder.lessThan(operands);
      break;
    case LESS_OR_EQUAL:
      builder.lessThanEquals(operands);
      break;
    case GREATER:
      builder.greaterThan(operands);
      break;
    case GREATER_OR_EQUAL:
      builder.greaterThanEquals(operands);
      break;
    default:
      throw new IllegalArgumentException(filterOp
          + " no MongoDB equivalent yet");
    }
    return builder;
  }

  /**
   * Transform all Utf8 into String before preparing MongoDB query.
   * <p>Otherwise, you'll get <tt>RuntimeException: json can't serialize type : Utf8</tt></p>
   *
   * @see <a href="https://issues.apache.org/jira/browse/GORA-388">GORA-388</a>
   */
  private List<String> convertOperandsToString(List<Object> rawOperands) {
    List<String> operands = new ArrayList<>(rawOperands.size());
    for (Object rawOperand : rawOperands) {
      if (rawOperand != null) {
        operands.add(rawOperand.toString());
      }
    }
    return operands;
  }
}
