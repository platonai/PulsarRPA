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

import com.github.kklisura.cdt.protocol.support.annotations.Optional;
import java.util.List;

public class AppManifest {

  private String url;

  private List<AppManifestError> errors;

  @Optional private String data;

  /** Manifest location. */
  public String getUrl() {
    return url;
  }

  /** Manifest location. */
  public void setUrl(String url) {
    this.url = url;
  }

  public List<AppManifestError> getErrors() {
    return errors;
  }

  public void setErrors(List<AppManifestError> errors) {
    this.errors = errors;
  }

  /** Manifest content. */
  public String getData() {
    return data;
  }

  /** Manifest content. */
  public void setData(String data) {
    this.data = data;
  }
}
