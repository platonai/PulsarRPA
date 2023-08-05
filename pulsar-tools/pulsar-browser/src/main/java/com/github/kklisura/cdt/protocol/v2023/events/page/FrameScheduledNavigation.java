package com.github.kklisura.cdt.protocol.v2023.events.page;

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

import com.github.kklisura.cdt.protocol.v2023.types.page.ClientNavigationReason;

/** Fired when frame schedules a potential navigation. */
@Deprecated
public class FrameScheduledNavigation {

  private String frameId;

  private Double delay;

  private ClientNavigationReason reason;

  private String url;

  /** Id of the frame that has scheduled a navigation. */
  public String getFrameId() {
    return frameId;
  }

  /** Id of the frame that has scheduled a navigation. */
  public void setFrameId(String frameId) {
    this.frameId = frameId;
  }

  /**
   * Delay (in seconds) until the navigation is scheduled to begin. The navigation is not guaranteed
   * to start.
   */
  public Double getDelay() {
    return delay;
  }

  /**
   * Delay (in seconds) until the navigation is scheduled to begin. The navigation is not guaranteed
   * to start.
   */
  public void setDelay(Double delay) {
    this.delay = delay;
  }

  /** The reason for the navigation. */
  public ClientNavigationReason getReason() {
    return reason;
  }

  /** The reason for the navigation. */
  public void setReason(ClientNavigationReason reason) {
    this.reason = reason;
  }

  /** The destination URL for the scheduled navigation. */
  public String getUrl() {
    return url;
  }

  /** The destination URL for the scheduled navigation. */
  public void setUrl(String url) {
    this.url = url;
  }
}
