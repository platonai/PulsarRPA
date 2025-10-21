package ai.platon.cdt.kt.protocol.commands

import ai.platon.cdt.kt.protocol.events.browser.DownloadProgress
import ai.platon.cdt.kt.protocol.events.browser.DownloadWillBegin
import ai.platon.cdt.kt.protocol.support.annotations.EventName
import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.support.annotations.ParamName
import ai.platon.cdt.kt.protocol.support.annotations.ReturnTypeParameter
import ai.platon.cdt.kt.protocol.support.annotations.Returns
import ai.platon.cdt.kt.protocol.support.types.EventHandler
import ai.platon.cdt.kt.protocol.support.types.EventListener
import ai.platon.cdt.kt.protocol.types.browser.Bounds
import ai.platon.cdt.kt.protocol.types.browser.BrowserCommandId
import ai.platon.cdt.kt.protocol.types.browser.Histogram
import ai.platon.cdt.kt.protocol.types.browser.PermissionDescriptor
import ai.platon.cdt.kt.protocol.types.browser.PermissionSetting
import ai.platon.cdt.kt.protocol.types.browser.PermissionType
import ai.platon.cdt.kt.protocol.types.browser.SetDownloadBehaviorBehavior
import ai.platon.cdt.kt.protocol.types.browser.Version
import ai.platon.cdt.kt.protocol.types.browser.WindowForTarget
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.Unit
import kotlin.collections.List

/**
 * The Browser domain defines methods and events for browser managing.
 */
public interface Browser {
  /**
   * Set permission settings for given origin.
   * @param permission Descriptor of permission to override.
   * @param setting Setting of the permission.
   * @param origin Origin the permission applies to, all origins if not specified.
   * @param browserContextId Context to override. When omitted, default browser context is used.
   */
  @Experimental
  public suspend fun setPermission(
    @ParamName("permission") permission: PermissionDescriptor,
    @ParamName("setting") setting: PermissionSetting,
    @ParamName("origin") @Optional origin: String?,
    @ParamName("browserContextId") @Optional browserContextId: String?,
  )

  @Experimental
  public suspend fun setPermission(@ParamName("permission") permission: PermissionDescriptor,
      @ParamName("setting") setting: PermissionSetting) {
    return setPermission(permission, setting, null, null)
  }

  /**
   * Grant specific permissions to the given origin and reject all others.
   * @param permissions
   * @param origin Origin the permission applies to, all origins if not specified.
   * @param browserContextId BrowserContext to override permissions. When omitted, default browser
   * context is used.
   */
  @Experimental
  public suspend fun grantPermissions(
    @ParamName("permissions") permissions: List<PermissionType>,
    @ParamName("origin") @Optional origin: String?,
    @ParamName("browserContextId") @Optional browserContextId: String?,
  )

  @Experimental
  public suspend fun grantPermissions(@ParamName("permissions") permissions: List<PermissionType>) {
    return grantPermissions(permissions, null, null)
  }

  /**
   * Reset all permission management for all origins.
   * @param browserContextId BrowserContext to reset permissions. When omitted, default browser
   * context is used.
   */
  @Experimental
  public suspend fun resetPermissions(@ParamName("browserContextId") @Optional
      browserContextId: String?)

  @Experimental
  public suspend fun resetPermissions() {
    return resetPermissions(null)
  }

  /**
   * Set the behavior when downloading a file.
   * @param behavior Whether to allow all or deny all download requests, or use default Chrome
   * behavior if
   * available (otherwise deny). |allowAndName| allows download and names files according to
   * their dowmload guids.
   * @param browserContextId BrowserContext to set download behavior. When omitted, default browser
   * context is used.
   * @param downloadPath The default path to save downloaded files to. This is required if behavior
   * is set to 'allow'
   * or 'allowAndName'.
   * @param eventsEnabled Whether to emit download events (defaults to false).
   */
  @Experimental
  public suspend fun setDownloadBehavior(
    @ParamName("behavior") behavior: SetDownloadBehaviorBehavior,
    @ParamName("browserContextId") @Optional browserContextId: String?,
    @ParamName("downloadPath") @Optional downloadPath: String?,
    @ParamName("eventsEnabled") @Optional eventsEnabled: Boolean?,
  )

  @Experimental
  public suspend fun setDownloadBehavior(@ParamName("behavior")
      behavior: SetDownloadBehaviorBehavior) {
    return setDownloadBehavior(behavior, null, null, null)
  }

