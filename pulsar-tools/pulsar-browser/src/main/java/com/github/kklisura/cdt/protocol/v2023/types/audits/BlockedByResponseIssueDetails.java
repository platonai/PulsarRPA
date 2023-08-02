package com.github.kklisura.cdt.protocol.v2023.types.audits;

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

import com.github.kklisura.cdt.protocol.v2023.support.annotations.Optional;

/**
 * Details for a request that has been blocked with the BLOCKED_BY_RESPONSE code. Currently only
 * used for COEP/COOP, but may be extended to include some CSP errors in the future.
 */
public class BlockedByResponseIssueDetails {

  private AffectedRequest request;

  @Optional
  private AffectedFrame parentFrame;

  @Optional private AffectedFrame blockedFrame;

  private BlockedByResponseReason reason;

  public AffectedRequest getRequest() {
    return request;
  }

  public void setRequest(AffectedRequest request) {
    this.request = request;
  }

  public AffectedFrame getParentFrame() {
    return parentFrame;
  }

  public void setParentFrame(AffectedFrame parentFrame) {
    this.parentFrame = parentFrame;
  }

  public AffectedFrame getBlockedFrame() {
    return blockedFrame;
  }

  public void setBlockedFrame(AffectedFrame blockedFrame) {
    this.blockedFrame = blockedFrame;
  }

  public BlockedByResponseReason getReason() {
    return reason;
  }

  public void setReason(BlockedByResponseReason reason) {
    this.reason = reason;
  }
}
