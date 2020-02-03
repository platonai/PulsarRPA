package com.github.kklisura.cdt.protocol.types.debugger;

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
import com.github.kklisura.cdt.protocol.types.runtime.RemoteObject;

/** Scope description. */
public class Scope {

  private ScopeType type;

  private RemoteObject object;

  @Optional private String name;

  @Optional private Location startLocation;

  @Optional private Location endLocation;

  /** Scope type. */
  public ScopeType getType() {
    return type;
  }

  /** Scope type. */
  public void setType(ScopeType type) {
    this.type = type;
  }

  /**
   * Object representing the scope. For `global` and `with` scopes it represents the actual object;
   * for the rest of the scopes, it is artificial transient object enumerating scope variables as
   * its properties.
   */
  public RemoteObject getObject() {
    return object;
  }

  /**
   * Object representing the scope. For `global` and `with` scopes it represents the actual object;
   * for the rest of the scopes, it is artificial transient object enumerating scope variables as
   * its properties.
   */
  public void setObject(RemoteObject object) {
    this.object = object;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  /** Location in the source code where scope starts */
  public Location getStartLocation() {
    return startLocation;
  }

  /** Location in the source code where scope starts */
  public void setStartLocation(Location startLocation) {
    this.startLocation = startLocation;
  }

  /** Location in the source code where scope ends */
  public Location getEndLocation() {
    return endLocation;
  }

  /** Location in the source code where scope ends */
  public void setEndLocation(Location endLocation) {
    this.endLocation = endLocation;
  }
}
