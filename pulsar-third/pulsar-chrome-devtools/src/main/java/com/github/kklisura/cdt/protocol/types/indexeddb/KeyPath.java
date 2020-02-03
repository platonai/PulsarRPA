package com.github.kklisura.cdt.protocol.types.indexeddb;

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

import com.github.kklisura.cdt.protocol.support.annotations.Optional;
import java.util.List;

/** Key path. */
public class KeyPath {

  private KeyPathType type;

  @Optional private String string;

  @Optional private List<String> array;

  /** Key path type. */
  public KeyPathType getType() {
    return type;
  }

  /** Key path type. */
  public void setType(KeyPathType type) {
    this.type = type;
  }

  /** String value. */
  public String getString() {
    return string;
  }

  /** String value. */
  public void setString(String string) {
    this.string = string;
  }

  /** Array value. */
  public List<String> getArray() {
    return array;
  }

  /** Array value. */
  public void setArray(List<String> array) {
    this.array = array;
  }
}
