package com.github.kklisura.cdt.protocol.types.page;

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

/** Information about the Frame hierarchy. */
public class FrameTree {

  private Frame frame;

  @Optional private List<FrameTree> childFrames;

  /** Frame information for this tree item. */
  public Frame getFrame() {
    return frame;
  }

  /** Frame information for this tree item. */
  public void setFrame(Frame frame) {
    this.frame = frame;
  }

  /** Child frames. */
  public List<FrameTree> getChildFrames() {
    return childFrames;
  }

  /** Child frames. */
  public void setChildFrames(List<FrameTree> childFrames) {
    this.childFrames = childFrames;
  }
}
