@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.commands

import ai.platon.cdt.kt.protocol.events.emulation.VirtualTimeBudgetExpired
import ai.platon.cdt.kt.protocol.support.annotations.EventName
import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.support.annotations.ParamName
import ai.platon.cdt.kt.protocol.support.annotations.Returns
import ai.platon.cdt.kt.protocol.support.types.EventHandler
import ai.platon.cdt.kt.protocol.support.types.EventListener
import ai.platon.cdt.kt.protocol.types.dom.RGBA
import ai.platon.cdt.kt.protocol.types.emulation.DisabledImageType
import ai.platon.cdt.kt.protocol.types.emulation.DisplayFeature
import ai.platon.cdt.kt.protocol.types.emulation.MediaFeature
import ai.platon.cdt.kt.protocol.types.emulation.ScreenOrientation
import ai.platon.cdt.kt.protocol.types.emulation.SetEmitTouchEventsForMouseConfiguration
import ai.platon.cdt.kt.protocol.types.emulation.SetEmulatedVisionDeficiencyType
import ai.platon.cdt.kt.protocol.types.emulation.UserAgentMetadata
import ai.platon.cdt.kt.protocol.types.emulation.VirtualTimePolicy
import ai.platon.cdt.kt.protocol.types.page.Viewport
import kotlin.Boolean
import kotlin.Deprecated
import kotlin.Double
import kotlin.Int
import kotlin.String
import kotlin.Unit
import kotlin.collections.List

/**
 * This domain emulates different environments for the page.
 */
interface Emulation {
  /**
   * Tells whether emulation is supported.
   */
  @Returns("result")
  suspend fun canEmulate(): Boolean

  /**
   * Clears the overridden device metrics.
   */
  suspend fun clearDeviceMetricsOverride()

  /**
   * Clears the overridden Geolocation Position and Error.
   */
  suspend fun clearGeolocationOverride()

  /**
   * Requests that page scale factor is reset to initial values.
   */
  @Experimental
  suspend fun resetPageScaleFactor()

  /**
   * Enables or disables simulating a focused and active page.
   * @param enabled Whether to enable to disable focus emulation.
   */
  @Experimental
  suspend fun setFocusEmulationEnabled(@ParamName("enabled") enabled: Boolean)

  /**
   * Enables CPU throttling to emulate slow CPUs.
   * @param rate Throttling rate as a slowdown factor (1 is no throttle, 2 is 2x slowdown, etc).
   */
  @Experimental
  suspend fun setCPUThrottlingRate(@ParamName("rate") rate: Double)

  /**
   * Sets or clears an override of the default background color of the frame. This override is used
   * if the content does not specify one.
   * @param color RGBA of the default background color. If not specified, any existing override will be
   * cleared.
   */
  suspend fun setDefaultBackgroundColorOverride(@ParamName("color") @Optional color: RGBA? = null)

  suspend fun setDefaultBackgroundColorOverride() {
    return setDefaultBackgroundColorOverride(null)
  }

  /**
   * Overrides the values of device screen dimensions (window.screen.width, window.screen.height,
   * window.innerWidth, window.innerHeight, and "device-width"/"device-height"-related CSS media
   * query results).
   * @param width Overriding width value in pixels (minimum 0, maximum 10000000). 0 disables the override.
   * @param height Overriding height value in pixels (minimum 0, maximum 10000000). 0 disables the override.
   * @param deviceScaleFactor Overriding device scale factor value. 0 disables the override.
   * @param mobile Whether to emulate mobile device. This includes viewport meta tag, overlay scrollbars, text
   * autosizing and more.
   * @param scale Scale to apply to resulting view image.
   * @param screenWidth Overriding screen width value in pixels (minimum 0, maximum 10000000).
   * @param screenHeight Overriding screen height value in pixels (minimum 0, maximum 10000000).
   * @param positionX Overriding view X position on screen in pixels (minimum 0, maximum 10000000).
   * @param positionY Overriding view Y position on screen in pixels (minimum 0, maximum 10000000).
   * @param dontSetVisibleSize Do not set visible view size, rely upon explicit setVisibleSize call.
   * @param screenOrientation Screen orientation override.
   * @param viewport If set, the visible area of the page will be overridden to this viewport. This viewport
   * change is not observed by the page, e.g. viewport-relative elements do not change positions.
   * @param displayFeature If set, the display feature of a multi-segment screen. If not set, multi-segment support
   * is turned-off.
   */
  suspend fun setDeviceMetricsOverride(
    @ParamName("width") width: Int,
    @ParamName("height") height: Int,
    @ParamName("deviceScaleFactor") deviceScaleFactor: Double,
    @ParamName("mobile") mobile: Boolean,
    @ParamName("scale") @Optional @Experimental scale: Double? = null,
    @ParamName("screenWidth") @Optional @Experimental screenWidth: Int? = null,
    @ParamName("screenHeight") @Optional @Experimental screenHeight: Int? = null,
    @ParamName("positionX") @Optional @Experimental positionX: Int? = null,
    @ParamName("positionY") @Optional @Experimental positionY: Int? = null,
    @ParamName("dontSetVisibleSize") @Optional @Experimental dontSetVisibleSize: Boolean? = null,
    @ParamName("screenOrientation") @Optional screenOrientation: ScreenOrientation? = null,
    @ParamName("viewport") @Optional @Experimental viewport: Viewport? = null,
    @ParamName("displayFeature") @Optional @Experimental displayFeature: DisplayFeature? = null,
  )

