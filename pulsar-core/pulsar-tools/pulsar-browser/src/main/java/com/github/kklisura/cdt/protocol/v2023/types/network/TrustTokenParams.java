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
import com.github.kklisura.cdt.protocol.v2023.support.annotations.Optional;

import java.util.List;

/**
 * Determines what type of Trust Token operation is executed and depending on the type, some
 * additional parameters. The values are specified in
 * third_party/blink/renderer/core/fetch/trust_token.idl.
 */
@Experimental
public class TrustTokenParams {

  private TrustTokenOperationType operation;

  private TrustTokenParamsRefreshPolicy refreshPolicy;

  @Optional
  private List<String> issuers;

  public TrustTokenOperationType getOperation() {
    return operation;
  }

  public void setOperation(TrustTokenOperationType operation) {
    this.operation = operation;
  }

  /**
   * Only set for "token-redemption" operation and determine whether to request a fresh SRR or use a
   * still valid cached SRR.
   */
  public TrustTokenParamsRefreshPolicy getRefreshPolicy() {
    return refreshPolicy;
  }

  /**
   * Only set for "token-redemption" operation and determine whether to request a fresh SRR or use a
   * still valid cached SRR.
   */
  public void setRefreshPolicy(TrustTokenParamsRefreshPolicy refreshPolicy) {
    this.refreshPolicy = refreshPolicy;
  }

  /** Origins of issuers from whom to request tokens or redemption records. */
  public List<String> getIssuers() {
    return issuers;
  }

  /** Origins of issuers from whom to request tokens or redemption records. */
  public void setIssuers(List<String> issuers) {
    this.issuers = issuers;
  }
}
