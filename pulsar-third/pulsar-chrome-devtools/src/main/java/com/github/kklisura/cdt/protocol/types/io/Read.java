package com.github.kklisura.cdt.protocol.types.io;

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

public class Read {

  @Optional private Boolean base64Encoded;

  private String data;

  private Boolean eof;

  /** Set if the data is base64-encoded */
  public Boolean getBase64Encoded() {
    return base64Encoded;
  }

  /** Set if the data is base64-encoded */
  public void setBase64Encoded(Boolean base64Encoded) {
    this.base64Encoded = base64Encoded;
  }

  /** Data that were read. */
  public String getData() {
    return data;
  }

  /** Data that were read. */
  public void setData(String data) {
    this.data = data;
  }

  /** Set if the end-of-file condition occured while reading. */
  public Boolean getEof() {
    return eof;
  }

  /** Set if the end-of-file condition occured while reading. */
  public void setEof(Boolean eof) {
    this.eof = eof;
  }
}
