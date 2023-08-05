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

/**
 * This struct holds a list of optional fields with additional information specific to the kind of
 * issue. When adding a new issue code, please also add a new optional field to this type.
 */
public class InspectorIssueDetails {

  @Optional
  private CookieIssueDetails cookieIssueDetails;

  @Optional private MixedContentIssueDetails mixedContentIssueDetails;

  @Optional private BlockedByResponseIssueDetails blockedByResponseIssueDetails;

  @Optional private HeavyAdIssueDetails heavyAdIssueDetails;

  @Optional private ContentSecurityPolicyIssueDetails contentSecurityPolicyIssueDetails;

  @Optional private SharedArrayBufferIssueDetails sharedArrayBufferIssueDetails;

  @Optional private LowTextContrastIssueDetails lowTextContrastIssueDetails;

  @Optional private CorsIssueDetails corsIssueDetails;

  @Optional private AttributionReportingIssueDetails attributionReportingIssueDetails;

  @Optional private QuirksModeIssueDetails quirksModeIssueDetails;

  @Deprecated @Optional private NavigatorUserAgentIssueDetails navigatorUserAgentIssueDetails;

  @Optional private GenericIssueDetails genericIssueDetails;

  @Optional private DeprecationIssueDetails deprecationIssueDetails;

  @Optional private ClientHintIssueDetails clientHintIssueDetails;

  @Optional private FederatedAuthRequestIssueDetails federatedAuthRequestIssueDetails;

  @Optional private BounceTrackingIssueDetails bounceTrackingIssueDetails;

  @Optional private StylesheetLoadingIssueDetails stylesheetLoadingIssueDetails;

  @Optional
  private FederatedAuthUserInfoRequestIssueDetails federatedAuthUserInfoRequestIssueDetails;

  public CookieIssueDetails getCookieIssueDetails() {
    return cookieIssueDetails;
  }

  public void setCookieIssueDetails(CookieIssueDetails cookieIssueDetails) {
    this.cookieIssueDetails = cookieIssueDetails;
  }

  public MixedContentIssueDetails getMixedContentIssueDetails() {
    return mixedContentIssueDetails;
  }

  public void setMixedContentIssueDetails(MixedContentIssueDetails mixedContentIssueDetails) {
    this.mixedContentIssueDetails = mixedContentIssueDetails;
  }

  public BlockedByResponseIssueDetails getBlockedByResponseIssueDetails() {
    return blockedByResponseIssueDetails;
  }

  public void setBlockedByResponseIssueDetails(
      BlockedByResponseIssueDetails blockedByResponseIssueDetails) {
    this.blockedByResponseIssueDetails = blockedByResponseIssueDetails;
  }

  public HeavyAdIssueDetails getHeavyAdIssueDetails() {
    return heavyAdIssueDetails;
  }

  public void setHeavyAdIssueDetails(HeavyAdIssueDetails heavyAdIssueDetails) {
    this.heavyAdIssueDetails = heavyAdIssueDetails;
  }

  public ContentSecurityPolicyIssueDetails getContentSecurityPolicyIssueDetails() {
    return contentSecurityPolicyIssueDetails;
  }

  public void setContentSecurityPolicyIssueDetails(
      ContentSecurityPolicyIssueDetails contentSecurityPolicyIssueDetails) {
    this.contentSecurityPolicyIssueDetails = contentSecurityPolicyIssueDetails;
  }

  public SharedArrayBufferIssueDetails getSharedArrayBufferIssueDetails() {
    return sharedArrayBufferIssueDetails;
  }

  public void setSharedArrayBufferIssueDetails(
      SharedArrayBufferIssueDetails sharedArrayBufferIssueDetails) {
    this.sharedArrayBufferIssueDetails = sharedArrayBufferIssueDetails;
  }

  public LowTextContrastIssueDetails getLowTextContrastIssueDetails() {
    return lowTextContrastIssueDetails;
  }

