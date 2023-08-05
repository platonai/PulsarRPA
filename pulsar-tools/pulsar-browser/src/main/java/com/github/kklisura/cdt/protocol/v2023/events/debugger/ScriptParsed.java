package com.github.kklisura.cdt.protocol.v2023.events.debugger;

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
import com.github.kklisura.cdt.protocol.v2023.types.debugger.DebugSymbols;
import com.github.kklisura.cdt.protocol.v2023.types.debugger.ScriptLanguage;
import com.github.kklisura.cdt.protocol.v2023.types.runtime.StackTrace;

import java.util.Map;

/**
 * Fired when virtual machine parses script. This event is also fired for all known and uncollected
 * scripts upon enabling debugger.
 */
public class ScriptParsed {

  private String scriptId;

  private String url;

  private Integer startLine;

  private Integer startColumn;

  private Integer endLine;

  private Integer endColumn;

  private Integer executionContextId;

  private String hash;

  @Optional
  private Map<String, Object> executionContextAuxData;

  @Experimental
  @Optional private Boolean isLiveEdit;

  @Optional private String sourceMapURL;

  @Optional private Boolean hasSourceURL;

  @Optional private Boolean isModule;

  @Optional private Integer length;

  @Experimental @Optional private StackTrace stackTrace;

  @Experimental @Optional private Integer codeOffset;

  @Experimental @Optional private ScriptLanguage scriptLanguage;

  @Experimental @Optional private DebugSymbols debugSymbols;

  @Experimental @Optional private String embedderName;

  /** Identifier of the script parsed. */
  public String getScriptId() {
    return scriptId;
  }

  /** Identifier of the script parsed. */
  public void setScriptId(String scriptId) {
    this.scriptId = scriptId;
  }

  /** URL or name of the script parsed (if any). */
  public String getUrl() {
    return url;
  }

  /** URL or name of the script parsed (if any). */
  public void setUrl(String url) {
    this.url = url;
  }

  /** Line offset of the script within the resource with given URL (for script tags). */
  public Integer getStartLine() {
    return startLine;
  }

  /** Line offset of the script within the resource with given URL (for script tags). */
  public void setStartLine(Integer startLine) {
    this.startLine = startLine;
  }

  /** Column offset of the script within the resource with given URL. */
  public Integer getStartColumn() {
    return startColumn;
  }

  /** Column offset of the script within the resource with given URL. */
  public void setStartColumn(Integer startColumn) {
    this.startColumn = startColumn;
  }

  /** Last line of the script. */
  public Integer getEndLine() {
    return endLine;
  }

  /** Last line of the script. */
  public void setEndLine(Integer endLine) {
    this.endLine = endLine;
  }

  /** Length of the last line of the script. */
  public Integer getEndColumn() {
    return endColumn;
  }

  /** Length of the last line of the script. */
  public void setEndColumn(Integer endColumn) {
    this.endColumn = endColumn;
  }

  /** Specifies script creation context. */
  public Integer getExecutionContextId() {
    return executionContextId;
  }

  /** Specifies script creation context. */
  public void setExecutionContextId(Integer executionContextId) {
    this.executionContextId = executionContextId;
  }

  /** Content hash of the script, SHA-256. */
  public String getHash() {
    return hash;
  }

  /** Content hash of the script, SHA-256. */
  public void setHash(String hash) {
    this.hash = hash;
  }

  /**
   * Embedder-specific auxiliary data likely matching {isDefault: boolean, type:
   * 'default'|'isolated'|'worker', frameId: string}
   */
  public Map<String, Object> getExecutionContextAuxData() {
    return executionContextAuxData;
  }

  /**
   * Embedder-specific auxiliary data likely matching {isDefault: boolean, type:
   * 'default'|'isolated'|'worker', frameId: string}
   */
  public void setExecutionContextAuxData(Map<String, Object> executionContextAuxData) {
    this.executionContextAuxData = executionContextAuxData;
  }

  /** True, if this script is generated as a result of the live edit operation. */
  public Boolean getIsLiveEdit() {
    return isLiveEdit;
  }

  /** True, if this script is generated as a result of the live edit operation. */
  public void setIsLiveEdit(Boolean isLiveEdit) {
    this.isLiveEdit = isLiveEdit;
  }

  /** URL of source map associated with script (if any). */
  public String getSourceMapURL() {
    return sourceMapURL;
  }

  /** URL of source map associated with script (if any). */
  public void setSourceMapURL(String sourceMapURL) {
    this.sourceMapURL = sourceMapURL;
  }

  /** True, if this script has sourceURL. */
  public Boolean getHasSourceURL() {
    return hasSourceURL;
  }

  /** True, if this script has sourceURL. */
  public void setHasSourceURL(Boolean hasSourceURL) {
    this.hasSourceURL = hasSourceURL;
  }

  /** True, if this script is ES6 module. */
  public Boolean getIsModule() {
    return isModule;
  }

  /** True, if this script is ES6 module. */
  public void setIsModule(Boolean isModule) {
    this.isModule = isModule;
  }

  /** This script length. */
  public Integer getLength() {
    return length;
  }

  /** This script length. */
  public void setLength(Integer length) {
    this.length = length;
  }

  /** JavaScript top stack frame of where the script parsed event was triggered if available. */
  public StackTrace getStackTrace() {
    return stackTrace;
  }

  /** JavaScript top stack frame of where the script parsed event was triggered if available. */
  public void setStackTrace(StackTrace stackTrace) {
    this.stackTrace = stackTrace;
  }

  /** If the scriptLanguage is WebAssembly, the code section offset in the module. */
  public Integer getCodeOffset() {
    return codeOffset;
  }

  /** If the scriptLanguage is WebAssembly, the code section offset in the module. */
  public void setCodeOffset(Integer codeOffset) {
    this.codeOffset = codeOffset;
  }

  /** The language of the script. */
  public ScriptLanguage getScriptLanguage() {
    return scriptLanguage;
  }

  /** The language of the script. */
  public void setScriptLanguage(ScriptLanguage scriptLanguage) {
    this.scriptLanguage = scriptLanguage;
  }

  /** If the scriptLanguage is WebASsembly, the source of debug symbols for the module. */
  public DebugSymbols getDebugSymbols() {
    return debugSymbols;
  }

  /** If the scriptLanguage is WebASsembly, the source of debug symbols for the module. */
  public void setDebugSymbols(DebugSymbols debugSymbols) {
    this.debugSymbols = debugSymbols;
  }

  /** The name the embedder supplied for this script. */
  public String getEmbedderName() {
    return embedderName;
  }

  /** The name the embedder supplied for this script. */
  public void setEmbedderName(String embedderName) {
    this.embedderName = embedderName;
  }
}
