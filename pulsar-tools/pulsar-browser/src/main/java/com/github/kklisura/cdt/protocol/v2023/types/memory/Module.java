package com.github.kklisura.cdt.protocol.v2023.types.memory;

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

/** Executable module information */
public class Module {

  private String name;

  private String uuid;

  private String baseAddress;

  private Double size;

  /** Name of the module. */
  public String getName() {
    return name;
  }

  /** Name of the module. */
  public void setName(String name) {
    this.name = name;
  }

  /** UUID of the module. */
  public String getUuid() {
    return uuid;
  }

  /** UUID of the module. */
  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  /**
   * Base address where the module is loaded into memory. Encoded as a decimal or hexadecimal (0x
   * prefixed) string.
   */
  public String getBaseAddress() {
    return baseAddress;
  }

  /**
   * Base address where the module is loaded into memory. Encoded as a decimal or hexadecimal (0x
   * prefixed) string.
   */
  public void setBaseAddress(String baseAddress) {
    this.baseAddress = baseAddress;
  }

  /** Size of the module in bytes. */
  public Double getSize() {
    return size;
  }

  /** Size of the module in bytes. */
  public void setSize(Double size) {
    this.size = size;
  }
}
