package com.github.kklisura.cdt.protocol.events.runtime;

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

import com.github.kklisura.cdt.protocol.support.annotations.Experimental;
import com.github.kklisura.cdt.protocol.support.annotations.Optional;
import com.github.kklisura.cdt.protocol.types.runtime.RemoteObject;
import com.github.kklisura.cdt.protocol.types.runtime.StackTrace;
import java.util.List;

/** Issued when console API was called. */
public class ConsoleAPICalled {

  private ConsoleAPICalledType type;

  private List<RemoteObject> args;

  private Integer executionContextId;

  private Double timestamp;

  @Optional private StackTrace stackTrace;

  @Experimental @Optional private String context;

  /** Type of the call. */
  public ConsoleAPICalledType getType() {
    return type;
  }

  /** Type of the call. */
  public void setType(ConsoleAPICalledType type) {
    this.type = type;
  }

  /** Call arguments. */
  public List<RemoteObject> getArgs() {
    return args;
  }

  /** Call arguments. */
  public void setArgs(List<RemoteObject> args) {
    this.args = args;
  }

  /** Identifier of the context where the call was made. */
  public Integer getExecutionContextId() {
    return executionContextId;
  }

  /** Identifier of the context where the call was made. */
  public void setExecutionContextId(Integer executionContextId) {
    this.executionContextId = executionContextId;
  }

  /** Call timestamp. */
  public Double getTimestamp() {
    return timestamp;
  }

  /** Call timestamp. */
  public void setTimestamp(Double timestamp) {
    this.timestamp = timestamp;
  }

  /**
   * Stack trace captured when the call was made. The async stack chain is automatically reported
   * for the following call types: `assert`, `error`, `trace`, `warning`. For other types the async
   * call chain can be retrieved using `Debugger.getStackTrace` and `stackTrace.parentId` field.
   */
  public StackTrace getStackTrace() {
    return stackTrace;
  }

  /**
   * Stack trace captured when the call was made. The async stack chain is automatically reported
   * for the following call types: `assert`, `error`, `trace`, `warning`. For other types the async
   * call chain can be retrieved using `Debugger.getStackTrace` and `stackTrace.parentId` field.
   */
  public void setStackTrace(StackTrace stackTrace) {
    this.stackTrace = stackTrace;
  }

  /**
   * Console context descriptor for calls on non-default console context (not console.*):
   * 'anonymous#unique-logger-id' for call on unnamed context, 'name#unique-logger-id' for call on
   * named context.
   */
  public String getContext() {
    return context;
  }

  /**
   * Console context descriptor for calls on non-default console context (not console.*):
   * 'anonymous#unique-logger-id' for call on unnamed context, 'name#unique-logger-id' for call on
   * named context.
   */
  public void setContext(String context) {
    this.context = context;
  }
}
