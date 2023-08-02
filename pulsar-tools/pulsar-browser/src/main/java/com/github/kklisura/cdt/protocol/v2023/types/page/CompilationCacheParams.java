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

/** Per-script compilation cache parameters for `Page.produceCompilationCache` */
@Experimental
public class CompilationCacheParams {

  private String url;

  @Optional
  private Boolean eager;

  /** The URL of the script to produce a compilation cache entry for. */
  public String getUrl() {
    return url;
  }

  /** The URL of the script to produce a compilation cache entry for. */
  public void setUrl(String url) {
    this.url = url;
  }

  /**
   * A hint to the backend whether eager compilation is recommended. (the actual compilation mode
   * used is upon backend discretion).
   */
  public Boolean getEager() {
    return eager;
  }

  /**
   * A hint to the backend whether eager compilation is recommended. (the actual compilation mode
   * used is upon backend discretion).
   */
  public void setEager(Boolean eager) {
    this.eager = eager;
  }
}
