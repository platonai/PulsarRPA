package com.github.kklisura.cdt.protocol.v2023.types.systeminfo;

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

/**
 * Describes a supported video encoding profile with its associated maximum resolution and maximum
 * framerate.
 */
public class VideoEncodeAcceleratorCapability {

  private String profile;

  private Size maxResolution;

  private Integer maxFramerateNumerator;

  private Integer maxFramerateDenominator;

  /** Video codec profile that is supported, e.g H264 Main. */
  public String getProfile() {
    return profile;
  }

  /** Video codec profile that is supported, e.g H264 Main. */
  public void setProfile(String profile) {
    this.profile = profile;
  }

  /** Maximum video dimensions in pixels supported for this |profile|. */
  public Size getMaxResolution() {
    return maxResolution;
  }

  /** Maximum video dimensions in pixels supported for this |profile|. */
  public void setMaxResolution(Size maxResolution) {
    this.maxResolution = maxResolution;
  }

  /**
   * Maximum encoding framerate in frames per second supported for this |profile|, as fraction's
   * numerator and denominator, e.g. 24/1 fps, 24000/1001 fps, etc.
   */
  public Integer getMaxFramerateNumerator() {
    return maxFramerateNumerator;
  }

  /**
   * Maximum encoding framerate in frames per second supported for this |profile|, as fraction's
   * numerator and denominator, e.g. 24/1 fps, 24000/1001 fps, etc.
   */
  public void setMaxFramerateNumerator(Integer maxFramerateNumerator) {
    this.maxFramerateNumerator = maxFramerateNumerator;
  }

  public Integer getMaxFramerateDenominator() {
    return maxFramerateDenominator;
  }

  public void setMaxFramerateDenominator(Integer maxFramerateDenominator) {
    this.maxFramerateDenominator = maxFramerateDenominator;
  }
}
