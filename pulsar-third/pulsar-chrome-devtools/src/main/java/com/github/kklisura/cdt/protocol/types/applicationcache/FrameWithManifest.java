package com.github.kklisura.cdt.protocol.types.applicationcache;

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

/** Frame identifier - manifest URL pair. */
public class FrameWithManifest {

  private String frameId;

  private String manifestURL;

  private Integer status;

  /** Frame identifier. */
  public String getFrameId() {
    return frameId;
  }

  /** Frame identifier. */
  public void setFrameId(String frameId) {
    this.frameId = frameId;
  }

  /** Manifest URL. */
  public String getManifestURL() {
    return manifestURL;
  }

  /** Manifest URL. */
  public void setManifestURL(String manifestURL) {
    this.manifestURL = manifestURL;
  }

  /** Application cache status. */
  public Integer getStatus() {
    return status;
  }

  /** Application cache status. */
  public void setStatus(Integer status) {
    this.status = status;
  }
}
