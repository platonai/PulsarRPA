package com.github.kklisura.cdt.protocol.v2023.types.layertree;

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

/** Reason for rectangle to force scrolling on the main thread */
public enum ScrollRectType {
  @JsonProperty("RepaintsOnScroll")
  REPAINTS_ON_SCROLL,
  @JsonProperty("TouchEventHandler")
  TOUCH_EVENT_HANDLER,
  @JsonProperty("WheelEventHandler")
  WHEEL_EVENT_HANDLER
}
