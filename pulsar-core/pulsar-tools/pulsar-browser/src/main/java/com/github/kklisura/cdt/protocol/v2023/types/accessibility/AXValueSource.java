package com.github.kklisura.cdt.protocol.v2023.types.accessibility;

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

/** A single source for a computed AX property. */
public class AXValueSource {

  private AXValueSourceType type;

  @Optional
  private AXValue value;

  @Optional private String attribute;

  @Optional private AXValue attributeValue;

  @Optional private Boolean superseded;

  @Optional private AXValueNativeSourceType nativeSource;

  @Optional private AXValue nativeSourceValue;

  @Optional private Boolean invalid;

  @Optional private String invalidReason;

  /** What type of source this is. */
  public AXValueSourceType getType() {
    return type;
  }

  /** What type of source this is. */
  public void setType(AXValueSourceType type) {
    this.type = type;
  }

  /** The value of this property source. */
  public AXValue getValue() {
    return value;
  }

  /** The value of this property source. */
  public void setValue(AXValue value) {
    this.value = value;
  }

  /** The name of the relevant attribute, if any. */
  public String getAttribute() {
    return attribute;
  }

  /** The name of the relevant attribute, if any. */
  public void setAttribute(String attribute) {
    this.attribute = attribute;
  }

  /** The value of the relevant attribute, if any. */
  public AXValue getAttributeValue() {
    return attributeValue;
  }

  /** The value of the relevant attribute, if any. */
  public void setAttributeValue(AXValue attributeValue) {
    this.attributeValue = attributeValue;
  }

  /** Whether this source is superseded by a higher priority source. */
  public Boolean getSuperseded() {
    return superseded;
  }

  /** Whether this source is superseded by a higher priority source. */
  public void setSuperseded(Boolean superseded) {
    this.superseded = superseded;
  }

  /** The native markup source for this value, e.g. a `<label>` element. */
  public AXValueNativeSourceType getNativeSource() {
    return nativeSource;
  }

  /** The native markup source for this value, e.g. a `<label>` element. */
  public void setNativeSource(AXValueNativeSourceType nativeSource) {
    this.nativeSource = nativeSource;
  }

  /** The value, such as a node or node list, of the native source. */
  public AXValue getNativeSourceValue() {
    return nativeSourceValue;
  }

  /** The value, such as a node or node list, of the native source. */
  public void setNativeSourceValue(AXValue nativeSourceValue) {
    this.nativeSourceValue = nativeSourceValue;
  }

  /** Whether the value for this property is invalid. */
  public Boolean getInvalid() {
    return invalid;
  }

  /** Whether the value for this property is invalid. */
  public void setInvalid(Boolean invalid) {
    this.invalid = invalid;
  }

  /** Reason for the value being invalid, if it is. */
  public String getInvalidReason() {
    return invalidReason;
  }

  /** Reason for the value being invalid, if it is. */
  public void setInvalidReason(String invalidReason) {
    this.invalidReason = invalidReason;
  }
}
