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

import java.util.List;

/**
 * Lists sources for a preloading attempt, specifically the ids of rule sets that had a speculation
 * rule that triggered the attempt, and the BackendNodeIds of <a href> or <area href> elements that
 * triggered the attempt (in the case of attempts triggered by a document rule). It is possible for
 * mulitple rule sets and links to trigger a single attempt.
 */
public class PreloadingAttemptSource {

  private PreloadingAttemptKey key;

  private List<String> ruleSetIds;

  private List<Integer> nodeIds;

  public PreloadingAttemptKey getKey() {
    return key;
  }

  public void setKey(PreloadingAttemptKey key) {
    this.key = key;
  }

  public List<String> getRuleSetIds() {
    return ruleSetIds;
  }

  public void setRuleSetIds(List<String> ruleSetIds) {
    this.ruleSetIds = ruleSetIds;
  }

  public List<Integer> getNodeIds() {
    return nodeIds;
  }

  public void setNodeIds(List<Integer> nodeIds) {
    this.nodeIds = nodeIds;
  }
}
