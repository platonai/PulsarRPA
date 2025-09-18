package com.github.kklisura.cdt.protocol.v2023.commands;

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

import com.github.kklisura.cdt.protocol.v2023.events.emulation.VirtualTimeBudgetExpired;
import com.github.kklisura.cdt.protocol.v2023.support.annotations.*;
import com.github.kklisura.cdt.protocol.v2023.support.types.EventHandler;
import com.github.kklisura.cdt.protocol.v2023.support.types.EventListener;
import com.github.kklisura.cdt.protocol.v2023.types.dom.RGBA;
import com.github.kklisura.cdt.protocol.v2023.types.emulation.*;
import com.github.kklisura.cdt.protocol.v2023.types.page.Viewport;

import java.util.List;

/** This domain emulates different environments for the page. */
public interface Emulation {

  /** Tells whether emulation is supported. */
  @Returns("result")
  Boolean canEmulate();

  /** Clears the overridden device metrics. */
  void clearDeviceMetricsOverride();

  /** Clears the overridden Geolocation Position and Error. */
  void clearGeolocationOverride();

  /** Requests that page scale factor is reset to initial values. */
  @Experimental
  void resetPageScaleFactor();

  /**
   * Enables or disables simulating a focused and active page.
   *
   * @param enabled Whether to enable to disable focus emulation.
   */
  @Experimental
  void setFocusEmulationEnabled(@ParamName("enabled") Boolean enabled);

  /** Automatically render all web contents using a dark theme. */
  @Experimental
  void setAutoDarkModeOverride();

  /**
   * Automatically render all web contents using a dark theme.
   *
   * @param enabled Whether to enable or disable automatic dark mode. If not specified, any existing
   *     override will be cleared.
   */
  @Experimental
  void setAutoDarkModeOverride(@Optional @ParamName("enabled") Boolean enabled);

  /**
   * Enables CPU throttling to emulate slow CPUs.
   *
   * @param rate Throttling rate as a slowdown factor (1 is no throttle, 2 is 2x slowdown, etc).
   */
  @Experimental
  void setCPUThrottlingRate(@ParamName("rate") Double rate);

  /**
   * Sets or clears an override of the default background color of the frame. This override is used
   * if the content does not specify one.
   */
  void setDefaultBackgroundColorOverride();

  /**
   * Sets or clears an override of the default background color of the frame. This override is used
   * if the content does not specify one.
   *
   * @param color RGBA of the default background color. If not specified, any existing override will
   *     be cleared.
   */
  void setDefaultBackgroundColorOverride(@Optional @ParamName("color") RGBA color);

  /**
   * Overrides the values of device screen dimensions (window.screen.width, window.screen.height,
   * window.innerWidth, window.innerHeight, and "device-width"/"device-height"-related CSS media
   * query results).
   *
   * @param width Overriding width value in pixels (minimum 0, maximum 10000000). 0 disables the
   *     override.
   * @param height Overriding height value in pixels (minimum 0, maximum 10000000). 0 disables the
   *     override.
   * @param deviceScaleFactor Overriding device scale factor value. 0 disables the override.
   * @param mobile Whether to emulate mobile device. This includes viewport meta tag, overlay
   *     scrollbars, text autosizing and more.
   */
  void setDeviceMetricsOverride(
      @ParamName("width") Integer width,
      @ParamName("height") Integer height,
      @ParamName("deviceScaleFactor") Double deviceScaleFactor,
      @ParamName("mobile") Boolean mobile);

