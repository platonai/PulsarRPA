package com.github.kklisura.cdt.protocol.v2023.types.animation;

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

/** Keyframe Style */
public class KeyframeStyle {

  private String offset;

  private String easing;

  /** Keyframe's time offset. */
  public String getOffset() {
    return offset;
  }

  /** Keyframe's time offset. */
  public void setOffset(String offset) {
    this.offset = offset;
  }

  /** `AnimationEffect`'s timing function. */
  public String getEasing() {
    return easing;
  }

  /** `AnimationEffect`'s timing function. */
  public void setEasing(String easing) {
    this.easing = easing;
  }
}
