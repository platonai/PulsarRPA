package com.github.kklisura.cdt.protocol.types.debugger;

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

/** Location in the source code. */
public class Location {

  private String scriptId;

  private Integer lineNumber;

  @Optional private Integer columnNumber;

  /** Script identifier as reported in the `Debugger.scriptParsed`. */
  public String getScriptId() {
    return scriptId;
  }

  /** Script identifier as reported in the `Debugger.scriptParsed`. */
  public void setScriptId(String scriptId) {
    this.scriptId = scriptId;
  }

  /** Line number in the script (0-based). */
  public Integer getLineNumber() {
    return lineNumber;
  }

  /** Line number in the script (0-based). */
  public void setLineNumber(Integer lineNumber) {
    this.lineNumber = lineNumber;
  }

  /** Column number in the script (0-based). */
  public Integer getColumnNumber() {
    return columnNumber;
  }

  /** Column number in the script (0-based). */
  public void setColumnNumber(Integer columnNumber) {
    this.columnNumber = columnNumber;
  }
}
