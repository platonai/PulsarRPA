package com.github.kklisura.cdt.protocol.v2023.types.layertree;

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

public class CompositingReasons {

  private List<String> compositingReasons;

  private List<String> compositingReasonIds;

  /** A list of strings specifying reasons for the given layer to become composited. */
  public List<String> getCompositingReasons() {
    return compositingReasons;
  }

  /** A list of strings specifying reasons for the given layer to become composited. */
  public void setCompositingReasons(List<String> compositingReasons) {
    this.compositingReasons = compositingReasons;
  }

  /** A list of strings specifying reason IDs for the given layer to become composited. */
  public List<String> getCompositingReasonIds() {
    return compositingReasonIds;
  }

  /** A list of strings specifying reason IDs for the given layer to become composited. */
  public void setCompositingReasonIds(List<String> compositingReasonIds) {
    this.compositingReasonIds = compositingReasonIds;
  }
}
