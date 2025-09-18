package com.github.kklisura.cdt.protocol.v2023.types.storage;

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

import java.util.List;

/** The full details of an interest group. */
public class InterestGroupDetails {

  private String ownerOrigin;

  private String name;

  private Double expirationTime;

  private String joiningOrigin;

  @Optional
  private String biddingUrl;

  @Optional private String biddingWasmHelperUrl;

  @Optional private String updateUrl;

  @Optional private String trustedBiddingSignalsUrl;

  private List<String> trustedBiddingSignalsKeys;

  @Optional private String userBiddingSignals;

  private List<InterestGroupAd> ads;

  private List<InterestGroupAd> adComponents;

  public String getOwnerOrigin() {
    return ownerOrigin;
  }

  public void setOwnerOrigin(String ownerOrigin) {
    this.ownerOrigin = ownerOrigin;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Double getExpirationTime() {
    return expirationTime;
  }

  public void setExpirationTime(Double expirationTime) {
    this.expirationTime = expirationTime;
  }

  public String getJoiningOrigin() {
    return joiningOrigin;
  }

  public void setJoiningOrigin(String joiningOrigin) {
    this.joiningOrigin = joiningOrigin;
  }

  public String getBiddingUrl() {
    return biddingUrl;
  }

  public void setBiddingUrl(String biddingUrl) {
    this.biddingUrl = biddingUrl;
  }

  public String getBiddingWasmHelperUrl() {
    return biddingWasmHelperUrl;
  }

  public void setBiddingWasmHelperUrl(String biddingWasmHelperUrl) {
    this.biddingWasmHelperUrl = biddingWasmHelperUrl;
  }

  public String getUpdateUrl() {
    return updateUrl;
  }

  public void setUpdateUrl(String updateUrl) {
    this.updateUrl = updateUrl;
  }

  public String getTrustedBiddingSignalsUrl() {
    return trustedBiddingSignalsUrl;
  }

  public void setTrustedBiddingSignalsUrl(String trustedBiddingSignalsUrl) {
    this.trustedBiddingSignalsUrl = trustedBiddingSignalsUrl;
  }

  public List<String> getTrustedBiddingSignalsKeys() {
    return trustedBiddingSignalsKeys;
  }

  public void setTrustedBiddingSignalsKeys(List<String> trustedBiddingSignalsKeys) {
    this.trustedBiddingSignalsKeys = trustedBiddingSignalsKeys;
  }

  public String getUserBiddingSignals() {
    return userBiddingSignals;
  }

  public void setUserBiddingSignals(String userBiddingSignals) {
    this.userBiddingSignals = userBiddingSignals;
  }

  public List<InterestGroupAd> getAds() {
    return ads;
  }

  public void setAds(List<InterestGroupAd> ads) {
    this.ads = ads;
  }

  public List<InterestGroupAd> getAdComponents() {
    return adComponents;
  }

  public void setAdComponents(List<InterestGroupAd> adComponents) {
    this.adComponents = adComponents;
  }
}
