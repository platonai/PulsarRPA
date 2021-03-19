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

import org.apache.gora.persistency.impl.PersistentBase;

/**
 * Base implementation of a
 * {@link FilterFactory} which just manage back
 * reference to {@link MongoFilterUtil}.
 * 
 * @author Damien Raude-Morvan draudemorvan@dictanova.com
 */
public abstract class BaseFactory<K, T extends PersistentBase> implements
    FilterFactory<K, T> {

  private MongoFilterUtil<K, T> filterUtil;

  @Override
  public MongoFilterUtil<K, T> getFilterUtil() {
    return filterUtil;
  }

  @Override
  public void setFilterUtil(final MongoFilterUtil<K, T> util) {
    this.filterUtil = util;
  }

}
