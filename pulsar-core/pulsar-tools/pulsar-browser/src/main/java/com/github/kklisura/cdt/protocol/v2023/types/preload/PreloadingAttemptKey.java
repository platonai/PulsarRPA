package com.github.kklisura.cdt.protocol.v2023.types.preload;

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

/**
 * A key that identifies a preloading attempt.
 *
 * <p>The url used is the url specified by the trigger (i.e. the initial URL), and not the final url
 * that is navigated to. For example, prerendering allows same-origin main frame navigations during
 * the attempt, but the attempt is still keyed with the initial URL.
 */
public class PreloadingAttemptKey {

  private String loaderId;

  private SpeculationAction action;

  private String url;

  @Optional
  private SpeculationTargetHint targetHint;

  public String getLoaderId() {
    return loaderId;
  }

  public void setLoaderId(String loaderId) {
    this.loaderId = loaderId;
  }

  public SpeculationAction getAction() {
    return action;
  }

  public void setAction(SpeculationAction action) {
    this.action = action;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public SpeculationTargetHint getTargetHint() {
    return targetHint;
  }

  public void setTargetHint(SpeculationTargetHint targetHint) {
    this.targetHint = targetHint;
  }
}
