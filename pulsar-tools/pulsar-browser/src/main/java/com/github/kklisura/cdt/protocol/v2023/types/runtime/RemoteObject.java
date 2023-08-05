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

/** Mirror object referencing original JavaScript object. */
public class RemoteObject {

  private RemoteObjectType type;

  @Optional
  private RemoteObjectSubtype subtype;

  @Optional private String className;

  @Optional private Object value;

  @Optional private String unserializableValue;

  @Optional private String description;

  @Deprecated @Optional private DeepSerializedValue webDriverValue;

  @Experimental
  @Optional private DeepSerializedValue deepSerializedValue;

  @Optional private String objectId;

  @Experimental @Optional private ObjectPreview preview;

  @Experimental @Optional private CustomPreview customPreview;

  /** Object type. */
  public RemoteObjectType getType() {
    return type;
  }

  /** Object type. */
  public void setType(RemoteObjectType type) {
    this.type = type;
  }

  /**
   * Object subtype hint. Specified for `object` type values only. NOTE: If you change anything
   * here, make sure to also update `subtype` in `ObjectPreview` and `PropertyPreview` below.
   */
  public RemoteObjectSubtype getSubtype() {
    return subtype;
  }

  /**
   * Object subtype hint. Specified for `object` type values only. NOTE: If you change anything
   * here, make sure to also update `subtype` in `ObjectPreview` and `PropertyPreview` below.
   */
  public void setSubtype(RemoteObjectSubtype subtype) {
    this.subtype = subtype;
  }

  /** Object class (constructor) name. Specified for `object` type values only. */
  public String getClassName() {
    return className;
  }

  /** Object class (constructor) name. Specified for `object` type values only. */
  public void setClassName(String className) {
    this.className = className;
  }

  /** Remote object value in case of primitive values or JSON values (if it was requested). */
  public Object getValue() {
    return value;
  }

  /** Remote object value in case of primitive values or JSON values (if it was requested). */
  public void setValue(Object value) {
    this.value = value;
  }

  /**
   * Primitive value which can not be JSON-stringified does not have `value`, but gets this
   * property.
   */
  public String getUnserializableValue() {
    return unserializableValue;
  }

  /**
   * Primitive value which can not be JSON-stringified does not have `value`, but gets this
   * property.
   */
  public void setUnserializableValue(String unserializableValue) {
    this.unserializableValue = unserializableValue;
  }

  /** String representation of the object. */
  public String getDescription() {
    return description;
  }

  /** String representation of the object. */
  public void setDescription(String description) {
    this.description = description;
  }

  /** Deprecated. Use `deepSerializedValue` instead. WebDriver BiDi representation of the value. */
  public DeepSerializedValue getWebDriverValue() {
    return webDriverValue;
  }

  /** Deprecated. Use `deepSerializedValue` instead. WebDriver BiDi representation of the value. */
  public void setWebDriverValue(DeepSerializedValue webDriverValue) {
    this.webDriverValue = webDriverValue;
  }

  /** Deep serialized value. */
  public DeepSerializedValue getDeepSerializedValue() {
    return deepSerializedValue;
  }

  /** Deep serialized value. */
  public void setDeepSerializedValue(DeepSerializedValue deepSerializedValue) {
    this.deepSerializedValue = deepSerializedValue;
  }

  /** Unique object identifier (for non-primitive values). */
  public String getObjectId() {
    return objectId;
  }

  /** Unique object identifier (for non-primitive values). */
  public void setObjectId(String objectId) {
    this.objectId = objectId;
  }

  /** Preview containing abbreviated property values. Specified for `object` type values only. */
  public ObjectPreview getPreview() {
    return preview;
  }

  /** Preview containing abbreviated property values. Specified for `object` type values only. */
  public void setPreview(ObjectPreview preview) {
    this.preview = preview;
  }

  public CustomPreview getCustomPreview() {
    return customPreview;
  }

  public void setCustomPreview(CustomPreview customPreview) {
    this.customPreview = customPreview;
  }
}
