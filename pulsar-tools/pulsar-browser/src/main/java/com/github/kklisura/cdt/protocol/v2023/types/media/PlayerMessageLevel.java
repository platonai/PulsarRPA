package com.github.kklisura.cdt.protocol.v2023.types.media;

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
 * Keep in sync with MediaLogMessageLevel We are currently keeping the message level 'error'
 * separate from the PlayerError type because right now they represent different things, this one
 * being a DVLOG(ERROR) style log message that gets printed based on what log level is selected in
 * the UI, and the other is a representation of a media::PipelineStatus object. Soon however we're
 * going to be moving away from using PipelineStatus for errors and introducing a new error type
 * which should hopefully let us integrate the error log level into the PlayerError type.
 */
public enum PlayerMessageLevel {
  @JsonProperty("error")
  ERROR,
  @JsonProperty("warning")
  WARNING,
  @JsonProperty("info")
  INFO,
  @JsonProperty("debug")
  DEBUG
}
