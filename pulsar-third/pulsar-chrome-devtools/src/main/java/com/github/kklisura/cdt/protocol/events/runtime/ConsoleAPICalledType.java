package com.github.kklisura.cdt.protocol.events.runtime;

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

import com.fasterxml.jackson.annotation.JsonProperty;

/** Type of the call. */
public enum ConsoleAPICalledType {
  @JsonProperty("log")
  LOG,
  @JsonProperty("debug")
  DEBUG,
  @JsonProperty("info")
  INFO,
  @JsonProperty("error")
  ERROR,
  @JsonProperty("warning")
  WARNING,
  @JsonProperty("dir")
  DIR,
  @JsonProperty("dirxml")
  DIRXML,
  @JsonProperty("table")
  TABLE,
  @JsonProperty("trace")
  TRACE,
  @JsonProperty("clear")
  CLEAR,
  @JsonProperty("startGroup")
  START_GROUP,
  @JsonProperty("startGroupCollapsed")
  START_GROUP_COLLAPSED,
  @JsonProperty("endGroup")
  END_GROUP,
  @JsonProperty("assert")
  ASSERT,
  @JsonProperty("profile")
  PROFILE,
  @JsonProperty("profileEnd")
  PROFILE_END,
  @JsonProperty("count")
  COUNT,
  @JsonProperty("timeEnd")
  TIME_END
}
