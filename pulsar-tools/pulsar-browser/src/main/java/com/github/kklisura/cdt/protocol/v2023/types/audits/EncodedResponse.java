package com.github.kklisura.cdt.protocol.v2023.types.audits;

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

import com.github.kklisura.cdt.protocol.v2023.support.annotations.Optional;

public class EncodedResponse {

  @Optional
  private String body;

  private Integer originalSize;

  private Integer encodedSize;

  /**
   * The encoded body as a base64 string. Omitted if sizeOnly is true. (Encoded as a base64 string
   * when passed over JSON)
   */
  public String getBody() {
    return body;
  }

  /**
   * The encoded body as a base64 string. Omitted if sizeOnly is true. (Encoded as a base64 string
   * when passed over JSON)
   */
  public void setBody(String body) {
    this.body = body;
  }

  /** Size before re-encoding. */
  public Integer getOriginalSize() {
    return originalSize;
  }

  /** Size before re-encoding. */
  public void setOriginalSize(Integer originalSize) {
    this.originalSize = originalSize;
  }

  /** Size after re-encoding. */
  public Integer getEncodedSize() {
    return encodedSize;
  }

  /** Size after re-encoding. */
  public void setEncodedSize(Integer encodedSize) {
    this.encodedSize = encodedSize;
  }
}
