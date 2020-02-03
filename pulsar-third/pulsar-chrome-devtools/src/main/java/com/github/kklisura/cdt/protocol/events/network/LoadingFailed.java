package com.github.kklisura.cdt.protocol.events.network;

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

import com.github.kklisura.cdt.protocol.support.annotations.Optional;
import com.github.kklisura.cdt.protocol.types.network.BlockedReason;
import com.github.kklisura.cdt.protocol.types.network.ResourceType;

/** Fired when HTTP request has failed to load. */
public class LoadingFailed {

  private String requestId;

  private Double timestamp;

  private ResourceType type;

  private String errorText;

  @Optional private Boolean canceled;

  @Optional private BlockedReason blockedReason;

  /** Request identifier. */
  public String getRequestId() {
    return requestId;
  }

  /** Request identifier. */
  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  /** Timestamp. */
  public Double getTimestamp() {
    return timestamp;
  }

  /** Timestamp. */
  public void setTimestamp(Double timestamp) {
    this.timestamp = timestamp;
  }

  /** Resource type. */
  public ResourceType getType() {
    return type;
  }

  /** Resource type. */
  public void setType(ResourceType type) {
    this.type = type;
  }

  /** User friendly error message. */
  public String getErrorText() {
    return errorText;
  }

  /** User friendly error message. */
  public void setErrorText(String errorText) {
    this.errorText = errorText;
  }

  /** True if loading was canceled. */
  public Boolean getCanceled() {
    return canceled;
  }

  /** True if loading was canceled. */
  public void setCanceled(Boolean canceled) {
    this.canceled = canceled;
  }

  /** The reason why loading was blocked, if any. */
  public BlockedReason getBlockedReason() {
    return blockedReason;
  }

  /** The reason why loading was blocked, if any. */
  public void setBlockedReason(BlockedReason blockedReason) {
    this.blockedReason = blockedReason;
  }
}
