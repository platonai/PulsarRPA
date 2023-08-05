package com.github.kklisura.cdt.protocol.v2023.types.storage;

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

import java.util.List;

/** Bundles a candidate URL with its reporting metadata. */
public class SharedStorageUrlWithMetadata {

  private String url;

  private List<SharedStorageReportingMetadata> reportingMetadata;

  /** Spec of candidate URL. */
  public String getUrl() {
    return url;
  }

  /** Spec of candidate URL. */
  public void setUrl(String url) {
    this.url = url;
  }

  /** Any associated reporting metadata. */
  public List<SharedStorageReportingMetadata> getReportingMetadata() {
    return reportingMetadata;
  }

  /** Any associated reporting metadata. */
  public void setReportingMetadata(List<SharedStorageReportingMetadata> reportingMetadata) {
    this.reportingMetadata = reportingMetadata;
  }
}
