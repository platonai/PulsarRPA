package com.github.kklisura.cdt.protocol.v2023.types.autofill;

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

public class AddressField {

  private String name;

  private String value;

  /** address field name, for example GIVEN_NAME. */
  public String getName() {
    return name;
  }

  /** address field name, for example GIVEN_NAME. */
  public void setName(String name) {
    this.name = name;
  }

  /** address field name, for example Jon Doe. */
  public String getValue() {
    return value;
  }

  /** address field name, for example Jon Doe. */
  public void setValue(String value) {
    this.value = value;
  }
}
