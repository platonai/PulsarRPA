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

/** Fired for top level page lifecycle events such as navigation, load, paint, etc. */
public class LifecycleEvent {

  private String frameId;

  private String loaderId;

  private String name;

  private Double timestamp;

  /** Id of the frame. */
  public String getFrameId() {
    return frameId;
  }

  /** Id of the frame. */
  public void setFrameId(String frameId) {
    this.frameId = frameId;
  }

  /** Loader identifier. Empty string if the request is fetched from worker. */
  public String getLoaderId() {
    return loaderId;
  }

  /** Loader identifier. Empty string if the request is fetched from worker. */
  public void setLoaderId(String loaderId) {
    this.loaderId = loaderId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Double getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Double timestamp) {
    this.timestamp = timestamp;
  }
}
