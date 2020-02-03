package com.github.kklisura.cdt.protocol.types.network;

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

/** Resource type as it was perceived by the rendering engine. */
public enum ResourceType {
  @JsonProperty("Document")
  DOCUMENT,
  @JsonProperty("Stylesheet")
  STYLESHEET,
  @JsonProperty("Image")
  IMAGE,
  @JsonProperty("Media")
  MEDIA,
  @JsonProperty("Font")
  FONT,
  @JsonProperty("Script")
  SCRIPT,
  @JsonProperty("TextTrack")
  TEXT_TRACK,
  @JsonProperty("XHR")
  XHR,
  @JsonProperty("Fetch")
  FETCH,
  @JsonProperty("EventSource")
  EVENT_SOURCE,
  @JsonProperty("WebSocket")
  WEB_SOCKET,
  @JsonProperty("Manifest")
  MANIFEST,
  @JsonProperty("SignedExchange")
  SIGNED_EXCHANGE,
  @JsonProperty("Ping")
  PING,
  @JsonProperty("CSPViolationReport")
  CSP_VIOLATION_REPORT,
  @JsonProperty("Other")
  OTHER
}
