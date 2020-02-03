package com.github.kklisura.cdt.protocol.types.runtime;

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

@Experimental
public class CustomPreview {

  private String header;

  @Optional private String bodyGetterId;

  /**
   * The JSON-stringified result of formatter.header(object, config) call. It contains json ML array
   * that represents RemoteObject.
   */
  public String getHeader() {
    return header;
  }

  /**
   * The JSON-stringified result of formatter.header(object, config) call. It contains json ML array
   * that represents RemoteObject.
   */
  public void setHeader(String header) {
    this.header = header;
  }

  /**
   * If formatter returns true as a result of formatter.hasBody call then bodyGetterId will contain
   * RemoteObjectId for the function that returns result of formatter.body(object, config) call. The
   * result value is json ML array.
   */
  public String getBodyGetterId() {
    return bodyGetterId;
  }

  /**
   * If formatter returns true as a result of formatter.hasBody call then bodyGetterId will contain
   * RemoteObjectId for the function that returns result of formatter.body(object, config) call. The
   * result value is json ML array.
   */
  public void setBodyGetterId(String bodyGetterId) {
    this.bodyGetterId = bodyGetterId;
  }
}
