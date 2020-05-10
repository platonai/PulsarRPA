package com.github.kklisura.cdt.protocol.events.security;

/*-
 * #%L
 * cdt-java-client
 * %%
 * Copyright (C) 2018 - 2019 Kenan Klisura
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

import com.github.kklisura.cdt.protocol.support.annotations.Optional;
import com.github.kklisura.cdt.protocol.types.security.InsecureContentStatus;
import com.github.kklisura.cdt.protocol.types.security.SecurityState;
import com.github.kklisura.cdt.protocol.types.security.SecurityStateExplanation;
import java.util.List;

/** The security state of the page changed. */
public class SecurityStateChanged {

  private SecurityState securityState;

  @Deprecated private Boolean schemeIsCryptographic;

  private List<SecurityStateExplanation> explanations;

  @Deprecated private InsecureContentStatus insecureContentStatus;

  @Optional private String summary;

  /** Security state. */
  public SecurityState getSecurityState() {
    return securityState;
  }

  /** Security state. */
  public void setSecurityState(SecurityState securityState) {
    this.securityState = securityState;
  }

  /** True if the page was loaded over cryptographic transport such as HTTPS. */
  public Boolean getSchemeIsCryptographic() {
    return schemeIsCryptographic;
  }

  /** True if the page was loaded over cryptographic transport such as HTTPS. */
  public void setSchemeIsCryptographic(Boolean schemeIsCryptographic) {
    this.schemeIsCryptographic = schemeIsCryptographic;
  }

  /**
   * List of explanations for the security state. If the overall security state is `insecure` or
   * `warning`, at least one corresponding explanation should be included.
   */
  public List<SecurityStateExplanation> getExplanations() {
    return explanations;
  }

  /**
   * List of explanations for the security state. If the overall security state is `insecure` or
   * `warning`, at least one corresponding explanation should be included.
   */
  public void setExplanations(List<SecurityStateExplanation> explanations) {
    this.explanations = explanations;
  }

  /** Information about insecure content on the page. */
  public InsecureContentStatus getInsecureContentStatus() {
    return insecureContentStatus;
  }

  /** Information about insecure content on the page. */
  public void setInsecureContentStatus(InsecureContentStatus insecureContentStatus) {
    this.insecureContentStatus = insecureContentStatus;
  }

  /** Overrides user-visible description of the state. */
  public String getSummary() {
    return summary;
  }

  /** Overrides user-visible description of the state. */
  public void setSummary(String summary) {
    this.summary = summary;
  }
}
