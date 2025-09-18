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

public enum AttributionReportingIssueType {
  @JsonProperty("PermissionPolicyDisabled")
  PERMISSION_POLICY_DISABLED,
  @JsonProperty("UntrustworthyReportingOrigin")
  UNTRUSTWORTHY_REPORTING_ORIGIN,
  @JsonProperty("InsecureContext")
  INSECURE_CONTEXT,
  @JsonProperty("InvalidHeader")
  INVALID_HEADER,
  @JsonProperty("InvalidRegisterTriggerHeader")
  INVALID_REGISTER_TRIGGER_HEADER,
  @JsonProperty("SourceAndTriggerHeaders")
  SOURCE_AND_TRIGGER_HEADERS,
  @JsonProperty("SourceIgnored")
  SOURCE_IGNORED,
  @JsonProperty("TriggerIgnored")
  TRIGGER_IGNORED,
  @JsonProperty("OsSourceIgnored")
  OS_SOURCE_IGNORED,
  @JsonProperty("OsTriggerIgnored")
  OS_TRIGGER_IGNORED,
  @JsonProperty("InvalidRegisterOsSourceHeader")
  INVALID_REGISTER_OS_SOURCE_HEADER,
  @JsonProperty("InvalidRegisterOsTriggerHeader")
  INVALID_REGISTER_OS_TRIGGER_HEADER,
  @JsonProperty("WebAndOsHeaders")
  WEB_AND_OS_HEADERS,
  @JsonProperty("NoWebOrOsSupport")
  NO_WEB_OR_OS_SUPPORT,
  @JsonProperty("NavigationRegistrationWithoutTransientUserActivation")
  NAVIGATION_REGISTRATION_WITHOUT_TRANSIENT_USER_ACTIVATION
}
