package com.github.kklisura.cdt.protocol.v2023.types.audits;

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
import com.github.kklisura.cdt.protocol.v2023.types.network.ClientSecurityState;
import com.github.kklisura.cdt.protocol.v2023.types.network.CorsErrorStatus;
import com.github.kklisura.cdt.protocol.v2023.types.network.IPAddressSpace;

/**
 * Details for a CORS related issue, e.g. a warning or error related to CORS RFC1918 enforcement.
 */
public class CorsIssueDetails {

  private CorsErrorStatus corsErrorStatus;

  private Boolean isWarning;

  private AffectedRequest request;

  @Optional
  private SourceCodeLocation location;

  @Optional private String initiatorOrigin;

  @Optional private IPAddressSpace resourceIPAddressSpace;

  @Optional private ClientSecurityState clientSecurityState;

  public CorsErrorStatus getCorsErrorStatus() {
    return corsErrorStatus;
  }

  public void setCorsErrorStatus(CorsErrorStatus corsErrorStatus) {
    this.corsErrorStatus = corsErrorStatus;
  }

  public Boolean getIsWarning() {
    return isWarning;
  }

  public void setIsWarning(Boolean isWarning) {
    this.isWarning = isWarning;
  }

  public AffectedRequest getRequest() {
    return request;
  }

  public void setRequest(AffectedRequest request) {
    this.request = request;
  }

  public SourceCodeLocation getLocation() {
    return location;
  }

  public void setLocation(SourceCodeLocation location) {
    this.location = location;
  }

  public String getInitiatorOrigin() {
    return initiatorOrigin;
  }

  public void setInitiatorOrigin(String initiatorOrigin) {
    this.initiatorOrigin = initiatorOrigin;
  }

  public IPAddressSpace getResourceIPAddressSpace() {
    return resourceIPAddressSpace;
  }

  public void setResourceIPAddressSpace(IPAddressSpace resourceIPAddressSpace) {
    this.resourceIPAddressSpace = resourceIPAddressSpace;
  }

  public ClientSecurityState getClientSecurityState() {
    return clientSecurityState;
  }

  public void setClientSecurityState(ClientSecurityState clientSecurityState) {
    this.clientSecurityState = clientSecurityState;
  }
}
