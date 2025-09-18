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

public enum MixedContentResourceType {
  @JsonProperty("AttributionSrc")
  ATTRIBUTION_SRC,
  @JsonProperty("Audio")
  AUDIO,
  @JsonProperty("Beacon")
  BEACON,
  @JsonProperty("CSPReport")
  CSP_REPORT,
  @JsonProperty("Download")
  DOWNLOAD,
  @JsonProperty("EventSource")
  EVENT_SOURCE,
  @JsonProperty("Favicon")
  FAVICON,
  @JsonProperty("Font")
  FONT,
  @JsonProperty("Form")
  FORM,
  @JsonProperty("Frame")
  FRAME,
  @JsonProperty("Image")
  IMAGE,
  @JsonProperty("Import")
  IMPORT,
  @JsonProperty("Manifest")
  MANIFEST,
  @JsonProperty("Ping")
  PING,
  @JsonProperty("PluginData")
  PLUGIN_DATA,
  @JsonProperty("PluginResource")
  PLUGIN_RESOURCE,
  @JsonProperty("Prefetch")
  PREFETCH,
  @JsonProperty("Resource")
  RESOURCE,
  @JsonProperty("Script")
  SCRIPT,
  @JsonProperty("ServiceWorker")
  SERVICE_WORKER,
  @JsonProperty("SharedWorker")
  SHARED_WORKER,
  @JsonProperty("Stylesheet")
  STYLESHEET,
  @JsonProperty("Track")
  TRACK,
  @JsonProperty("Video")
  VIDEO,
  @JsonProperty("Worker")
  WORKER,
  @JsonProperty("XMLHttpRequest")
  XML_HTTP_REQUEST,
  @JsonProperty("XSLT")
  XSLT
}
