package com.github.kklisura.cdt.protocol.v2023.events.browser;

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

import com.github.kklisura.cdt.protocol.v2023.support.annotations.Experimental;

/** Fired when download makes progress. Last call has |done| == true. */
@Experimental
public class DownloadProgress {

  private String guid;

  private Double totalBytes;

  private Double receivedBytes;

  private DownloadProgressState state;

  /** Global unique identifier of the download. */
  public String getGuid() {
    return guid;
  }

  /** Global unique identifier of the download. */
  public void setGuid(String guid) {
    this.guid = guid;
  }

  /** Total expected bytes to download. */
  public Double getTotalBytes() {
    return totalBytes;
  }

  /** Total expected bytes to download. */
  public void setTotalBytes(Double totalBytes) {
    this.totalBytes = totalBytes;
  }

  /** Total bytes received. */
  public Double getReceivedBytes() {
    return receivedBytes;
  }

  /** Total bytes received. */
  public void setReceivedBytes(Double receivedBytes) {
    this.receivedBytes = receivedBytes;
  }

  /** Download status. */
  public DownloadProgressState getState() {
    return state;
  }

  /** Download status. */
  public void setState(DownloadProgressState state) {
    this.state = state;
  }
}
