package com.github.kklisura.cdt.protocol.types.page;

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

/** Error while paring app manifest. */
public class AppManifestError {

  private String message;

  private Integer critical;

  private Integer line;

  private Integer column;

  /** Error message. */
  public String getMessage() {
    return message;
  }

  /** Error message. */
  public void setMessage(String message) {
    this.message = message;
  }

  /** If criticial, this is a non-recoverable parse error. */
  public Integer getCritical() {
    return critical;
  }

  /** If criticial, this is a non-recoverable parse error. */
  public void setCritical(Integer critical) {
    this.critical = critical;
  }

  /** Error line. */
  public Integer getLine() {
    return line;
  }

  /** Error line. */
  public void setLine(Integer line) {
    this.line = line;
  }

  /** Error column. */
  public Integer getColumn() {
    return column;
  }

  /** Error column. */
  public void setColumn(Integer column) {
    this.column = column;
  }
}
