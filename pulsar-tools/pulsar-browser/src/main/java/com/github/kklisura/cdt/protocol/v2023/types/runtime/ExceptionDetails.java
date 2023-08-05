package com.github.kklisura.cdt.protocol.v2023.types.runtime;

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

import java.util.Map;

/**
 * Detailed information about exception (or error) that was thrown during script compilation or
 * execution.
 */
public class ExceptionDetails {

  private Integer exceptionId;

  private String text;

  private Integer lineNumber;

  private Integer columnNumber;

  @Optional
  private String scriptId;

  @Optional private String url;

  @Optional private StackTrace stackTrace;

  @Optional private RemoteObject exception;

  @Optional private Integer executionContextId;

  @Experimental
  @Optional private Map<String, Object> exceptionMetaData;

  /** Exception id. */
  public Integer getExceptionId() {
    return exceptionId;
  }

  /** Exception id. */
  public void setExceptionId(Integer exceptionId) {
    this.exceptionId = exceptionId;
  }

  /** Exception text, which should be used together with exception object when available. */
  public String getText() {
    return text;
  }

  /** Exception text, which should be used together with exception object when available. */
  public void setText(String text) {
    this.text = text;
  }

  /** Line number of the exception location (0-based). */
  public Integer getLineNumber() {
    return lineNumber;
  }

  /** Line number of the exception location (0-based). */
  public void setLineNumber(Integer lineNumber) {
    this.lineNumber = lineNumber;
  }

  /** Column number of the exception location (0-based). */
  public Integer getColumnNumber() {
    return columnNumber;
  }

  /** Column number of the exception location (0-based). */
  public void setColumnNumber(Integer columnNumber) {
    this.columnNumber = columnNumber;
  }

  /** Script ID of the exception location. */
  public String getScriptId() {
    return scriptId;
  }

  /** Script ID of the exception location. */
  public void setScriptId(String scriptId) {
    this.scriptId = scriptId;
  }

  /** URL of the exception location, to be used when the script was not reported. */
  public String getUrl() {
    return url;
  }

  /** URL of the exception location, to be used when the script was not reported. */
  public void setUrl(String url) {
    this.url = url;
  }

  /** JavaScript stack trace if available. */
  public StackTrace getStackTrace() {
    return stackTrace;
  }

  /** JavaScript stack trace if available. */
  public void setStackTrace(StackTrace stackTrace) {
    this.stackTrace = stackTrace;
  }

  /** Exception object if available. */
  public RemoteObject getException() {
    return exception;
  }

  /** Exception object if available. */
  public void setException(RemoteObject exception) {
    this.exception = exception;
  }

  /** Identifier of the context where exception happened. */
  public Integer getExecutionContextId() {
    return executionContextId;
  }

  /** Identifier of the context where exception happened. */
  public void setExecutionContextId(Integer executionContextId) {
    this.executionContextId = executionContextId;
  }

  /**
   * Dictionary with entries of meta data that the client associated with this exception, such as
   * information about associated network requests, etc.
   */
  public Map<String, Object> getExceptionMetaData() {
    return exceptionMetaData;
  }

  /**
   * Dictionary with entries of meta data that the client associated with this exception, such as
   * information about associated network requests, etc.
   */
  public void setExceptionMetaData(Map<String, Object> exceptionMetaData) {
    this.exceptionMetaData = exceptionMetaData;
  }
}
