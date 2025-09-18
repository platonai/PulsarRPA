package com.github.kklisura.cdt.protocol.v2023.types.indexeddb;

/*-
 * #%L
 * cdt-java-client
 * %%
 * Copyright (C) 2018 - 2023 Kenan Klisura
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.util.List;

/** Object store. */
public class ObjectStore {

  private String name;

  private KeyPath keyPath;

  private Boolean autoIncrement;

  private List<ObjectStoreIndex> indexes;

  /** Object store name. */
  public String getName() {
    return name;
  }

  /** Object store name. */
  public void setName(String name) {
    this.name = name;
  }

  /** Object store key path. */
  public KeyPath getKeyPath() {
    return keyPath;
  }

  /** Object store key path. */
  public void setKeyPath(KeyPath keyPath) {
    this.keyPath = keyPath;
  }

  /** If true, object store has auto increment flag set. */
  public Boolean getAutoIncrement() {
    return autoIncrement;
  }

  /** If true, object store has auto increment flag set. */
  public void setAutoIncrement(Boolean autoIncrement) {
    this.autoIncrement = autoIncrement;
  }

  /** Indexes in this object store. */
  public List<ObjectStoreIndex> getIndexes() {
    return indexes;
  }

  /** Indexes in this object store. */
  public void setIndexes(List<ObjectStoreIndex> indexes) {
    this.indexes = indexes;
  }
}
