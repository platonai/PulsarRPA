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
import com.github.kklisura.cdt.protocol.v2023.support.annotations.Optional;

/**
 * Issued when detached from target for any reason (including `detachFromTarget` command). Can be
 * issued multiple times per target if multiple sessions have been attached to it.
 */
@Experimental
public class DetachedFromTarget {

  private String sessionId;

  @Deprecated @Optional
  private String targetId;

  /** Detached session identifier. */
  public String getSessionId() {
    return sessionId;
  }

  /** Detached session identifier. */
  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
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
