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

import com.github.kklisura.cdt.protocol.v2023.events.browser.DownloadProgress;
import com.github.kklisura.cdt.protocol.v2023.events.browser.DownloadWillBegin;
import com.github.kklisura.cdt.protocol.v2023.support.annotations.*;
import com.github.kklisura.cdt.protocol.v2023.support.types.EventHandler;
import com.github.kklisura.cdt.protocol.v2023.support.types.EventListener;
import com.github.kklisura.cdt.protocol.v2023.types.browser.*;

import java.util.List;

/** The Browser domain defines methods and events for browser managing. */
public interface Browser {

  /**
   * Set permission settings for given origin.
   *
   * @param permission Descriptor of permission to override.
   * @param setting Setting of the permission.
   */
  @Experimental
  void setPermission(
      @ParamName("permission") PermissionDescriptor permission,
      @ParamName("setting") PermissionSetting setting);

  /**
   * Set permission settings for given origin.
   *
   * @param permission Descriptor of permission to override.
   * @param setting Setting of the permission.
   * @param origin Origin the permission applies to, all origins if not specified.
   * @param browserContextId Context to override. When omitted, default browser context is used.
   */
  @Experimental
  void setPermission(
      @ParamName("permission") PermissionDescriptor permission,
      @ParamName("setting") PermissionSetting setting,
      @Optional @ParamName("origin") String origin,
      @Optional @ParamName("browserContextId") String browserContextId);

  /**
   * Grant specific permissions to the given origin and reject all others.
   *
   * @param permissions
   */
  @Experimental
  void grantPermissions(@ParamName("permissions") List<PermissionType> permissions);

  /**
   * Grant specific permissions to the given origin and reject all others.
   *
   * @param permissions
   * @param origin Origin the permission applies to, all origins if not specified.
   * @param browserContextId BrowserContext to override permissions. When omitted, default browser
   *     context is used.
   */
  @Experimental
  void grantPermissions(
      @ParamName("permissions") List<PermissionType> permissions,
      @Optional @ParamName("origin") String origin,
      @Optional @ParamName("browserContextId") String browserContextId);

  /** Reset all permission management for all origins. */
  @Experimental
  void resetPermissions();

  /**
   * Reset all permission management for all origins.
   *
   * @param browserContextId BrowserContext to reset permissions. When omitted, default browser
   *     context is used.
   */
  @Experimental
  void resetPermissions(@Optional @ParamName("browserContextId") String browserContextId);

  /**
   * Set the behavior when downloading a file.
   *
   * @param behavior Whether to allow all or deny all download requests, or use default Chrome
   *     behavior if available (otherwise deny). |allowAndName| allows download and names files
   *     according to their dowmload guids.
   */
  @Experimental
  void setDownloadBehavior(@ParamName("behavior") SetDownloadBehaviorBehavior behavior);

  /**
   * Set the behavior when downloading a file.
   *
   * @param behavior Whether to allow all or deny all download requests, or use default Chrome
   *     behavior if available (otherwise deny). |allowAndName| allows download and names files
   *     according to their dowmload guids.
   * @param browserContextId BrowserContext to set download behavior. When omitted, default browser
   *     context is used.
   * @param downloadPath The default path to save downloaded files to. This is required if behavior
   *     is set to 'allow' or 'allowAndName'.
   * @param eventsEnabled Whether to emit download events (defaults to false).
   */
  @Experimental
  void setDownloadBehavior(
      @ParamName("behavior") SetDownloadBehaviorBehavior behavior,
      @Optional @ParamName("browserContextId") String browserContextId,
      @Optional @ParamName("downloadPath") String downloadPath,
      @Optional @ParamName("eventsEnabled") Boolean eventsEnabled);

  /**
   * Cancel a download if in progress
   *
   * @param guid Global unique identifier of the download.
   */
  @Experimental
  void cancelDownload(@ParamName("guid") String guid);

  /**
   * Cancel a download if in progress
   *
   * @param guid Global unique identifier of the download.
   * @param browserContextId BrowserContext to perform the action in. When omitted, default browser
   *     context is used.
   */
  @Experimental
  void cancelDownload(
      @ParamName("guid") String guid,
      @Optional @ParamName("browserContextId") String browserContextId);

