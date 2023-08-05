package com.github.kklisura.cdt.protocol.v2023.events.target;

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
import com.github.kklisura.cdt.protocol.v2023.types.target.TargetInfo;

/** Issued when attached to target because of auto-attach or `attachToTarget` command. */
@Experimental
public class AttachedToTarget {

  private String sessionId;

  private TargetInfo targetInfo;

  private Boolean waitingForDebugger;

  /** Identifier assigned to the session used to send/receive messages. */
  public String getSessionId() {
    return sessionId;
  }

  /** Identifier assigned to the session used to send/receive messages. */
  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public TargetInfo getTargetInfo() {
    return targetInfo;
  }

  public void setTargetInfo(TargetInfo targetInfo) {
    this.targetInfo = targetInfo;
  }

  public Boolean getWaitingForDebugger() {
    return waitingForDebugger;
  }

  public void setWaitingForDebugger(Boolean waitingForDebugger) {
    this.waitingForDebugger = waitingForDebugger;
  }
}
