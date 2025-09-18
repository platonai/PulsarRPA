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

public class HeavyAdIssueDetails {

  private HeavyAdResolutionStatus resolution;

  private HeavyAdReason reason;

  private AffectedFrame frame;

  /** The resolution status, either blocking the content or warning. */
  public HeavyAdResolutionStatus getResolution() {
    return resolution;
  }

  /** The resolution status, either blocking the content or warning. */
  public void setResolution(HeavyAdResolutionStatus resolution) {
    this.resolution = resolution;
  }

  /** The reason the ad was blocked, total network or cpu or peak cpu. */
  public HeavyAdReason getReason() {
    return reason;
  }

  /** The reason the ad was blocked, total network or cpu or peak cpu. */
  public void setReason(HeavyAdReason reason) {
    this.reason = reason;
  }

  /** The frame that was blocked. */
  public AffectedFrame getFrame() {
    return frame;
  }

  /** The frame that was blocked. */
  public void setFrame(AffectedFrame frame) {
    this.frame = frame;
  }
}
