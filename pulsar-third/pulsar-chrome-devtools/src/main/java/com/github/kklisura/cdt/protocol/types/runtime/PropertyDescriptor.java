package com.github.kklisura.cdt.protocol.types.runtime;

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

import com.github.kklisura.cdt.protocol.support.annotations.Optional;

/** Object property descriptor. */
public class PropertyDescriptor {

  private String name;

  @Optional private RemoteObject value;

  @Optional private Boolean writable;

  @Optional private RemoteObject get;

  @Optional private RemoteObject set;

  private Boolean configurable;

  private Boolean enumerable;

  @Optional private Boolean wasThrown;

  @Optional private Boolean isOwn;

  @Optional private RemoteObject symbol;

  /** Property name or symbol description. */
  public String getName() {
    return name;
  }

  /** Property name or symbol description. */
  public void setName(String name) {
    this.name = name;
  }

  /** The value associated with the property. */
  public RemoteObject getValue() {
    return value;
  }

  /** The value associated with the property. */
  public void setValue(RemoteObject value) {
    this.value = value;
  }

  /** True if the value associated with the property may be changed (data descriptors only). */
  public Boolean getWritable() {
    return writable;
  }

  /** True if the value associated with the property may be changed (data descriptors only). */
  public void setWritable(Boolean writable) {
    this.writable = writable;
  }

  /**
   * A function which serves as a getter for the property, or `undefined` if there is no getter
   * (accessor descriptors only).
   */
  public RemoteObject getGet() {
    return get;
  }

  /**
   * A function which serves as a getter for the property, or `undefined` if there is no getter
   * (accessor descriptors only).
   */
  public void setGet(RemoteObject get) {
    this.get = get;
  }

  /**
   * A function which serves as a setter for the property, or `undefined` if there is no setter
   * (accessor descriptors only).
   */
  public RemoteObject getSet() {
    return set;
  }

  /**
   * A function which serves as a setter for the property, or `undefined` if there is no setter
   * (accessor descriptors only).
   */
  public void setSet(RemoteObject set) {
    this.set = set;
  }

  /**
   * True if the type of this property descriptor may be changed and if the property may be deleted
   * from the corresponding object.
   */
  public Boolean getConfigurable() {
    return configurable;
  }

  /**
   * True if the type of this property descriptor may be changed and if the property may be deleted
   * from the corresponding object.
   */
  public void setConfigurable(Boolean configurable) {
    this.configurable = configurable;
  }

  /**
   * True if this property shows up during enumeration of the properties on the corresponding
   * object.
   */
  public Boolean getEnumerable() {
    return enumerable;
  }

  /**
   * True if this property shows up during enumeration of the properties on the corresponding
   * object.
   */
  public void setEnumerable(Boolean enumerable) {
    this.enumerable = enumerable;
  }

  /** True if the result was thrown during the evaluation. */
  public Boolean getWasThrown() {
    return wasThrown;
  }

  /** True if the result was thrown during the evaluation. */
  public void setWasThrown(Boolean wasThrown) {
    this.wasThrown = wasThrown;
  }

  /** True if the property is owned for the object. */
  public Boolean getIsOwn() {
    return isOwn;
  }

  /** True if the property is owned for the object. */
  public void setIsOwn(Boolean isOwn) {
    this.isOwn = isOwn;
  }

  /** Property symbol object, if the property is of the `symbol` type. */
  public RemoteObject getSymbol() {
    return symbol;
  }

  /** Property symbol object, if the property is of the `symbol` type. */
  public void setSymbol(RemoteObject symbol) {
    this.symbol = symbol;
  }
}
