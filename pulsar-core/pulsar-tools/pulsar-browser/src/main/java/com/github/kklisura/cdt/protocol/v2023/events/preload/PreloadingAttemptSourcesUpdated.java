package com.github.kklisura.cdt.protocol.v2023.events.preload;

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

import com.github.kklisura.cdt.protocol.v2023.types.preload.PreloadingAttemptSource;

import java.util.List;

/** Send a list of sources for all preloading attempts in a document. */
public class PreloadingAttemptSourcesUpdated {

  private String loaderId;

  private List<PreloadingAttemptSource> preloadingAttemptSources;

  public String getLoaderId() {
    return loaderId;
  }

  public void setLoaderId(String loaderId) {
    this.loaderId = loaderId;
  }

  public List<PreloadingAttemptSource> getPreloadingAttemptSources() {
    return preloadingAttemptSources;
  }

  public void setPreloadingAttemptSources(List<PreloadingAttemptSource> preloadingAttemptSources) {
    this.preloadingAttemptSources = preloadingAttemptSources;
  }
}
