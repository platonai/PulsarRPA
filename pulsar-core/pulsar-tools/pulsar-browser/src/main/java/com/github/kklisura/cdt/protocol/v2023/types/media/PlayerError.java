package com.github.kklisura.cdt.protocol.v2023.types.media;

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

import java.util.List;
import java.util.Map;

/** Corresponds to kMediaError */
public class PlayerError {

  private String errorType;

  private Integer code;

  private List<PlayerErrorSourceLocation> stack;

  private List<PlayerError> cause;

  private Map<String, Object> data;

  public String getErrorType() {
    return errorType;
  }

  public void setErrorType(String errorType) {
    this.errorType = errorType;
  }

  /**
   * Code is the numeric enum entry for a specific set of error codes, such as PipelineStatusCodes
   * in media/base/pipeline_status.h
   */
  public Integer getCode() {
    return code;
  }

  /**
   * Code is the numeric enum entry for a specific set of error codes, such as PipelineStatusCodes
   * in media/base/pipeline_status.h
   */
  public void setCode(Integer code) {
    this.code = code;
  }

  /** A trace of where this error was caused / where it passed through. */
  public List<PlayerErrorSourceLocation> getStack() {
    return stack;
  }

  /** A trace of where this error was caused / where it passed through. */
  public void setStack(List<PlayerErrorSourceLocation> stack) {
    this.stack = stack;
  }

  /**
   * Errors potentially have a root cause error, ie, a DecoderError might be caused by an
   * WindowsError
   */
  public List<PlayerError> getCause() {
    return cause;
  }

  /**
   * Errors potentially have a root cause error, ie, a DecoderError might be caused by an
   * WindowsError
   */
  public void setCause(List<PlayerError> cause) {
    this.cause = cause;
  }

  /** Extra data attached to an error, such as an HRESULT, Video Codec, etc. */
  public Map<String, Object> getData() {
    return data;
  }

  /** Extra data attached to an error, such as an HRESULT, Video Codec, etc. */
  public void setData(Map<String, Object> data) {
    this.data = data;
  }
}
