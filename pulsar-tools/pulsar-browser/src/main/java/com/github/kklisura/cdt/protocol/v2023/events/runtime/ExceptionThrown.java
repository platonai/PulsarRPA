package com.github.kklisura.cdt.protocol.v2023.events.runtime;

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

import com.github.kklisura.cdt.protocol.v2023.types.runtime.ExceptionDetails;

/** Issued when exception was thrown and unhandled. */
public class ExceptionThrown {

  private Double timestamp;

  private ExceptionDetails exceptionDetails;

  /** Timestamp of the exception. */
  public Double getTimestamp() {
    return timestamp;
  }

  /** Timestamp of the exception. */
  public void setTimestamp(Double timestamp) {
    this.timestamp = timestamp;
  }

  public ExceptionDetails getExceptionDetails() {
    return exceptionDetails;
  }

  public void setExceptionDetails(ExceptionDetails exceptionDetails) {
    this.exceptionDetails = exceptionDetails;
  }
}
