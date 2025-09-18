package com.github.kklisura.cdt.protocol.v2023.events.preload;

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

import com.github.kklisura.cdt.protocol.v2023.types.preload.PrefetchStatus;
import com.github.kklisura.cdt.protocol.v2023.types.preload.PreloadingAttemptKey;
import com.github.kklisura.cdt.protocol.v2023.types.preload.PreloadingStatus;

/** Fired when a prefetch attempt is updated. */
public class PrefetchStatusUpdated {

  private PreloadingAttemptKey key;

  private String initiatingFrameId;

  private String prefetchUrl;

  private PreloadingStatus status;

  private PrefetchStatus prefetchStatus;

  private String requestId;

  public PreloadingAttemptKey getKey() {
    return key;
  }

  public void setKey(PreloadingAttemptKey key) {
    this.key = key;
  }

  /** The frame id of the frame initiating prefetch. */
  public String getInitiatingFrameId() {
    return initiatingFrameId;
  }

  /** The frame id of the frame initiating prefetch. */
  public void setInitiatingFrameId(String initiatingFrameId) {
    this.initiatingFrameId = initiatingFrameId;
  }

  public String getPrefetchUrl() {
    return prefetchUrl;
  }

  public void setPrefetchUrl(String prefetchUrl) {
    this.prefetchUrl = prefetchUrl;
  }

  public PreloadingStatus getStatus() {
    return status;
  }

  public void setStatus(PreloadingStatus status) {
    this.status = status;
  }

  public PrefetchStatus getPrefetchStatus() {
    return prefetchStatus;
  }

  public void setPrefetchStatus(PrefetchStatus prefetchStatus) {
    this.prefetchStatus = prefetchStatus;
  }

  public String getRequestId() {
    return requestId;
  }

  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }
}
