package com.github.kklisura.cdt.protocol.v2023.events.page;

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
import com.github.kklisura.cdt.protocol.v2023.types.page.BackForwardCacheNotRestoredExplanation;
import com.github.kklisura.cdt.protocol.v2023.types.page.BackForwardCacheNotRestoredExplanationTree;

import java.util.List;

/**
 * Fired for failed bfcache history navigations if BackForwardCache feature is enabled. Do not
 * assume any ordering with the Page.frameNavigated event. This event is fired only for main-frame
 * history navigation where the document changes (non-same-document navigations), when bfcache
 * navigation fails.
 */
@Experimental
public class BackForwardCacheNotUsed {

  private String loaderId;

  private String frameId;

  private List<BackForwardCacheNotRestoredExplanation> notRestoredExplanations;

  @Optional
  private BackForwardCacheNotRestoredExplanationTree notRestoredExplanationsTree;

  /** The loader id for the associated navgation. */
  public String getLoaderId() {
    return loaderId;
  }

  /** The loader id for the associated navgation. */
  public void setLoaderId(String loaderId) {
    this.loaderId = loaderId;
  }

  /** The frame id of the associated frame. */
  public String getFrameId() {
    return frameId;
  }

  /** The frame id of the associated frame. */
  public void setFrameId(String frameId) {
    this.frameId = frameId;
  }

  /** Array of reasons why the page could not be cached. This must not be empty. */
  public List<BackForwardCacheNotRestoredExplanation> getNotRestoredExplanations() {
    return notRestoredExplanations;
  }

  /** Array of reasons why the page could not be cached. This must not be empty. */
  public void setNotRestoredExplanations(
      List<BackForwardCacheNotRestoredExplanation> notRestoredExplanations) {
    this.notRestoredExplanations = notRestoredExplanations;
  }

  /** Tree structure of reasons why the page could not be cached for each frame. */
  public BackForwardCacheNotRestoredExplanationTree getNotRestoredExplanationsTree() {
    return notRestoredExplanationsTree;
  }

  /** Tree structure of reasons why the page could not be cached for each frame. */
  public void setNotRestoredExplanationsTree(
      BackForwardCacheNotRestoredExplanationTree notRestoredExplanationsTree) {
    this.notRestoredExplanationsTree = notRestoredExplanationsTree;
  }
}
