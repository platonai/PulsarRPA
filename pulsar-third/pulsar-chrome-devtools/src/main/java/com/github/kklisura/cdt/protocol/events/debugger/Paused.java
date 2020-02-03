package com.github.kklisura.cdt.protocol.events.debugger;

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
import com.github.kklisura.cdt.protocol.types.debugger.CallFrame;
import com.github.kklisura.cdt.protocol.types.runtime.StackTrace;
import com.github.kklisura.cdt.protocol.types.runtime.StackTraceId;
import java.util.List;
import java.util.Map;

/** Fired when the virtual machine stopped on breakpoint or exception or any other stop criteria. */
public class Paused {

  private List<CallFrame> callFrames;

  private PausedReason reason;

  @Optional private Map<String, Object> data;

  @Optional private List<String> hitBreakpoints;

  @Optional private StackTrace asyncStackTrace;

  @Experimental @Optional private StackTraceId asyncStackTraceId;

  @Experimental @Optional private StackTraceId asyncCallStackTraceId;

  /** Call stack the virtual machine stopped on. */
  public List<CallFrame> getCallFrames() {
    return callFrames;
  }

  /** Call stack the virtual machine stopped on. */
  public void setCallFrames(List<CallFrame> callFrames) {
    this.callFrames = callFrames;
  }

  /** Pause reason. */
  public PausedReason getReason() {
    return reason;
  }

  /** Pause reason. */
  public void setReason(PausedReason reason) {
    this.reason = reason;
  }

  /** Object containing break-specific auxiliary properties. */
  public Map<String, Object> getData() {
    return data;
  }

  /** Object containing break-specific auxiliary properties. */
  public void setData(Map<String, Object> data) {
    this.data = data;
  }

  /** Hit breakpoints IDs */
  public List<String> getHitBreakpoints() {
    return hitBreakpoints;
  }

  /** Hit breakpoints IDs */
  public void setHitBreakpoints(List<String> hitBreakpoints) {
    this.hitBreakpoints = hitBreakpoints;
  }

  /** Async stack trace, if any. */
  public StackTrace getAsyncStackTrace() {
    return asyncStackTrace;
  }

  /** Async stack trace, if any. */
  public void setAsyncStackTrace(StackTrace asyncStackTrace) {
    this.asyncStackTrace = asyncStackTrace;
  }

  /** Async stack trace, if any. */
  public StackTraceId getAsyncStackTraceId() {
    return asyncStackTraceId;
  }

  /** Async stack trace, if any. */
  public void setAsyncStackTraceId(StackTraceId asyncStackTraceId) {
    this.asyncStackTraceId = asyncStackTraceId;
  }

  /**
   * Just scheduled async call will have this stack trace as parent stack during async execution.
   * This field is available only after `Debugger.stepInto` call with `breakOnAsynCall` flag.
   */
  public StackTraceId getAsyncCallStackTraceId() {
    return asyncCallStackTraceId;
  }

  /**
   * Just scheduled async call will have this stack trace as parent stack during async execution.
   * This field is available only after `Debugger.stepInto` call with `breakOnAsynCall` flag.
   */
  public void setAsyncCallStackTraceId(StackTraceId asyncCallStackTraceId) {
    this.asyncCallStackTraceId = asyncCallStackTraceId;
  }
}
