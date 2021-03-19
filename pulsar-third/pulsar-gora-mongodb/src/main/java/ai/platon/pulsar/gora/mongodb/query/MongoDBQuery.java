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
package ai.platon.pulsar.gora.mongodb.query;

import ai.platon.pulsar.gora.mongodb.store.MongoMapping;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.Query;
import org.apache.gora.query.impl.QueryBase;
import org.apache.gora.store.DataStore;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * MongoDB specific implementation of the {@link Query} interface.
 * 
 * @author Fabien Poulard fpoulard@dictanova.com
 */
public class MongoDBQuery<K, T extends PersistentBase> extends QueryBase<K, T> {

  public MongoDBQuery() {
    super(null);
  }

  public MongoDBQuery(DataStore<K, T> dataStore) {
    super(dataStore);
  }

  /**
   * Compute the query itself. Only make use of the keys for querying.
   * 
   * @return a {@link DBObject} corresponding to the query
   */
  public static DBObject toDBQuery(Query<?, ?> query) {
    BasicDBObject q = new BasicDBObject();
    if ((query.getStartKey() != null) && (query.getEndKey() != null)
        && query.getStartKey().equals(query.getEndKey())) {
      q.put("_id", query.getStartKey());
    } else {
      if (query.getStartKey() != null)
        q.put("_id", new BasicDBObject("$gte", query.getStartKey()));
      if (query.getEndKey() != null)
        q.put("_id", new BasicDBObject("$lte", query.getEndKey()));
    }

    return q;
  }

  /**
   * Compute the projection of the query, that is the fields that will be
   * retrieved from the database.
   * 
   * @return a {@link DBObject} corresponding to the list of field to be
   *         retrieved with the associated boolean
   */
  public static DBObject toProjection(String[] fields, MongoMapping mapping) {
    BasicDBObject proj = new BasicDBObject();

    for (String k : fields) {
      String dbFieldName = mapping.getDocumentField(k);
      if (dbFieldName != null && dbFieldName.length() > 0) {
        proj.put(dbFieldName, true);
      }
    }

    return proj;
  }
}
