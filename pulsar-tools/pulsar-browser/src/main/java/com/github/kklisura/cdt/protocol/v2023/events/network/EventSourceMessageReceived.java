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

/** Fired when EventSource message is received. */
public class EventSourceMessageReceived {

  private String requestId;

  private Double timestamp;

  private String eventName;

  private String eventId;

  private String data;

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

  /** Message type. */
  public String getEventName() {
    return eventName;
  }

  /** Message type. */
  public void setEventName(String eventName) {
    this.eventName = eventName;
  }

  /** Message identifier. */
  public String getEventId() {
    return eventId;
  }

  /** Message identifier. */
  public void setEventId(String eventId) {
    this.eventId = eventId;
  }

  /** Message content. */
  public String getData() {
    return data;
  }

  /** Message content. */
  public void setData(String data) {
    this.data = data;
  }
}
