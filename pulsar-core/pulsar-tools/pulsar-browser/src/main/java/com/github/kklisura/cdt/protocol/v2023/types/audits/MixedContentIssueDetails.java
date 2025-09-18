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

public class MixedContentIssueDetails {

  @Optional
  private MixedContentResourceType resourceType;

  private MixedContentResolutionStatus resolutionStatus;

  private String insecureURL;

  private String mainResourceURL;

  @Optional private AffectedRequest request;

  @Optional private AffectedFrame frame;

  /**
   * The type of resource causing the mixed content issue (css, js, iframe, form,...). Marked as
   * optional because it is mapped to from blink::mojom::RequestContextType, which will be replaced
   * by network::mojom::RequestDestination
   */
  public MixedContentResourceType getResourceType() {
    return resourceType;
  }

  /**
   * The type of resource causing the mixed content issue (css, js, iframe, form,...). Marked as
   * optional because it is mapped to from blink::mojom::RequestContextType, which will be replaced
   * by network::mojom::RequestDestination
   */
  public void setResourceType(MixedContentResourceType resourceType) {
    this.resourceType = resourceType;
  }

  /** The way the mixed content issue is being resolved. */
  public MixedContentResolutionStatus getResolutionStatus() {
    return resolutionStatus;
  }

  /** The way the mixed content issue is being resolved. */
  public void setResolutionStatus(MixedContentResolutionStatus resolutionStatus) {
    this.resolutionStatus = resolutionStatus;
  }

  /** The unsafe http url causing the mixed content issue. */
  public String getInsecureURL() {
    return insecureURL;
  }

  /** The unsafe http url causing the mixed content issue. */
  public void setInsecureURL(String insecureURL) {
    this.insecureURL = insecureURL;
  }

  /** The url responsible for the call to an unsafe url. */
  public String getMainResourceURL() {
    return mainResourceURL;
  }

  /** The url responsible for the call to an unsafe url. */
  public void setMainResourceURL(String mainResourceURL) {
    this.mainResourceURL = mainResourceURL;
  }

  /** The mixed content request. Does not always exist (e.g. for unsafe form submission urls). */
  public AffectedRequest getRequest() {
    return request;
  }

  /** The mixed content request. Does not always exist (e.g. for unsafe form submission urls). */
  public void setRequest(AffectedRequest request) {
    this.request = request;
  }

  /** Optional because not every mixed content issue is necessarily linked to a frame. */
  public AffectedFrame getFrame() {
    return frame;
  }

  /** Optional because not every mixed content issue is necessarily linked to a frame. */
  public void setFrame(AffectedFrame frame) {
    this.frame = frame;
  }
}
