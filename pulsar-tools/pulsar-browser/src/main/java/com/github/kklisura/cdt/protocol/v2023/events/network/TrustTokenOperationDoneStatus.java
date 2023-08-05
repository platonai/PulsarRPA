package com.github.kklisura.cdt.protocol.v2023.events.network;

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

/**
 * Detailed success or error status of the operation. 'AlreadyExists' also signifies a successful
 * operation, as the result of the operation already exists und thus, the operation was abort
 * preemptively (e.g. a cache hit).
 */
public enum TrustTokenOperationDoneStatus {
  @JsonProperty("Ok")
  OK,
  @JsonProperty("InvalidArgument")
  INVALID_ARGUMENT,
  @JsonProperty("MissingIssuerKeys")
  MISSING_ISSUER_KEYS,
  @JsonProperty("FailedPrecondition")
  FAILED_PRECONDITION,
  @JsonProperty("ResourceExhausted")
  RESOURCE_EXHAUSTED,
  @JsonProperty("AlreadyExists")
  ALREADY_EXISTS,
  @JsonProperty("Unavailable")
  UNAVAILABLE,
  @JsonProperty("Unauthorized")
  UNAUTHORIZED,
  @JsonProperty("BadResponse")
  BAD_RESPONSE,
  @JsonProperty("InternalError")
  INTERNAL_ERROR,
  @JsonProperty("UnknownError")
  UNKNOWN_ERROR,
  @JsonProperty("FulfilledLocally")
  FULFILLED_LOCALLY
}