  /** Close browser gracefully. */
  void close();

  /** Crashes browser on the main thread. */
  @Experimental
  void crash();

  /** Crashes GPU process. */
  @Experimental
  void crashGpuProcess();

  /** Returns version information. */
  Version getVersion();

  /**
   * Returns the command line switches for the browser process if, and only if --enable-automation
   * is on the commandline.
   */
  @Experimental
  @Returns("arguments")
  @ReturnTypeParameter(String.class)
  List<String> getBrowserCommandLine();

  /** Get Chrome histograms. */
  @Experimental
  @Returns("histograms")
  @ReturnTypeParameter(Histogram.class)
  List<Histogram> getHistograms();

  /**
   * Get Chrome histograms.
   *
   * @param query Requested substring in name. Only histograms which have query as a substring in
   *     their name are extracted. An empty or absent query returns all histograms.
   * @param delta If true, retrieve delta since last delta call.
   */
  @Experimental
  @Returns("histograms")
  @ReturnTypeParameter(Histogram.class)
  List<Histogram> getHistograms(
      @Optional @ParamName("query") String query, @Optional @ParamName("delta") Boolean delta);

  /**
   * Get a Chrome histogram by name.
   *
   * @param name Requested histogram name.
   */
  @Experimental
  @Returns("histogram")
  Histogram getHistogram(@ParamName("name") String name);

  /**
   * Get a Chrome histogram by name.
   *
   * @param name Requested histogram name.
   * @param delta If true, retrieve delta since last delta call.
   */
  @Experimental
  @Returns("histogram")
  Histogram getHistogram(
      @ParamName("name") String name, @Optional @ParamName("delta") Boolean delta);

  /**
   * Get position and size of the browser window.
   *
   * @param windowId Browser window id.
   */
  @Experimental
  @Returns("bounds")
  Bounds getWindowBounds(@ParamName("windowId") Integer windowId);

  /** Get the browser window that contains the devtools target. */
  @Experimental
  WindowForTarget getWindowForTarget();

  /**
   * Get the browser window that contains the devtools target.
   *
   * @param targetId Devtools agent host id. If called as a part of the session, associated targetId
   *     is used.
   */
  @Experimental
  WindowForTarget getWindowForTarget(@Optional @ParamName("targetId") String targetId);

  /**
   * Set position and/or size of the browser window.
   *
   * @param windowId Browser window id.
   * @param bounds New window bounds. The 'minimized', 'maximized' and 'fullscreen' states cannot be
   *     combined with 'left', 'top', 'width' or 'height'. Leaves unspecified fields unchanged.
   */
  @Experimental
  void setWindowBounds(@ParamName("windowId") Integer windowId, @ParamName("bounds") Bounds bounds);

  /** Set dock tile details, platform-specific. */
  @Experimental
  void setDockTile();

  /**
   * Set dock tile details, platform-specific.
   *
   * @param badgeLabel
   * @param image Png encoded image. (Encoded as a base64 string when passed over JSON)
   */
  @Experimental
  void setDockTile(
      @Optional @ParamName("badgeLabel") String badgeLabel,
      @Optional @ParamName("image") String image);

  /**
   * Invoke custom browser commands used by telemetry.
   *
   * @param commandId
   */
  @Experimental
  void executeBrowserCommand(@ParamName("commandId") BrowserCommandId commandId);

  /**
   * Allows a site to use privacy sandbox features that require enrollment without the site actually
   * being enrolled. Only supported on page targets.
   *
   * @param url
   */
  void addPrivacySandboxEnrollmentOverride(@ParamName("url") String url);

  /** Fired when page is about to start a download. */
  @EventName("downloadWillBegin")
  @Experimental
  EventListener onDownloadWillBegin(EventHandler<DownloadWillBegin> eventListener);

  /** Fired when download makes progress. Last call has |done| == true. */
  @EventName("downloadProgress")
  @Experimental
  EventListener onDownloadProgress(EventHandler<DownloadProgress> eventListener);
}
