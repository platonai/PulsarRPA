package com.github.kklisura.cdt.protocol.types.network;

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

import com.fasterxml.jackson.annotation.JsonProperty;

/** Field type for a signed exchange related error. */
public enum SignedExchangeErrorField {
  @JsonProperty("signatureSig")
  SIGNATURE_SIG,
  @JsonProperty("signatureIntegrity")
  SIGNATURE_INTEGRITY,
  @JsonProperty("signatureCertUrl")
  SIGNATURE_CERT_URL,
  @JsonProperty("signatureCertSha256")
  SIGNATURE_CERT_SHA_256,
  @JsonProperty("signatureValidityUrl")
  SIGNATURE_VALIDITY_URL,
  @JsonProperty("signatureTimestamps")
  SIGNATURE_TIMESTAMPS
}
