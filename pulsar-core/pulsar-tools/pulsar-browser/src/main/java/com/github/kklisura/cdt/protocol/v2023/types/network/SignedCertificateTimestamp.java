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

/** Details of a signed certificate timestamp (SCT). */
public class SignedCertificateTimestamp {

  private String status;

  private String origin;

  private String logDescription;

  private String logId;

  private Double timestamp;

  private String hashAlgorithm;

  private String signatureAlgorithm;

  private String signatureData;

  /** Validation status. */
  public String getStatus() {
    return status;
  }

  /** Validation status. */
  public void setStatus(String status) {
    this.status = status;
  }

  /** Origin. */
  public String getOrigin() {
    return origin;
  }

  /** Origin. */
  public void setOrigin(String origin) {
    this.origin = origin;
  }

  /** Log name / description. */
  public String getLogDescription() {
    return logDescription;
  }

  /** Log name / description. */
  public void setLogDescription(String logDescription) {
    this.logDescription = logDescription;
  }

  /** Log ID. */
  public String getLogId() {
    return logId;
  }

  /** Log ID. */
  public void setLogId(String logId) {
    this.logId = logId;
  }

  /**
   * Issuance date. Unlike TimeSinceEpoch, this contains the number of milliseconds since January 1,
   * 1970, UTC, not the number of seconds.
   */
  public Double getTimestamp() {
    return timestamp;
  }

  /**
   * Issuance date. Unlike TimeSinceEpoch, this contains the number of milliseconds since January 1,
   * 1970, UTC, not the number of seconds.
   */
  public void setTimestamp(Double timestamp) {
    this.timestamp = timestamp;
  }

  /** Hash algorithm. */
  public String getHashAlgorithm() {
    return hashAlgorithm;
  }

  /** Hash algorithm. */
  public void setHashAlgorithm(String hashAlgorithm) {
    this.hashAlgorithm = hashAlgorithm;
  }

  /** Signature algorithm. */
  public String getSignatureAlgorithm() {
    return signatureAlgorithm;
  }

  /** Signature algorithm. */
  public void setSignatureAlgorithm(String signatureAlgorithm) {
    this.signatureAlgorithm = signatureAlgorithm;
  }

  /** Signature data. */
  public String getSignatureData() {
    return signatureData;
  }

  /** Signature data. */
  public void setSignatureData(String signatureData) {
    this.signatureData = signatureData;
  }
}
