package com.github.kklisura.cdt.protocol.v2023.events.network;

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

/** Fired once when parsing the .wbn file has failed. */
@Experimental
public class SubresourceWebBundleMetadataError {

  private String requestId;

  private String errorMessage;

  /** Request identifier. Used to match this information to another event. */
  public String getRequestId() {
    return requestId;
  }

  /** Request identifier. Used to match this information to another event. */
  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  /** Error message */
  public String getErrorMessage() {
    return errorMessage;
  }

  /** Error message */
  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }
}
