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

import java.util.List;

@Experimental
public class BackForwardCacheNotRestoredExplanationTree {

  private String url;

  private List<BackForwardCacheNotRestoredExplanation> explanations;

  private List<BackForwardCacheNotRestoredExplanationTree> children;

  /** URL of each frame */
  public String getUrl() {
    return url;
  }

  /** URL of each frame */
  public void setUrl(String url) {
    this.url = url;
  }

  /** Not restored reasons of each frame */
  public List<BackForwardCacheNotRestoredExplanation> getExplanations() {
    return explanations;
  }

  /** Not restored reasons of each frame */
  public void setExplanations(List<BackForwardCacheNotRestoredExplanation> explanations) {
    this.explanations = explanations;
  }

  /** Array of children frame */
  public List<BackForwardCacheNotRestoredExplanationTree> getChildren() {
    return children;
  }

  /** Array of children frame */
  public void setChildren(List<BackForwardCacheNotRestoredExplanationTree> children) {
    this.children = children;
  }
}
