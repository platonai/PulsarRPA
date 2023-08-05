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

/** The reason why Chrome uses a specific transport protocol for HTTP semantics. */
public enum AlternateProtocolUsage {
  @JsonProperty("alternativeJobWonWithoutRace")
  ALTERNATIVE_JOB_WON_WITHOUT_RACE,
  @JsonProperty("alternativeJobWonRace")
  ALTERNATIVE_JOB_WON_RACE,
  @JsonProperty("mainJobWonRace")
  MAIN_JOB_WON_RACE,
  @JsonProperty("mappingMissing")
  MAPPING_MISSING,
  @JsonProperty("broken")
  BROKEN,
  @JsonProperty("dnsAlpnH3JobWonWithoutRace")
  DNS_ALPN_H_3JOB_WON_WITHOUT_RACE,
  @JsonProperty("dnsAlpnH3JobWonRace")
  DNS_ALPN_H_3JOB_WON_RACE,
  @JsonProperty("unspecifiedReason")
  UNSPECIFIED_REASON
}
