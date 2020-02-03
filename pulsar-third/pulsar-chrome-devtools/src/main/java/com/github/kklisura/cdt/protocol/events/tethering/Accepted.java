package com.github.kklisura.cdt.protocol.events.tethering;

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

/** Informs that port was successfully bound and got a specified connection id. */
public class Accepted {

  private Integer port;

  private String connectionId;

  /** Port number that was successfully bound. */
  public Integer getPort() {
    return port;
  }

  /** Port number that was successfully bound. */
  public void setPort(Integer port) {
    this.port = port;
  }

  /** Connection id to be used. */
  public String getConnectionId() {
    return connectionId;
  }

  /** Connection id to be used. */
  public void setConnectionId(String connectionId) {
    this.connectionId = connectionId;
  }
}
