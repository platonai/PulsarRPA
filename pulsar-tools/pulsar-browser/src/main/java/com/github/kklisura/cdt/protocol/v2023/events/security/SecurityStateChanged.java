package com.github.kklisura.cdt.protocol.v2023.events.security;

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
import com.github.kklisura.cdt.protocol.v2023.types.security.InsecureContentStatus;
import com.github.kklisura.cdt.protocol.v2023.types.security.SecurityState;
import com.github.kklisura.cdt.protocol.v2023.types.security.SecurityStateExplanation;

import java.util.List;

/** The security state of the page changed. No longer being sent. */
@Deprecated
public class SecurityStateChanged {

  private SecurityState securityState;

  @Deprecated private Boolean schemeIsCryptographic;

  @Deprecated private List<SecurityStateExplanation> explanations;

  @Deprecated private InsecureContentStatus insecureContentStatus;

  @Deprecated @Optional
  private String summary;

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

  /** Previously a list of explanations for the security state. Now always empty. */
  public List<SecurityStateExplanation> getExplanations() {
    return explanations;
  }

  /** Previously a list of explanations for the security state. Now always empty. */
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

  /** Overrides user-visible description of the state. Always omitted. */
  public String getSummary() {
    return summary;
  }

  /** Overrides user-visible description of the state. Always omitted. */
  public void setSummary(String summary) {
    this.summary = summary;
  }
}
