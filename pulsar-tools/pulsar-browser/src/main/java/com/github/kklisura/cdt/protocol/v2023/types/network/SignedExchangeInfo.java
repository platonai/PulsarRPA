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

/** Information about a signed exchange response. */
@Experimental
public class SignedExchangeInfo {

  private Response outerResponse;

  @Optional
  private SignedExchangeHeader header;

  @Optional private SecurityDetails securityDetails;

  @Optional private List<SignedExchangeError> errors;

  /** The outer response of signed HTTP exchange which was received from network. */
  public Response getOuterResponse() {
    return outerResponse;
  }

  /** The outer response of signed HTTP exchange which was received from network. */
  public void setOuterResponse(Response outerResponse) {
    this.outerResponse = outerResponse;
  }

  /** Information about the signed exchange header. */
  public SignedExchangeHeader getHeader() {
    return header;
  }

  /** Information about the signed exchange header. */
  public void setHeader(SignedExchangeHeader header) {
    this.header = header;
  }

  /** Security details for the signed exchange header. */
  public SecurityDetails getSecurityDetails() {
    return securityDetails;
  }

  /** Security details for the signed exchange header. */
  public void setSecurityDetails(SecurityDetails securityDetails) {
    this.securityDetails = securityDetails;
  }

  /** Errors occurred while handling the signed exchagne. */
  public List<SignedExchangeError> getErrors() {
    return errors;
  }

  /** Errors occurred while handling the signed exchagne. */
  public void setErrors(List<SignedExchangeError> errors) {
    this.errors = errors;
  }
}