  /**
   * Overrides the values of device screen dimensions (window.screen.width, window.screen.height,
   * window.innerWidth, window.innerHeight, and "device-width"/"device-height"-related CSS media
   * query results).
   *
   * @param width Overriding width value in pixels (minimum 0, maximum 10000000). 0 disables the
   *     override.
   * @param height Overriding height value in pixels (minimum 0, maximum 10000000). 0 disables the
   *     override.
   * @param deviceScaleFactor Overriding device scale factor value. 0 disables the override.
   * @param mobile Whether to emulate mobile device. This includes viewport meta tag, overlay
   *     scrollbars, text autosizing and more.
   * @param scale Scale to apply to resulting view image.
   * @param screenWidth Overriding screen width value in pixels (minimum 0, maximum 10000000).
   * @param screenHeight Overriding screen height value in pixels (minimum 0, maximum 10000000).
   * @param positionX Overriding view X position on screen in pixels (minimum 0, maximum 10000000).
   * @param positionY Overriding view Y position on screen in pixels (minimum 0, maximum 10000000).
   * @param dontSetVisibleSize Do not set visible view size, rely upon explicit setVisibleSize call.
   * @param screenOrientation Screen orientation override.
   * @param viewport If set, the visible area of the page will be overridden to this viewport. This
   *     viewport change is not observed by the page, e.g. viewport-relative elements do not change
   *     positions.
   * @param displayFeature If set, the display feature of a multi-segment screen. If not set,
   *     multi-segment support is turned-off.
   */
  void setDeviceMetricsOverride(
      @ParamName("width") Integer width,
      @ParamName("height") Integer height,
      @ParamName("deviceScaleFactor") Double deviceScaleFactor,
      @ParamName("mobile") Boolean mobile,
      @Experimental @Optional @ParamName("scale") Double scale,
      @Experimental @Optional @ParamName("screenWidth") Integer screenWidth,
      @Experimental @Optional @ParamName("screenHeight") Integer screenHeight,
      @Experimental @Optional @ParamName("positionX") Integer positionX,
      @Experimental @Optional @ParamName("positionY") Integer positionY,
      @Experimental @Optional @ParamName("dontSetVisibleSize") Boolean dontSetVisibleSize,
      @Optional @ParamName("screenOrientation") ScreenOrientation screenOrientation,
      @Experimental @Optional @ParamName("viewport") Viewport viewport,
      @Experimental @Optional @ParamName("displayFeature") DisplayFeature displayFeature);

  /** @param hidden Whether scrollbars should be always hidden. */
  @Experimental
  void setScrollbarsHidden(@ParamName("hidden") Boolean hidden);

  /** @param disabled Whether document.coookie API should be disabled. */
  @Experimental
  void setDocumentCookieDisabled(@ParamName("disabled") Boolean disabled);

  /** @param enabled Whether touch emulation based on mouse input should be enabled. */
  @Experimental
  void setEmitTouchEventsForMouse(@ParamName("enabled") Boolean enabled);

  /**
   * @param enabled Whether touch emulation based on mouse input should be enabled.
   * @param configuration Touch/gesture events configuration. Default: current platform.
   */
  @Experimental
  void setEmitTouchEventsForMouse(
      @ParamName("enabled") Boolean enabled,
      @Optional @ParamName("configuration") SetEmitTouchEventsForMouseConfiguration configuration);

  /** Emulates the given media type or media feature for CSS media queries. */
  void setEmulatedMedia();

  /**
   * Emulates the given media type or media feature for CSS media queries.
   *
   * @param media Media type to emulate. Empty string disables the override.
   * @param features Media features to emulate.
   */
  void setEmulatedMedia(
      @Optional @ParamName("media") String media,
      @Optional @ParamName("features") List<MediaFeature> features);

  /**
   * Emulates the given vision deficiency.
   *
   * @param type Vision deficiency to emulate. Order: best-effort emulations come first, followed by
   *     any physiologically accurate emulations for medically recognized color vision deficiencies.
   */
  @Experimental
  void setEmulatedVisionDeficiency(@ParamName("type") SetEmulatedVisionDeficiencyType type);

  /**
   * Overrides the Geolocation Position or Error. Omitting any of the parameters emulates position
   * unavailable.
   */
  void setGeolocationOverride();

  /**
   * Overrides the Geolocation Position or Error. Omitting any of the parameters emulates position
   * unavailable.
   *
   * @param latitude Mock latitude
   * @param longitude Mock longitude
   * @param accuracy Mock accuracy
   */
  void setGeolocationOverride(
      @Optional @ParamName("latitude") Double latitude,
      @Optional @ParamName("longitude") Double longitude,
      @Optional @ParamName("accuracy") Double accuracy);

  /**
   * Overrides the Idle state.
   *
   * @param isUserActive Mock isUserActive
   * @param isScreenUnlocked Mock isScreenUnlocked
   */
  @Experimental
  void setIdleOverride(
      @ParamName("isUserActive") Boolean isUserActive,
      @ParamName("isScreenUnlocked") Boolean isScreenUnlocked);

  /** Clears Idle state overrides. */
  @Experimental
  void clearIdleOverride();

  /**
   * Overrides value returned by the javascript navigator object.
   *
   * @param platform The platform navigator.platform should return.
   */
  @Deprecated
  @Experimental
  void setNavigatorOverrides(@ParamName("platform") String platform);

  /**
   * Sets a specified page scale factor.
   *
   * @param pageScaleFactor Page scale factor.
   */
  @Experimental
  void setPageScaleFactor(@ParamName("pageScaleFactor") Double pageScaleFactor);

  /**
   * Switches script execution in the page.
   *
   * @param value Whether script execution should be disabled in the page.
   */
  void setScriptExecutionDisabled(@ParamName("value") Boolean value);

