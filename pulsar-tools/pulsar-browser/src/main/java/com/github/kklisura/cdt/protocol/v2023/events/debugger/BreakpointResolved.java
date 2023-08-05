package com.github.kklisura.cdt.protocol.v2023.events.debugger;

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

import com.github.kklisura.cdt.protocol.v2023.types.debugger.Location;

/** Fired when breakpoint is resolved to an actual script and location. */
public class BreakpointResolved {

  private String breakpointId;

  private Location location;

  /** Breakpoint unique identifier. */
  public String getBreakpointId() {
    return breakpointId;
  }

  /** Breakpoint unique identifier. */
  public void setBreakpointId(String breakpointId) {
    this.breakpointId = breakpointId;
  }

  /** Actual breakpoint location. */
  public Location getLocation() {
    return location;
  }

  /** Actual breakpoint location. */
  public void setLocation(Location location) {
    this.location = location;
  }
}
