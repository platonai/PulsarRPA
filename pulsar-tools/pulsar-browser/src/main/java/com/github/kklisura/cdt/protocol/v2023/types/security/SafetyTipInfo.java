package com.github.kklisura.cdt.protocol.v2023.types.security;

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

@Experimental
public class SafetyTipInfo {

  private SafetyTipStatus safetyTipStatus;

  @Optional
  private String safeUrl;

  /**
   * Describes whether the page triggers any safety tips or reputation warnings. Default is unknown.
   */
  public SafetyTipStatus getSafetyTipStatus() {
    return safetyTipStatus;
  }

  /**
   * Describes whether the page triggers any safety tips or reputation warnings. Default is unknown.
   */
  public void setSafetyTipStatus(SafetyTipStatus safetyTipStatus) {
    this.safetyTipStatus = safetyTipStatus;
  }

  /** The URL the safety tip suggested ("Did you mean?"). Only filled in for lookalike matches. */
  public String getSafeUrl() {
    return safeUrl;
  }

  /** The URL the safety tip suggested ("Did you mean?"). Only filled in for lookalike matches. */
  public void setSafeUrl(String safeUrl) {
    this.safeUrl = safeUrl;
  }
}
