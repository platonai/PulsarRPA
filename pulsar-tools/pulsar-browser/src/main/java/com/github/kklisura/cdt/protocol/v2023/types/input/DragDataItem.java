package com.github.kklisura.cdt.protocol.v2023.types.input;

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

@Experimental
public class DragDataItem {

  private String mimeType;

  private String data;

  @Optional
  private String title;

  @Optional private String baseURL;

  /** Mime type of the dragged data. */
  public String getMimeType() {
    return mimeType;
  }

  /** Mime type of the dragged data. */
  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }

  /**
   * Depending of the value of `mimeType`, it contains the dragged link, text, HTML markup or any
   * other data.
   */
  public String getData() {
    return data;
  }

  /**
   * Depending of the value of `mimeType`, it contains the dragged link, text, HTML markup or any
   * other data.
   */
  public void setData(String data) {
    this.data = data;
  }

  /** Title associated with a link. Only valid when `mimeType` == "text/uri-list". */
  public String getTitle() {
    return title;
  }

  /** Title associated with a link. Only valid when `mimeType` == "text/uri-list". */
  public void setTitle(String title) {
    this.title = title;
  }

  /** Stores the base URL for the contained markup. Only valid when `mimeType` == "text/html". */
  public String getBaseURL() {
    return baseURL;
  }

  /** Stores the base URL for the contained markup. Only valid when `mimeType` == "text/html". */
  public void setBaseURL(String baseURL) {
    this.baseURL = baseURL;
  }
}