  suspend fun setDeviceMetricsOverride(
    @ParamName("width") width: Int,
    @ParamName("height") height: Int,
    @ParamName("deviceScaleFactor") deviceScaleFactor: Double,
    @ParamName("mobile") mobile: Boolean,
  ) {
    return setDeviceMetricsOverride(width, height, deviceScaleFactor, mobile, null, null, null, null, null, null, null, null, null)
  }

  /**
   * @param hidden Whether scrollbars should be always hidden.
   */
  @Experimental
  suspend fun setScrollbarsHidden(@ParamName("hidden") hidden: Boolean)

  /**
   * @param disabled Whether document.coookie API should be disabled.
   */
  @Experimental
  suspend fun setDocumentCookieDisabled(@ParamName("disabled") disabled: Boolean)

  /**
   * @param enabled Whether touch emulation based on mouse input should be enabled.
   * @param configuration Touch/gesture events configuration. Default: current platform.
   */
  @Experimental
  suspend fun setEmitTouchEventsForMouse(@ParamName("enabled") enabled: Boolean, @ParamName("configuration") @Optional configuration: SetEmitTouchEventsForMouseConfiguration? = null)

  @Experimental
  suspend fun setEmitTouchEventsForMouse(@ParamName("enabled") enabled: Boolean) {
    return setEmitTouchEventsForMouse(enabled, null)
  }

  /**
   * Emulates the given media type or media feature for CSS media queries.
   * @param media Media type to emulate. Empty string disables the override.
   * @param features Media features to emulate.
   */
  suspend fun setEmulatedMedia(@ParamName("media") @Optional media: String? = null, @ParamName("features") @Optional features: List<MediaFeature>? = null)

  suspend fun setEmulatedMedia() {
    return setEmulatedMedia(null, null)
  }

  /**
   * Emulates the given vision deficiency.
   * @param type Vision deficiency to emulate.
   */
  @Experimental
  suspend fun setEmulatedVisionDeficiency(@ParamName("type") type: SetEmulatedVisionDeficiencyType)

  /**
   * Overrides the Geolocation Position or Error. Omitting any of the parameters emulates position
   * unavailable.
   * @param latitude Mock latitude
   * @param longitude Mock longitude
   * @param accuracy Mock accuracy
   */
  suspend fun setGeolocationOverride(
    @ParamName("latitude") @Optional latitude: Double? = null,
    @ParamName("longitude") @Optional longitude: Double? = null,
    @ParamName("accuracy") @Optional accuracy: Double? = null,
  )

  suspend fun setGeolocationOverride() {
    return setGeolocationOverride(null, null, null)
  }

  /**
   * Overrides the Idle state.
   * @param isUserActive Mock isUserActive
   * @param isScreenUnlocked Mock isScreenUnlocked
   */
  @Experimental
  suspend fun setIdleOverride(@ParamName("isUserActive") isUserActive: Boolean, @ParamName("isScreenUnlocked") isScreenUnlocked: Boolean)

  /**
   * Clears Idle state overrides.
   */
  @Experimental
  suspend fun clearIdleOverride()

  /**
   * Overrides value returned by the javascript navigator object.
   * @param platform The platform navigator.platform should return.
   */
  @Deprecated("Deprecated by protocol")
  @Experimental
  suspend fun setNavigatorOverrides(@ParamName("platform") platform: String)

  /**
   * Sets a specified page scale factor.
   * @param pageScaleFactor Page scale factor.
   */
  @Experimental
  suspend fun setPageScaleFactor(@ParamName("pageScaleFactor") pageScaleFactor: Double)

  /**
   * Switches script execution in the page.
   * @param value Whether script execution should be disabled in the page.
   */
  suspend fun setScriptExecutionDisabled(@ParamName("value") `value`: Boolean)

