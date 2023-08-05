package com.github.kklisura.cdt.protocol.v2023.types.dom;

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

public class PerformSearch {

  private String searchId;

  private Integer resultCount;

  /** Unique search session identifier. */
  public String getSearchId() {
    return searchId;
  }

  /** Unique search session identifier. */
  public void setSearchId(String searchId) {
    this.searchId = searchId;
  }

  /** Number of search results. */
  public Integer getResultCount() {
    return resultCount;
  }

  /** Number of search results. */
  public void setResultCount(Integer resultCount) {
    this.resultCount = resultCount;
  }
}
