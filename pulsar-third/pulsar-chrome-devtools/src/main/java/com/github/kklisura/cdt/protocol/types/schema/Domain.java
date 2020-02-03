package com.github.kklisura.cdt.protocol.types.schema;

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

/** Description of the protocol domain. */
public class Domain {

  private String name;

  private String version;

  /** Domain name. */
  public String getName() {
    return name;
  }

  /** Domain name. */
  public void setName(String name) {
    this.name = name;
  }

  /** Domain version. */
  public String getVersion() {
    return version;
  }

  /** Domain version. */
  public void setVersion(String version) {
    this.version = version;
  }
}
