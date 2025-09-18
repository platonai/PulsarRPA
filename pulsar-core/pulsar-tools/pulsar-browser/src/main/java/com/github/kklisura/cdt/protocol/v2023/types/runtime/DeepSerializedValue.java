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

import com.github.kklisura.cdt.protocol.v2023.support.annotations.Optional;

/** Represents deep serialized value. */
public class DeepSerializedValue {

  private DeepSerializedValueType type;

  @Optional
  private Object value;

  @Optional private String objectId;

  @Optional private Integer weakLocalObjectReference;

  public DeepSerializedValueType getType() {
    return type;
  }

  public void setType(DeepSerializedValueType type) {
    this.type = type;
  }

  public Object getValue() {
    return value;
  }

  public void setValue(Object value) {
    this.value = value;
  }

  public String getObjectId() {
    return objectId;
  }

  public void setObjectId(String objectId) {
    this.objectId = objectId;
  }

  /**
   * Set if value reference met more then once during serialization. In such case, value is provided
   * only to one of the serialized values. Unique per value in the scope of one CDP call.
   */
  public Integer getWeakLocalObjectReference() {
    return weakLocalObjectReference;
  }

  /**
   * Set if value reference met more then once during serialization. In such case, value is provided
   * only to one of the serialized values. Unique per value in the scope of one CDP call.
   */
  public void setWeakLocalObjectReference(Integer weakLocalObjectReference) {
    this.weakLocalObjectReference = weakLocalObjectReference;
  }
}
