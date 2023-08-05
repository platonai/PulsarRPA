package com.github.kklisura.cdt.protocol.v2023.types.network;

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

import com.fasterxml.jackson.annotation.JsonProperty;

/** Network level fetch failure reason. */
public enum ErrorReason {
  @JsonProperty("Failed")
  FAILED,
  @JsonProperty("Aborted")
  ABORTED,
  @JsonProperty("TimedOut")
  TIMED_OUT,
  @JsonProperty("AccessDenied")
  ACCESS_DENIED,
  @JsonProperty("ConnectionClosed")
  CONNECTION_CLOSED,
  @JsonProperty("ConnectionReset")
  CONNECTION_RESET,
  @JsonProperty("ConnectionRefused")
  CONNECTION_REFUSED,
  @JsonProperty("ConnectionAborted")
  CONNECTION_ABORTED,
  @JsonProperty("ConnectionFailed")
  CONNECTION_FAILED,
  @JsonProperty("NameNotResolved")
  NAME_NOT_RESOLVED,
  @JsonProperty("InternetDisconnected")
  INTERNET_DISCONNECTED,
  @JsonProperty("AddressUnreachable")
  ADDRESS_UNREACHABLE,
  @JsonProperty("BlockedByClient")
  BLOCKED_BY_CLIENT,
  @JsonProperty("BlockedByResponse")
  BLOCKED_BY_RESPONSE
}
