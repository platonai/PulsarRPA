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

/**
 * WebSocket message data. This represents an entire WebSocket message, not just a fragmented frame
 * as the name suggests.
 */
public class WebSocketFrame {

  private Double opcode;

  private Boolean mask;

  private String payloadData;

  /** WebSocket message opcode. */
  public Double getOpcode() {
    return opcode;
  }

  /** WebSocket message opcode. */
  public void setOpcode(Double opcode) {
    this.opcode = opcode;
  }

  /** WebSocket message mask. */
  public Boolean getMask() {
    return mask;
  }

  /** WebSocket message mask. */
  public void setMask(Boolean mask) {
    this.mask = mask;
  }

  /**
   * WebSocket message payload data. If the opcode is 1, this is a text message and payloadData is a
   * UTF-8 string. If the opcode isn't 1, then payloadData is a base64 encoded string representing
   * binary data.
   */
  public String getPayloadData() {
    return payloadData;
  }

  /**
   * WebSocket message payload data. If the opcode is 1, this is a text message and payloadData is a
   * UTF-8 string. If the opcode isn't 1, then payloadData is a base64 encoded string representing
   * binary data.
   */
  public void setPayloadData(String payloadData) {
    this.payloadData = payloadData;
  }
}
