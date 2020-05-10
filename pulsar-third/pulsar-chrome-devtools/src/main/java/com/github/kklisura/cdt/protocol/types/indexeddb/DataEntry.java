package com.github.kklisura.cdt.protocol.types.indexeddb;

/*-
 * #%L
 * cdt-java-client
 * %%
 * Copyright (C) 2018 - 2019 Kenan Klisura
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

import com.github.kklisura.cdt.protocol.types.runtime.RemoteObject;

/** Data entry. */
public class DataEntry {

  private RemoteObject key;

  private RemoteObject primaryKey;

  private RemoteObject value;

  /** Key object. */
  public RemoteObject getKey() {
    return key;
  }

  /** Key object. */
  public void setKey(RemoteObject key) {
    this.key = key;
  }

  /** Primary key object. */
  public RemoteObject getPrimaryKey() {
    return primaryKey;
  }

  /** Primary key object. */
  public void setPrimaryKey(RemoteObject primaryKey) {
    this.primaryKey = primaryKey;
  }

  /** Value object. */
  public RemoteObject getValue() {
    return value;
  }

  /** Value object. */
  public void setValue(RemoteObject value) {
    this.value = value;
  }
}
