package com.github.kklisura.cdt.protocol.v2023.events.security;

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

/**
 * There is a certificate error. If overriding certificate errors is enabled, then it should be
 * handled with the `handleCertificateError` command. Note: this event does not fire if the
 * certificate error has been allowed internally. Only one client per target should override
 * certificate errors at the same time.
 */
@Deprecated
public class CertificateError {

  private Integer eventId;

  private String errorType;

  private String requestURL;

  /** The ID of the event. */
  public Integer getEventId() {
    return eventId;
  }

  /** The ID of the event. */
  public void setEventId(Integer eventId) {
    this.eventId = eventId;
  }

  /** The type of the error. */
  public String getErrorType() {
    return errorType;
  }

  /** The type of the error. */
  public void setErrorType(String errorType) {
    this.errorType = errorType;
  }

  /** The url that was requested. */
  public String getRequestURL() {
    return requestURL;
  }

  /** The url that was requested. */
  public void setRequestURL(String requestURL) {
    this.requestURL = requestURL;
  }
}
