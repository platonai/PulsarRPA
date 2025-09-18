package com.github.kklisura.cdt.protocol.v2023.events.runtime;

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
import com.github.kklisura.cdt.protocol.v2023.types.runtime.RemoteObject;

import java.util.Map;

/**
 * Issued when object should be inspected (for example, as a result of inspect() command line API
 * call).
 */
public class InspectRequested {

  private RemoteObject object;

  private Map<String, Object> hints;

  @Experimental
  @Optional
  private Integer executionContextId;

  public RemoteObject getObject() {
    return object;
  }

  public void setObject(RemoteObject object) {
    this.object = object;
  }

  public Map<String, Object> getHints() {
    return hints;
  }

  public void setHints(Map<String, Object> hints) {
    this.hints = hints;
  }

  /** Identifier of the context where the call was made. */
  public Integer getExecutionContextId() {
    return executionContextId;
  }

  /** Identifier of the context where the call was made. */
  public void setExecutionContextId(Integer executionContextId) {
    this.executionContextId = executionContextId;
  }
}
