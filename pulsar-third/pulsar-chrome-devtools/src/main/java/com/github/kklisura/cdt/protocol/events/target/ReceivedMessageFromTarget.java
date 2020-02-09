package com.github.kklisura.cdt.protocol.events.target;

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

/**
 * Notifies about a new protocol message received from the session (as reported in
 * `attachedToTarget` event).
 */
public class ReceivedMessageFromTarget {

  private String sessionId;

  private String message;

  @Deprecated @Optional private String targetId;

  /** Identifier of a session which sends a message. */
  public String getSessionId() {
    return sessionId;
  }

  /** Identifier of a session which sends a message. */
  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  /** Deprecated. */
  public String getTargetId() {
    return targetId;
  }

  /** Deprecated. */
  public void setTargetId(String targetId) {
    this.targetId = targetId;
  }
}