  /**
   * Enables touch on platforms which do not support them.
   *
   * @param enabled Whether the touch event emulation should be enabled.
   */
  void setTouchEmulationEnabled(@ParamName("enabled") Boolean enabled);

  /**
   * Enables touch on platforms which do not support them.
   *
   * @param enabled Whether the touch event emulation should be enabled.
   * @param maxTouchPoints Maximum touch points supported. Defaults to one.
   */
  void setTouchEmulationEnabled(
      @ParamName("enabled") Boolean enabled,
      @Optional @ParamName("maxTouchPoints") Integer maxTouchPoints);

  /**
   * Turns on virtual time for all frames (replacing real-time with a synthetic time source) and
   * sets the current virtual time policy. Note this supersedes any previous time budget.
   *
   * @param policy
   */
  @Experimental
  @Returns("virtualTimeTicksBase")
  Double setVirtualTimePolicy(@ParamName("policy") VirtualTimePolicy policy);

  /**
   * Turns on virtual time for all frames (replacing real-time with a synthetic time source) and
   * sets the current virtual time policy. Note this supersedes any previous time budget.
   *
   * @param policy
   * @param budget If set, after this many virtual milliseconds have elapsed virtual time will be
   *     paused and a virtualTimeBudgetExpired event is sent.
   * @param maxVirtualTimeTaskStarvationCount If set this specifies the maximum number of tasks that
   *     can be run before virtual is forced forwards to prevent deadlock.
   * @param initialVirtualTime If set, base::Time::Now will be overridden to initially return this
   *     value.
   */
  @Experimental
  @Returns("virtualTimeTicksBase")
  Double setVirtualTimePolicy(
      @ParamName("policy") VirtualTimePolicy policy,
      @Optional @ParamName("budget") Double budget,
      @Optional @ParamName("maxVirtualTimeTaskStarvationCount")
          Integer maxVirtualTimeTaskStarvationCount,
      @Optional @ParamName("initialVirtualTime") Double initialVirtualTime);

  /** Overrides default host system locale with the specified one. */
  @Experimental
  void setLocaleOverride();

  /**
   * Overrides default host system locale with the specified one.
   *
   * @param locale ICU style C locale (e.g. "en_US"). If not specified or empty, disables the
   *     override and restores default host system locale.
   */
  @Experimental
  void setLocaleOverride(@Optional @ParamName("locale") String locale);

  /**
   * Overrides default host system timezone with the specified one.
   *
   * @param timezoneId The timezone identifier. If empty, disables the override and restores default
   *     host system timezone.
   */
  @Experimental
  void setTimezoneOverride(@ParamName("timezoneId") String timezoneId);

  /**
   * Resizes the frame/viewport of the page. Note that this does not affect the frame's container
   * (e.g. browser window). Can be used to produce screenshots of the specified size. Not supported
   * on Android.
   *
   * @param width Frame width (DIP).
   * @param height Frame height (DIP).
   */
  @Deprecated
  @Experimental
  void setVisibleSize(@ParamName("width") Integer width, @ParamName("height") Integer height);

  /** @param imageTypes Image types to disable. */
  @Experimental
  void setDisabledImageTypes(@ParamName("imageTypes") List<DisabledImageType> imageTypes);

  /** @param hardwareConcurrency Hardware concurrency to report */
  @Experimental
  void setHardwareConcurrencyOverride(
      @ParamName("hardwareConcurrency") Integer hardwareConcurrency);

  /**
   * Allows overriding user agent with the given string.
   *
   * @param userAgent User agent to use.
   */
  void setUserAgentOverride(@ParamName("userAgent") String userAgent);

  /**
   * Allows overriding user agent with the given string.
   *
   * @param userAgent User agent to use.
   * @param acceptLanguage Browser langugage to emulate.
   * @param platform The platform navigator.platform should return.
   * @param userAgentMetadata To be sent in Sec-CH-UA-* headers and returned in
   *     navigator.userAgentData
   */
  void setUserAgentOverride(
      @ParamName("userAgent") String userAgent,
      @Optional @ParamName("acceptLanguage") String acceptLanguage,
      @Optional @ParamName("platform") String platform,
      @Experimental @Optional @ParamName("userAgentMetadata") UserAgentMetadata userAgentMetadata);

  /**
   * Allows overriding the automation flag.
   *
   * @param enabled Whether the override should be enabled.
   */
  @Experimental
  void setAutomationOverride(@ParamName("enabled") Boolean enabled);

  /**
   * Notification sent after the virtual time budget for the current VirtualTimePolicy has run out.
   */
  @EventName("virtualTimeBudgetExpired")
  @Experimental
  EventListener onVirtualTimeBudgetExpired(EventHandler<VirtualTimeBudgetExpired> eventListener);
}
