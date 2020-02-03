package com.github.kklisura.cdt.protocol.types.headlessexperimental;

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

public class BeginFrame {

  private Boolean hasDamage;

  @Optional private String screenshotData;

  /**
   * Whether the BeginFrame resulted in damage and, thus, a new frame was committed to the display.
   * Reported for diagnostic uses, may be removed in the future.
   */
  public Boolean getHasDamage() {
    return hasDamage;
  }

  /**
   * Whether the BeginFrame resulted in damage and, thus, a new frame was committed to the display.
   * Reported for diagnostic uses, may be removed in the future.
   */
  public void setHasDamage(Boolean hasDamage) {
    this.hasDamage = hasDamage;
  }

  /** Base64-encoded image data of the screenshot, if one was requested and successfully taken. */
  public String getScreenshotData() {
    return screenshotData;
  }

  /** Base64-encoded image data of the screenshot, if one was requested and successfully taken. */
  public void setScreenshotData(String screenshotData) {
    this.screenshotData = screenshotData;
  }
}
