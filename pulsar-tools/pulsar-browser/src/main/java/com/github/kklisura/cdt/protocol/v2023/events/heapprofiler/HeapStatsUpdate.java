package com.github.kklisura.cdt.protocol.v2023.events.heapprofiler;

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
 * If heap objects tracking has been started then backend may send update for one or more fragments
 */
public class HeapStatsUpdate {

  private List<Integer> statsUpdate;

  /**
   * An array of triplets. Each triplet describes a fragment. The first integer is the fragment
   * index, the second integer is a total count of objects for the fragment, the third integer is a
   * total size of the objects for the fragment.
   */
  public List<Integer> getStatsUpdate() {
    return statsUpdate;
  }

  /**
   * An array of triplets. Each triplet describes a fragment. The first integer is the fragment
   * index, the second integer is a total count of objects for the fragment, the third integer is a
   * total size of the objects for the fragment.
   */
  public void setStatsUpdate(List<Integer> statsUpdate) {
    this.statsUpdate = statsUpdate;
  }
}
