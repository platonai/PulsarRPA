package com.github.kklisura.cdt.protocol.v2023.events.network;

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

/** Fired when data chunk was received over the network. */
public class DataReceived {

  private String requestId;

  private Double timestamp;

  private Integer dataLength;

  private Integer encodedDataLength;

  /** Request identifier. */
  public String getRequestId() {
    return requestId;
  }

  /** Request identifier. */
  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  /** Timestamp. */
  public Double getTimestamp() {
    return timestamp;
  }

  /** Timestamp. */
  public void setTimestamp(Double timestamp) {
    this.timestamp = timestamp;
  }

  /** Data chunk length. */
  public Integer getDataLength() {
    return dataLength;
  }

  /** Data chunk length. */
  public void setDataLength(Integer dataLength) {
    this.dataLength = dataLength;
  }

  /** Actual bytes received (might be less than dataLength for compressed encodings). */
  public Integer getEncodedDataLength() {
    return encodedDataLength;
  }

  /** Actual bytes received (might be less than dataLength for compressed encodings). */
  public void setEncodedDataLength(Integer encodedDataLength) {
    this.encodedDataLength = encodedDataLength;
  }
}
