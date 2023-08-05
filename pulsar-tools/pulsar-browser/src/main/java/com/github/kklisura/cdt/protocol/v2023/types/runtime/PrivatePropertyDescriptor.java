package com.github.kklisura.cdt.protocol.v2023.types.runtime;

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

import com.github.kklisura.cdt.protocol.v2023.support.annotations.Experimental;
import com.github.kklisura.cdt.protocol.v2023.support.annotations.Optional;

/** Object private field descriptor. */
@Experimental
public class PrivatePropertyDescriptor {

  private String name;

  @Optional
  private RemoteObject value;

  @Optional private RemoteObject get;

  @Optional private RemoteObject set;

  /** Private property name. */
  public String getName() {
    return name;
  }

  /** Private property name. */
  public void setName(String name) {
    this.name = name;
  }

  /** The value associated with the private property. */
  public RemoteObject getValue() {
    return value;
  }

  /** The value associated with the private property. */
  public void setValue(RemoteObject value) {
    this.value = value;
  }

  /**
   * A function which serves as a getter for the private property, or `undefined` if there is no
   * getter (accessor descriptors only).
   */
  public RemoteObject getGet() {
    return get;
  }

  /**
   * A function which serves as a getter for the private property, or `undefined` if there is no
   * getter (accessor descriptors only).
   */
  public void setGet(RemoteObject get) {
    this.get = get;
  }

  /**
   * A function which serves as a setter for the private property, or `undefined` if there is no
   * setter (accessor descriptors only).
   */
  public RemoteObject getSet() {
    return set;
  }

  /**
   * A function which serves as a setter for the private property, or `undefined` if there is no
   * setter (accessor descriptors only).
   */
  public void setSet(RemoteObject set) {
    this.set = set;
  }
}
