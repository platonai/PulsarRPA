package com.github.kklisura.cdt.protocol.events.headlessexperimental;

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

/** Issued when the target starts or stops needing BeginFrames. */
public class NeedsBeginFramesChanged {

  private Boolean needsBeginFrames;

  /** True if BeginFrames are needed, false otherwise. */
  public Boolean getNeedsBeginFrames() {
    return needsBeginFrames;
  }

  /** True if BeginFrames are needed, false otherwise. */
  public void setNeedsBeginFrames(Boolean needsBeginFrames) {
    this.needsBeginFrames = needsBeginFrames;
  }
}
