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

import java.util.List;

import org.apache.gora.filter.Filter;
import ai.platon.pulsar.gora.mongodb.store.MongoStore;
import org.apache.gora.persistency.impl.PersistentBase;

import com.mongodb.DBObject;

/**
 * Describe factory which create remote filter for MongoDB.
 * 
 * @author Damien Raude-Morvan draudemorvan@dictanova.com
 */
public interface FilterFactory<K, T extends PersistentBase> {

  MongoFilterUtil<K, T> getFilterUtil();

  void setFilterUtil(MongoFilterUtil<K, T> util);

  List<String> getSupportedFilters();

  DBObject createFilter(Filter<K, T> filter, MongoStore<K, T> store);
}
