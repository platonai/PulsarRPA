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

/** The reason why request was blocked. */
public enum CorsError {
  @JsonProperty("DisallowedByMode")
  DISALLOWED_BY_MODE,
  @JsonProperty("InvalidResponse")
  INVALID_RESPONSE,
  @JsonProperty("WildcardOriginNotAllowed")
  WILDCARD_ORIGIN_NOT_ALLOWED,
  @JsonProperty("MissingAllowOriginHeader")
  MISSING_ALLOW_ORIGIN_HEADER,
  @JsonProperty("MultipleAllowOriginValues")
  MULTIPLE_ALLOW_ORIGIN_VALUES,
  @JsonProperty("InvalidAllowOriginValue")
  INVALID_ALLOW_ORIGIN_VALUE,
  @JsonProperty("AllowOriginMismatch")
  ALLOW_ORIGIN_MISMATCH,
  @JsonProperty("InvalidAllowCredentials")
  INVALID_ALLOW_CREDENTIALS,
  @JsonProperty("CorsDisabledScheme")
  CORS_DISABLED_SCHEME,
  @JsonProperty("PreflightInvalidStatus")
  PREFLIGHT_INVALID_STATUS,
  @JsonProperty("PreflightDisallowedRedirect")
  PREFLIGHT_DISALLOWED_REDIRECT,
  @JsonProperty("PreflightWildcardOriginNotAllowed")
  PREFLIGHT_WILDCARD_ORIGIN_NOT_ALLOWED,
  @JsonProperty("PreflightMissingAllowOriginHeader")
  PREFLIGHT_MISSING_ALLOW_ORIGIN_HEADER,
  @JsonProperty("PreflightMultipleAllowOriginValues")
  PREFLIGHT_MULTIPLE_ALLOW_ORIGIN_VALUES,
  @JsonProperty("PreflightInvalidAllowOriginValue")
  PREFLIGHT_INVALID_ALLOW_ORIGIN_VALUE,
  @JsonProperty("PreflightAllowOriginMismatch")
  PREFLIGHT_ALLOW_ORIGIN_MISMATCH,
  @JsonProperty("PreflightInvalidAllowCredentials")
  PREFLIGHT_INVALID_ALLOW_CREDENTIALS,
  @JsonProperty("PreflightMissingAllowExternal")
  PREFLIGHT_MISSING_ALLOW_EXTERNAL,
  @JsonProperty("PreflightInvalidAllowExternal")
  PREFLIGHT_INVALID_ALLOW_EXTERNAL,
  @JsonProperty("PreflightMissingAllowPrivateNetwork")
  PREFLIGHT_MISSING_ALLOW_PRIVATE_NETWORK,
  @JsonProperty("PreflightInvalidAllowPrivateNetwork")
  PREFLIGHT_INVALID_ALLOW_PRIVATE_NETWORK,
  @JsonProperty("InvalidAllowMethodsPreflightResponse")
  INVALID_ALLOW_METHODS_PREFLIGHT_RESPONSE,
  @JsonProperty("InvalidAllowHeadersPreflightResponse")
  INVALID_ALLOW_HEADERS_PREFLIGHT_RESPONSE,
  @JsonProperty("MethodDisallowedByPreflightResponse")
  METHOD_DISALLOWED_BY_PREFLIGHT_RESPONSE,
  @JsonProperty("HeaderDisallowedByPreflightResponse")
  HEADER_DISALLOWED_BY_PREFLIGHT_RESPONSE,
  @JsonProperty("RedirectContainsCredentials")
  REDIRECT_CONTAINS_CREDENTIALS,
  @JsonProperty("InsecurePrivateNetwork")
  INSECURE_PRIVATE_NETWORK,
  @JsonProperty("InvalidPrivateNetworkAccess")
  INVALID_PRIVATE_NETWORK_ACCESS,
  @JsonProperty("UnexpectedPrivateNetworkAccess")
  UNEXPECTED_PRIVATE_NETWORK_ACCESS,
  @JsonProperty("NoCorsRedirectModeNotFollow")
  NO_CORS_REDIRECT_MODE_NOT_FOLLOW,
  @JsonProperty("PreflightMissingPrivateNetworkAccessId")
  PREFLIGHT_MISSING_PRIVATE_NETWORK_ACCESS_ID,
  @JsonProperty("PreflightMissingPrivateNetworkAccessName")
  PREFLIGHT_MISSING_PRIVATE_NETWORK_ACCESS_NAME,
  @JsonProperty("PrivateNetworkAccessPermissionUnavailable")
  PRIVATE_NETWORK_ACCESS_PERMISSION_UNAVAILABLE,
  @JsonProperty("PrivateNetworkAccessPermissionDenied")
  PRIVATE_NETWORK_ACCESS_PERMISSION_DENIED
}
