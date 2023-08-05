package com.github.kklisura.cdt.protocol.v2023.types.page;

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

public class PrintToPDF {

  private String data;

  @Experimental
  @Optional
  private String stream;

  /**
   * Base64-encoded pdf data. Empty if |returnAsStream| is specified. (Encoded as a base64 string
   * when passed over JSON)
   */
  public String getData() {
    return data;
  }

  /**
   * Base64-encoded pdf data. Empty if |returnAsStream| is specified. (Encoded as a base64 string
   * when passed over JSON)
   */
  public void setData(String data) {
    this.data = data;
  }

  /** A handle of the stream that holds resulting PDF data. */
  public String getStream() {
    return stream;
  }

  /** A handle of the stream that holds resulting PDF data. */
  public void setStream(String stream) {
    this.stream = stream;
  }
}