  /**
   * Enables touch on platforms which do not support them.
   * @param enabled Whether the touch event emulation should be enabled.
   * @param maxTouchPoints Maximum touch points supported. Defaults to one.
   */
  suspend fun setTouchEmulationEnabled(@ParamName("enabled") enabled: Boolean, @ParamName("maxTouchPoints") @Optional maxTouchPoints: Int? = null)

  suspend fun setTouchEmulationEnabled(@ParamName("enabled") enabled: Boolean) {
    return setTouchEmulationEnabled(enabled, null)
  }

  /**
   * Turns on virtual time for all frames (replacing real-time with a synthetic time source) and sets
   * the current virtual time policy.  Note this supersedes any previous time budget.
   * @param policy
   * @param budget If set, after this many virtual milliseconds have elapsed virtual time will be paused and a
   * virtualTimeBudgetExpired event is sent.
   * @param maxVirtualTimeTaskStarvationCount If set this specifies the maximum number of tasks that can be run before virtual is forced
   * forwards to prevent deadlock.
   * @param waitForNavigation If set the virtual time policy change should be deferred until any frame starts navigating.
   * Note any previous deferred policy change is superseded.
   * @param initialVirtualTime If set, base::Time::Now will be overridden to initially return this value.
   */
  @Experimental
  @Returns("virtualTimeTicksBase")
  suspend fun setVirtualTimePolicy(
    @ParamName("policy") policy: VirtualTimePolicy,
    @ParamName("budget") @Optional budget: Double? = null,
    @ParamName("maxVirtualTimeTaskStarvationCount") @Optional maxVirtualTimeTaskStarvationCount: Int? = null,
    @ParamName("waitForNavigation") @Optional waitForNavigation: Boolean? = null,
    @ParamName("initialVirtualTime") @Optional initialVirtualTime: Double? = null,
  ): Double

  @Experimental
  @Returns("virtualTimeTicksBase")
  suspend fun setVirtualTimePolicy(@ParamName("policy") policy: VirtualTimePolicy): Double {
    return setVirtualTimePolicy(policy, null, null, null, null)
  }

  /**
   * Overrides default host system locale with the specified one.
   * @param locale ICU style C locale (e.g. "en_US"). If not specified or empty, disables the override and
   * restores default host system locale.
   */
  @Experimental
  suspend fun setLocaleOverride(@ParamName("locale") @Optional locale: String? = null)

  @Experimental
  suspend fun setLocaleOverride() {
    return setLocaleOverride(null)
  }

  /**
   * Overrides default host system timezone with the specified one.
   * @param timezoneId The timezone identifier. If empty, disables the override and
   * restores default host system timezone.
   */
  @Experimental
  suspend fun setTimezoneOverride(@ParamName("timezoneId") timezoneId: String)

  /**
   * Resizes the frame/viewport of the page. Note that this does not affect the frame's container
   * (e.g. browser window). Can be used to produce screenshots of the specified size. Not supported
   * on Android.
   * @param width Frame width (DIP).
   * @param height Frame height (DIP).
   */
  @Deprecated("Deprecated by protocol")
  @Experimental
  suspend fun setVisibleSize(@ParamName("width") width: Int, @ParamName("height") height: Int)

  /**
   * @param imageTypes Image types to disable.
   */
  @Experimental
  suspend fun setDisabledImageTypes(@ParamName("imageTypes") imageTypes: List<DisabledImageType>)

  /**
   * Allows overriding user agent with the given string.
   * @param userAgent User agent to use.
   * @param acceptLanguage Browser langugage to emulate.
   * @param platform The platform navigator.platform should return.
   * @param userAgentMetadata To be sent in Sec-CH-UA-* headers and returned in navigator.userAgentData
   */
  suspend fun setUserAgentOverride(
    @ParamName("userAgent") userAgent: String,
    @ParamName("acceptLanguage") @Optional acceptLanguage: String? = null,
    @ParamName("platform") @Optional platform: String? = null,
    @ParamName("userAgentMetadata") @Optional @Experimental userAgentMetadata: UserAgentMetadata? = null,
  )

  suspend fun setUserAgentOverride(@ParamName("userAgent") userAgent: String) {
    return setUserAgentOverride(userAgent, null, null, null)
  }

  @EventName("virtualTimeBudgetExpired")
  @Experimental
  fun onVirtualTimeBudgetExpired(eventListener: EventHandler<VirtualTimeBudgetExpired>): EventListener

  @EventName("virtualTimeBudgetExpired")
  @Experimental
  fun onVirtualTimeBudgetExpired(eventListener: suspend (VirtualTimeBudgetExpired) -> Unit): EventListener
}
