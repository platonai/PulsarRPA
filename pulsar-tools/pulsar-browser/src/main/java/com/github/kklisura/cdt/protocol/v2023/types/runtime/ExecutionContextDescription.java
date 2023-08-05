package com.github.kklisura.cdt.protocol.v2023.types.runtime;

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

import com.github.kklisura.cdt.protocol.v2023.support.annotations.Experimental;
import com.github.kklisura.cdt.protocol.v2023.support.annotations.Optional;

import java.util.Map;

/** Description of an isolated world. */
public class ExecutionContextDescription {

  private Integer id;

  private String origin;

  private String name;

  @Experimental
  private String uniqueId;

  @Optional
  private Map<String, Object> auxData;

  /**
   * Unique id of the execution context. It can be used to specify in which execution context script
   * evaluation should be performed.
   */
  public Integer getId() {
    return id;
  }

  /**
   * Unique id of the execution context. It can be used to specify in which execution context script
   * evaluation should be performed.
   */
  public void setId(Integer id) {
    this.id = id;
  }

  /** Execution context origin. */
  public String getOrigin() {
    return origin;
  }

  /** Execution context origin. */
  public void setOrigin(String origin) {
    this.origin = origin;
  }

  /** Human readable name describing given context. */
  public String getName() {
    return name;
  }

  /** Human readable name describing given context. */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * A system-unique execution context identifier. Unlike the id, this is unique across multiple
   * processes, so can be reliably used to identify specific context while backend performs a
   * cross-process navigation.
   */
  public String getUniqueId() {
    return uniqueId;
  }

  /**
   * A system-unique execution context identifier. Unlike the id, this is unique across multiple
   * processes, so can be reliably used to identify specific context while backend performs a
   * cross-process navigation.
   */
  public void setUniqueId(String uniqueId) {
    this.uniqueId = uniqueId;
  }

  /**
   * Embedder-specific auxiliary data likely matching {isDefault: boolean, type:
   * 'default'|'isolated'|'worker', frameId: string}
   */
  public Map<String, Object> getAuxData() {
    return auxData;
  }

  /**
   * Embedder-specific auxiliary data likely matching {isDefault: boolean, type:
   * 'default'|'isolated'|'worker', frameId: string}
   */
  public void setAuxData(Map<String, Object> auxData) {
    this.auxData = auxData;
  }
}
