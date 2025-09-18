package com.github.kklisura.cdt.protocol.v2023.events.profiler;

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

import com.github.kklisura.cdt.protocol.v2023.support.annotations.Optional;
import com.github.kklisura.cdt.protocol.v2023.types.debugger.Location;

/** Sent when new profile recording is started using console.profile() call. */
public class ConsoleProfileStarted {

  private String id;

  private Location location;

  @Optional
  private String title;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  /** Location of console.profile(). */
  public Location getLocation() {
    return location;
  }

  /** Location of console.profile(). */
  public void setLocation(Location location) {
    this.location = location;
  }

  /** Profile title passed as an argument to console.profile(). */
  public String getTitle() {
    return title;
  }

  /** Profile title passed as an argument to console.profile(). */
  public void setTitle(String title) {
    this.title = title;
  }
}
