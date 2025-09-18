package com.github.kklisura.cdt.protocol.v2023.types.headlessexperimental;

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

/** Encoding options for a screenshot. */
public class ScreenshotParams {

  @Optional
  private ScreenshotParamsFormat format;

  @Optional private Integer quality;

  @Optional private Boolean optimizeForSpeed;

  /** Image compression format (defaults to png). */
  public ScreenshotParamsFormat getFormat() {
    return format;
  }

  /** Image compression format (defaults to png). */
  public void setFormat(ScreenshotParamsFormat format) {
    this.format = format;
  }

  /** Compression quality from range [0..100] (jpeg and webp only). */
  public Integer getQuality() {
    return quality;
  }

  /** Compression quality from range [0..100] (jpeg and webp only). */
  public void setQuality(Integer quality) {
    this.quality = quality;
  }

  /** Optimize image encoding for speed, not for resulting size (defaults to false) */
  public Boolean getOptimizeForSpeed() {
    return optimizeForSpeed;
  }

  /** Optimize image encoding for speed, not for resulting size (defaults to false) */
  public void setOptimizeForSpeed(Boolean optimizeForSpeed) {
    this.optimizeForSpeed = optimizeForSpeed;
  }
}
