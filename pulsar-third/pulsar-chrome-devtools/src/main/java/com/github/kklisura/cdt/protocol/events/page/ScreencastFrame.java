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
import com.github.kklisura.cdt.protocol.types.page.ScreencastFrameMetadata;

/** Compressed image data requested by the `startScreencast`. */
@Experimental
public class ScreencastFrame {

  private String data;

  private ScreencastFrameMetadata metadata;

  private Integer sessionId;

  /** Base64-encoded compressed image. */
  public String getData() {
    return data;
  }

  /** Base64-encoded compressed image. */
  public void setData(String data) {
    this.data = data;
  }

  /** Screencast frame metadata. */
  public ScreencastFrameMetadata getMetadata() {
    return metadata;
  }

  /** Screencast frame metadata. */
  public void setMetadata(ScreencastFrameMetadata metadata) {
    this.metadata = metadata;
  }

  /** Frame number. */
  public Integer getSessionId() {
    return sessionId;
  }

  /** Frame number. */
  public void setSessionId(Integer sessionId) {
    this.sessionId = sessionId;
  }
}
