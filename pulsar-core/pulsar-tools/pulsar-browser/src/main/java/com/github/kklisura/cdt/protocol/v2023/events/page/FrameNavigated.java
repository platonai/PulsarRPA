package com.github.kklisura.cdt.protocol.v2023.events.page;

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
import com.github.kklisura.cdt.protocol.v2023.types.page.Frame;
import com.github.kklisura.cdt.protocol.v2023.types.page.NavigationType;

/**
 * Fired once navigation of the frame has completed. Frame is now associated with the new loader.
 */
public class FrameNavigated {

  private Frame frame;

  @Experimental
  private NavigationType type;

  /** Frame object. */
  public Frame getFrame() {
    return frame;
  }

  /** Frame object. */
  public void setFrame(Frame frame) {
    this.frame = frame;
  }

  public NavigationType getType() {
    return type;
  }

  public void setType(NavigationType type) {
    this.type = type;
  }
}
