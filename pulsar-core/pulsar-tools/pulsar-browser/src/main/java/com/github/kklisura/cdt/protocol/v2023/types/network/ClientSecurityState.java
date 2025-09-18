package com.github.kklisura.cdt.protocol.v2023.types.network;

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

@Experimental
public class ClientSecurityState {

  private Boolean initiatorIsSecureContext;

  private IPAddressSpace initiatorIPAddressSpace;

  private PrivateNetworkRequestPolicy privateNetworkRequestPolicy;

  public Boolean getInitiatorIsSecureContext() {
    return initiatorIsSecureContext;
  }

  public void setInitiatorIsSecureContext(Boolean initiatorIsSecureContext) {
    this.initiatorIsSecureContext = initiatorIsSecureContext;
  }

  public IPAddressSpace getInitiatorIPAddressSpace() {
    return initiatorIPAddressSpace;
  }

  public void setInitiatorIPAddressSpace(IPAddressSpace initiatorIPAddressSpace) {
    this.initiatorIPAddressSpace = initiatorIPAddressSpace;
  }

  public PrivateNetworkRequestPolicy getPrivateNetworkRequestPolicy() {
    return privateNetworkRequestPolicy;
  }

  public void setPrivateNetworkRequestPolicy(
      PrivateNetworkRequestPolicy privateNetworkRequestPolicy) {
    this.privateNetworkRequestPolicy = privateNetworkRequestPolicy;
  }
}
