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

import java.util.List;

public class SetBreakpointByUrl {

  private String breakpointId;

  private List<Location> locations;

  /** Id of the created breakpoint for further reference. */
  public String getBreakpointId() {
    return breakpointId;
  }

  /** Id of the created breakpoint for further reference. */
  public void setBreakpointId(String breakpointId) {
    this.breakpointId = breakpointId;
  }

  /** List of the locations this breakpoint resolved into upon addition. */
  public List<Location> getLocations() {
    return locations;
  }

  /** List of the locations this breakpoint resolved into upon addition. */
  public void setLocations(List<Location> locations) {
    this.locations = locations;
  }
}
