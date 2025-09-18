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

import com.github.kklisura.cdt.protocol.v2023.support.annotations.Optional;
import com.github.kklisura.cdt.protocol.v2023.types.network.Initiator;

/** Fired upon WebTransport creation. */
public class WebTransportCreated {

  private String transportId;

  private String url;

  private Double timestamp;

  @Optional
  private Initiator initiator;

  /** WebTransport identifier. */
  public String getTransportId() {
    return transportId;
  }

  /** WebTransport identifier. */
  public void setTransportId(String transportId) {
    this.transportId = transportId;
  }

  /** WebTransport request URL. */
  public String getUrl() {
    return url;
  }

  /** WebTransport request URL. */
  public void setUrl(String url) {
    this.url = url;
  }

  /** Timestamp. */
  public Double getTimestamp() {
    return timestamp;
  }

  /** Timestamp. */
  public void setTimestamp(Double timestamp) {
    this.timestamp = timestamp;
  }

  /** Request initiator. */
  public Initiator getInitiator() {
    return initiator;
  }

  /** Request initiator. */
  public void setInitiator(Initiator initiator) {
    this.initiator = initiator;
  }
}
