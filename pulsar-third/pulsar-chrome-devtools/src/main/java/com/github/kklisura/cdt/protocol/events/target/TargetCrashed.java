package com.github.kklisura.cdt.protocol.events.target;

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

/** Issued when a target has crashed. */
public class TargetCrashed {

  private String targetId;

  private String status;

  private Integer errorCode;

  public String getTargetId() {
    return targetId;
  }

  public void setTargetId(String targetId) {
    this.targetId = targetId;
  }

  /** Termination status type. */
  public String getStatus() {
    return status;
  }

  /** Termination status type. */
  public void setStatus(String status) {
    this.status = status;
  }

  /** Termination error code. */
  public Integer getErrorCode() {
    return errorCode;
  }

  /** Termination error code. */
  public void setErrorCode(Integer errorCode) {
    this.errorCode = errorCode;
  }
}
