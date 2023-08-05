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
 * Represents the failure reason when a getUserInfo() call fails. Should be updated alongside
 * FederatedAuthUserInfoRequestResult in
 * third_party/blink/public/mojom/devtools/inspector_issue.mojom.
 */
public enum FederatedAuthUserInfoRequestIssueReason {
  @JsonProperty("NotSameOrigin")
  NOT_SAME_ORIGIN,
  @JsonProperty("NotIframe")
  NOT_IFRAME,
  @JsonProperty("NotPotentiallyTrustworthy")
  NOT_POTENTIALLY_TRUSTWORTHY,
  @JsonProperty("NoApiPermission")
  NO_API_PERMISSION,
  @JsonProperty("NotSignedInWithIdp")
  NOT_SIGNED_IN_WITH_IDP,
  @JsonProperty("NoAccountSharingPermission")
  NO_ACCOUNT_SHARING_PERMISSION,
  @JsonProperty("InvalidConfigOrWellKnown")
  INVALID_CONFIG_OR_WELL_KNOWN,
  @JsonProperty("InvalidAccountsResponse")
  INVALID_ACCOUNTS_RESPONSE,
  @JsonProperty("NoReturningUserFromFetchedAccounts")
  NO_RETURNING_USER_FROM_FETCHED_ACCOUNTS
}