  public void setLowTextContrastIssueDetails(
      LowTextContrastIssueDetails lowTextContrastIssueDetails) {
    this.lowTextContrastIssueDetails = lowTextContrastIssueDetails;
  }

  public CorsIssueDetails getCorsIssueDetails() {
    return corsIssueDetails;
  }

  public void setCorsIssueDetails(CorsIssueDetails corsIssueDetails) {
    this.corsIssueDetails = corsIssueDetails;
  }

  public AttributionReportingIssueDetails getAttributionReportingIssueDetails() {
    return attributionReportingIssueDetails;
  }

  public void setAttributionReportingIssueDetails(
      AttributionReportingIssueDetails attributionReportingIssueDetails) {
    this.attributionReportingIssueDetails = attributionReportingIssueDetails;
  }

  public QuirksModeIssueDetails getQuirksModeIssueDetails() {
    return quirksModeIssueDetails;
  }

  public void setQuirksModeIssueDetails(QuirksModeIssueDetails quirksModeIssueDetails) {
    this.quirksModeIssueDetails = quirksModeIssueDetails;
  }

  public NavigatorUserAgentIssueDetails getNavigatorUserAgentIssueDetails() {
    return navigatorUserAgentIssueDetails;
  }

  public void setNavigatorUserAgentIssueDetails(
      NavigatorUserAgentIssueDetails navigatorUserAgentIssueDetails) {
    this.navigatorUserAgentIssueDetails = navigatorUserAgentIssueDetails;
  }

  public GenericIssueDetails getGenericIssueDetails() {
    return genericIssueDetails;
  }

  public void setGenericIssueDetails(GenericIssueDetails genericIssueDetails) {
    this.genericIssueDetails = genericIssueDetails;
  }

  public DeprecationIssueDetails getDeprecationIssueDetails() {
    return deprecationIssueDetails;
  }

  public void setDeprecationIssueDetails(DeprecationIssueDetails deprecationIssueDetails) {
    this.deprecationIssueDetails = deprecationIssueDetails;
  }

  public ClientHintIssueDetails getClientHintIssueDetails() {
    return clientHintIssueDetails;
  }

  public void setClientHintIssueDetails(ClientHintIssueDetails clientHintIssueDetails) {
    this.clientHintIssueDetails = clientHintIssueDetails;
  }

  public FederatedAuthRequestIssueDetails getFederatedAuthRequestIssueDetails() {
    return federatedAuthRequestIssueDetails;
  }

  public void setFederatedAuthRequestIssueDetails(
      FederatedAuthRequestIssueDetails federatedAuthRequestIssueDetails) {
    this.federatedAuthRequestIssueDetails = federatedAuthRequestIssueDetails;
  }

  public BounceTrackingIssueDetails getBounceTrackingIssueDetails() {
    return bounceTrackingIssueDetails;
  }

  public void setBounceTrackingIssueDetails(BounceTrackingIssueDetails bounceTrackingIssueDetails) {
    this.bounceTrackingIssueDetails = bounceTrackingIssueDetails;
  }

  public StylesheetLoadingIssueDetails getStylesheetLoadingIssueDetails() {
    return stylesheetLoadingIssueDetails;
  }

  public void setStylesheetLoadingIssueDetails(
      StylesheetLoadingIssueDetails stylesheetLoadingIssueDetails) {
    this.stylesheetLoadingIssueDetails = stylesheetLoadingIssueDetails;
  }

  public FederatedAuthUserInfoRequestIssueDetails getFederatedAuthUserInfoRequestIssueDetails() {
    return federatedAuthUserInfoRequestIssueDetails;
  }

  public void setFederatedAuthUserInfoRequestIssueDetails(
      FederatedAuthUserInfoRequestIssueDetails federatedAuthUserInfoRequestIssueDetails) {
    this.federatedAuthUserInfoRequestIssueDetails = federatedAuthUserInfoRequestIssueDetails;
  }
}
