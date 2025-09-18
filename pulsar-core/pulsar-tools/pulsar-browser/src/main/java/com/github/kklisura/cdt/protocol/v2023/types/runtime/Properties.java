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

import java.util.List;

public class Properties {

  private List<PropertyDescriptor> result;

  @Optional
  private List<InternalPropertyDescriptor> internalProperties;

  @Experimental
  @Optional private List<PrivatePropertyDescriptor> privateProperties;

  @Optional private ExceptionDetails exceptionDetails;

  /** Object properties. */
  public List<PropertyDescriptor> getResult() {
    return result;
  }

  /** Object properties. */
  public void setResult(List<PropertyDescriptor> result) {
    this.result = result;
  }

  /** Internal object properties (only of the element itself). */
  public List<InternalPropertyDescriptor> getInternalProperties() {
    return internalProperties;
  }

  /** Internal object properties (only of the element itself). */
  public void setInternalProperties(List<InternalPropertyDescriptor> internalProperties) {
    this.internalProperties = internalProperties;
  }

  /** Object private properties. */
  public List<PrivatePropertyDescriptor> getPrivateProperties() {
    return privateProperties;
  }

  /** Object private properties. */
  public void setPrivateProperties(List<PrivatePropertyDescriptor> privateProperties) {
    this.privateProperties = privateProperties;
  }

  /** Exception details. */
  public ExceptionDetails getExceptionDetails() {
    return exceptionDetails;
  }

  /** Exception details. */
  public void setExceptionDetails(ExceptionDetails exceptionDetails) {
    this.exceptionDetails = exceptionDetails;
  }
}
