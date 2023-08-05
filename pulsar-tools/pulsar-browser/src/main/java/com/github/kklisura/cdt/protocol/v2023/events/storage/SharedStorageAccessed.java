package com.github.kklisura.cdt.protocol.v2023.events.storage;

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

import com.github.kklisura.cdt.protocol.v2023.types.storage.SharedStorageAccessParams;
import com.github.kklisura.cdt.protocol.v2023.types.storage.SharedStorageAccessType;

/**
 * Shared storage was accessed by the associated page. The following parameters are included in all
 * events.
 */
public class SharedStorageAccessed {

  private Double accessTime;

  private SharedStorageAccessType type;

  private String mainFrameId;

  private String ownerOrigin;

  private SharedStorageAccessParams params;

  /** Time of the access. */
  public Double getAccessTime() {
    return accessTime;
  }

  /** Time of the access. */
  public void setAccessTime(Double accessTime) {
    this.accessTime = accessTime;
  }

  /** Enum value indicating the Shared Storage API method invoked. */
  public SharedStorageAccessType getType() {
    return type;
  }

  /** Enum value indicating the Shared Storage API method invoked. */
  public void setType(SharedStorageAccessType type) {
    this.type = type;
  }

  /** DevTools Frame Token for the primary frame tree's root. */
  public String getMainFrameId() {
    return mainFrameId;
  }

  /** DevTools Frame Token for the primary frame tree's root. */
  public void setMainFrameId(String mainFrameId) {
    this.mainFrameId = mainFrameId;
  }

  /** Serialized origin for the context that invoked the Shared Storage API. */
  public String getOwnerOrigin() {
    return ownerOrigin;
  }

  /** Serialized origin for the context that invoked the Shared Storage API. */
  public void setOwnerOrigin(String ownerOrigin) {
    this.ownerOrigin = ownerOrigin;
  }

  /**
   * The sub-parameters warapped by `params` are all optional and their presence/absence depends on
   * `type`.
   */
  public SharedStorageAccessParams getParams() {
    return params;
  }

  /**
   * The sub-parameters warapped by `params` are all optional and their presence/absence depends on
   * `type`.
   */
  public void setParams(SharedStorageAccessParams params) {
    this.params = params;
  }
}
