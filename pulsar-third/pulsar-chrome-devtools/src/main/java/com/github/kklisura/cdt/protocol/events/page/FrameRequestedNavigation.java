package com.github.kklisura.cdt.protocol.events.page;

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

import com.github.kklisura.cdt.protocol.support.annotations.Experimental;
import com.github.kklisura.cdt.protocol.types.page.ClientNavigationReason;

/**
 * Fired when a renderer-initiated navigation is requested. Navigation may still be cancelled after
 * the event is issued.
 */
@Experimental
public class FrameRequestedNavigation {

  private String frameId;

  private ClientNavigationReason reason;

  private String url;

  /** Id of the frame that is being navigated. */
  public String getFrameId() {
    return frameId;
  }

  /** Id of the frame that is being navigated. */
  public void setFrameId(String frameId) {
    this.frameId = frameId;
  }

  /** The reason for the navigation. */
  public ClientNavigationReason getReason() {
    return reason;
  }

  /** The reason for the navigation. */
  public void setReason(ClientNavigationReason reason) {
    this.reason = reason;
  }

  /** The destination URL for the requested navigation. */
  public String getUrl() {
    return url;
  }

  /** The destination URL for the requested navigation. */
  public void setUrl(String url) {
    this.url = url;
  }
}
