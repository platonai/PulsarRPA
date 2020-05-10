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

/**
 * Fired when same-document navigation happens, e.g. due to history API usage or anchor navigation.
 */
@Experimental
public class NavigatedWithinDocument {

  private String frameId;

  private String url;

  /** Id of the frame. */
  public String getFrameId() {
    return frameId;
  }

  /** Id of the frame. */
  public void setFrameId(String frameId) {
    this.frameId = frameId;
  }

  /** Frame's new url. */
  public String getUrl() {
    return url;
  }

  /** Frame's new url. */
  public void setUrl(String url) {
    this.url = url;
  }
}
