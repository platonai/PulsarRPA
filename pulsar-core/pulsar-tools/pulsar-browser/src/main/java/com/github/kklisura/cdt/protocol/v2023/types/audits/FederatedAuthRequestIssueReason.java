package com.github.kklisura.cdt.protocol.v2023.types.audits;

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
 * Represents the failure reason when a federated authentication reason fails. Should be updated
 * alongside RequestIdTokenStatus in third_party/blink/public/mojom/devtools/inspector_issue.mojom
 * to include all cases except for success.
 */
public enum FederatedAuthRequestIssueReason {
  @JsonProperty("ShouldEmbargo")
  SHOULD_EMBARGO,
  @JsonProperty("TooManyRequests")
  TOO_MANY_REQUESTS,
  @JsonProperty("WellKnownHttpNotFound")
  WELL_KNOWN_HTTP_NOT_FOUND,
  @JsonProperty("WellKnownNoResponse")
  WELL_KNOWN_NO_RESPONSE,
  @JsonProperty("WellKnownInvalidResponse")
  WELL_KNOWN_INVALID_RESPONSE,
  @JsonProperty("WellKnownListEmpty")
  WELL_KNOWN_LIST_EMPTY,
  @JsonProperty("WellKnownInvalidContentType")
  WELL_KNOWN_INVALID_CONTENT_TYPE,
  @JsonProperty("ConfigNotInWellKnown")
  CONFIG_NOT_IN_WELL_KNOWN,
  @JsonProperty("WellKnownTooBig")
  WELL_KNOWN_TOO_BIG,
  @JsonProperty("ConfigHttpNotFound")
  CONFIG_HTTP_NOT_FOUND,
  @JsonProperty("ConfigNoResponse")
  CONFIG_NO_RESPONSE,
  @JsonProperty("ConfigInvalidResponse")
  CONFIG_INVALID_RESPONSE,
  @JsonProperty("ConfigInvalidContentType")
  CONFIG_INVALID_CONTENT_TYPE,
  @JsonProperty("ClientMetadataHttpNotFound")
  CLIENT_METADATA_HTTP_NOT_FOUND,
  @JsonProperty("ClientMetadataNoResponse")
  CLIENT_METADATA_NO_RESPONSE,
  @JsonProperty("ClientMetadataInvalidResponse")
  CLIENT_METADATA_INVALID_RESPONSE,
  @JsonProperty("ClientMetadataInvalidContentType")
  CLIENT_METADATA_INVALID_CONTENT_TYPE,
  @JsonProperty("DisabledInSettings")
  DISABLED_IN_SETTINGS,
  @JsonProperty("ErrorFetchingSignin")
  ERROR_FETCHING_SIGNIN,
  @JsonProperty("InvalidSigninResponse")
  INVALID_SIGNIN_RESPONSE,
  @JsonProperty("AccountsHttpNotFound")
  ACCOUNTS_HTTP_NOT_FOUND,
  @JsonProperty("AccountsNoResponse")
  ACCOUNTS_NO_RESPONSE,
  @JsonProperty("AccountsInvalidResponse")
  ACCOUNTS_INVALID_RESPONSE,
  @JsonProperty("AccountsListEmpty")
  ACCOUNTS_LIST_EMPTY,
  @JsonProperty("AccountsInvalidContentType")
  ACCOUNTS_INVALID_CONTENT_TYPE,
  @JsonProperty("IdTokenHttpNotFound")
  ID_TOKEN_HTTP_NOT_FOUND,
  @JsonProperty("IdTokenNoResponse")
  ID_TOKEN_NO_RESPONSE,
  @JsonProperty("IdTokenInvalidResponse")
  ID_TOKEN_INVALID_RESPONSE,
  @JsonProperty("IdTokenInvalidRequest")
  ID_TOKEN_INVALID_REQUEST,
  @JsonProperty("IdTokenInvalidContentType")
  ID_TOKEN_INVALID_CONTENT_TYPE,
  @JsonProperty("ErrorIdToken")
  ERROR_ID_TOKEN,
  @JsonProperty("Canceled")
  CANCELED,
  @JsonProperty("RpPageNotVisible")
  RP_PAGE_NOT_VISIBLE,
  @JsonProperty("SilentMediationFailure")
  SILENT_MEDIATION_FAILURE,
  @JsonProperty("ThirdPartyCookiesBlocked")
  THIRD_PARTY_COOKIES_BLOCKED
}