  /**
   * Cancel a download if in progress
   * @param guid Global unique identifier of the download.
   * @param browserContextId BrowserContext to perform the action in. When omitted, default browser
   * context is used.
   */
  @Experimental
  public suspend fun cancelDownload(@ParamName("guid") guid: String, @ParamName("browserContextId")
      @Optional browserContextId: String?)

  @Experimental
  public suspend fun cancelDownload(@ParamName("guid") guid: String) {
    return cancelDownload(guid, null)
  }

  /**
   * Close browser gracefully.
   */
  public suspend fun close()

  /**
   * Crashes browser on the main thread.
   */
  @Experimental
  public suspend fun crash()

  /**
   * Crashes GPU process.
   */
  @Experimental
  public suspend fun crashGpuProcess()

  /**
   * Returns version information.
   */
  public suspend fun getVersion(): Version

  /**
   * Returns the command line switches for the browser process if, and only if
   * --enable-automation is on the commandline.
   */
  @Experimental
  @Returns("arguments")
  @ReturnTypeParameter(String::class)
  public suspend fun getBrowserCommandLine(): List<String>

  /**
   * Get Chrome histograms.
   * @param query Requested substring in name. Only histograms which have query as a
   * substring in their name are extracted. An empty or absent query returns
   * all histograms.
   * @param delta If true, retrieve delta since last call.
   */
  @Experimental
  @Returns("histograms")
  @ReturnTypeParameter(Histogram::class)
  public suspend fun getHistograms(@ParamName("query") @Optional query: String?, @ParamName("delta")
      @Optional delta: Boolean?): List<Histogram>

  @Experimental
  @Returns("histograms")
  @ReturnTypeParameter(Histogram::class)
  public suspend fun getHistograms(): List<Histogram> {
    return getHistograms(null, null)
  }

  /**
   * Get a Chrome histogram by name.
   * @param name Requested histogram name.
   * @param delta If true, retrieve delta since last call.
   */
  @Experimental
  @Returns("histogram")
  public suspend fun getHistogram(@ParamName("name") name: String, @ParamName("delta") @Optional
      delta: Boolean?): Histogram

  @Experimental
  @Returns("histogram")
  public suspend fun getHistogram(@ParamName("name") name: String): Histogram {
    return getHistogram(name, null)
  }

  /**
   * Get position and size of the browser window.
   * @param windowId Browser window id.
   */
  @Experimental
  @Returns("bounds")
  public suspend fun getWindowBounds(@ParamName("windowId") windowId: Int): Bounds

  /**
   * Get the browser window that contains the devtools target.
   * @param targetId Devtools agent host id. If called as a part of the session, associated targetId
   * is used.
   */
  @Experimental
  public suspend fun getWindowForTarget(@ParamName("targetId") @Optional targetId: String?):
      WindowForTarget

  @Experimental
  public suspend fun getWindowForTarget(): WindowForTarget {
    return getWindowForTarget(null)
  }

  /**
   * Set position and/or size of the browser window.
   * @param windowId Browser window id.
   * @param bounds New window bounds. The 'minimized', 'maximized' and 'fullscreen' states cannot be
   * combined
   * with 'left', 'top', 'width' or 'height'. Leaves unspecified fields unchanged.
   */
  @Experimental
  public suspend fun setWindowBounds(@ParamName("windowId") windowId: Int, @ParamName("bounds")
      bounds: Bounds)

  /**
   * Set dock tile details, platform-specific.
   * @param badgeLabel
   * @param image Png encoded image. (Encoded as a base64 string when passed over JSON)
   */
  @Experimental
  public suspend fun setDockTile(@ParamName("badgeLabel") @Optional badgeLabel: String?,
      @ParamName("image") @Optional image: String?)

  @Experimental
  public suspend fun setDockTile() {
    return setDockTile(null, null)
  }

  /**
   * Invoke custom browser commands used by telemetry.
   * @param commandId
   */
  @Experimental
  public suspend fun executeBrowserCommand(@ParamName("commandId") commandId: BrowserCommandId)

  @EventName("downloadWillBegin")
  @Experimental
  public fun onDownloadWillBegin(eventListener: EventHandler<DownloadWillBegin>): EventListener

  @EventName("downloadWillBegin")
  @Experimental
  public fun onDownloadWillBegin(eventListener: suspend (DownloadWillBegin) -> Unit): EventListener

  @EventName("downloadProgress")
  @Experimental
  public fun onDownloadProgress(eventListener: EventHandler<DownloadProgress>): EventListener

  @EventName("downloadProgress")
  @Experimental
  public fun onDownloadProgress(eventListener: suspend (DownloadProgress) -> Unit): EventListener
}
