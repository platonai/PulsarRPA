@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.commands

import ai.platon.cdt.kt.protocol.events.headlessexperimental.NeedsBeginFramesChanged
import ai.platon.cdt.kt.protocol.support.annotations.EventName
import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.support.annotations.ParamName
import ai.platon.cdt.kt.protocol.support.types.EventHandler
import ai.platon.cdt.kt.protocol.support.types.EventListener
import ai.platon.cdt.kt.protocol.types.headlessexperimental.BeginFrame
import ai.platon.cdt.kt.protocol.types.headlessexperimental.ScreenshotParams
import kotlin.Boolean
import kotlin.Deprecated
import kotlin.Double
import kotlin.Unit

/**
 * This domain provides experimental commands only supported in headless mode.
 */
@Experimental
interface HeadlessExperimental {
  /**
   * Sends a BeginFrame to the target and returns when the frame was completed. Optionally captures a
   * screenshot from the resulting frame. Requires that the target was created with enabled
   * BeginFrameControl. Designed for use with --run-all-compositor-stages-before-draw, see also
   * https://goo.gl/3zHXhB for more background.
   * @param frameTimeTicks Timestamp of this BeginFrame in Renderer TimeTicks (milliseconds of uptime). If not set,
   * the current time will be used.
   * @param interval The interval between BeginFrames that is reported to the compositor, in milliseconds.
   * Defaults to a 60 frames/second interval, i.e. about 16.666 milliseconds.
   * @param noDisplayUpdates Whether updates should not be committed and drawn onto the display. False by default. If
   * true, only side effects of the BeginFrame will be run, such as layout and animations, but
   * any visual updates may not be visible on the display or in screenshots.
   * @param screenshot If set, a screenshot of the frame will be captured and returned in the response. Otherwise,
   * no screenshot will be captured. Note that capturing a screenshot can fail, for example,
   * during renderer initialization. In such a case, no screenshot data will be returned.
   */
  suspend fun beginFrame(
    @ParamName("frameTimeTicks") @Optional frameTimeTicks: Double? = null,
    @ParamName("interval") @Optional interval: Double? = null,
    @ParamName("noDisplayUpdates") @Optional noDisplayUpdates: Boolean? = null,
    @ParamName("screenshot") @Optional screenshot: ScreenshotParams? = null,
  ): BeginFrame

  suspend fun beginFrame(): BeginFrame {
    return beginFrame(null, null, null, null)
  }

  /**
   * Disables headless events for the target.
   */
  suspend fun disable()

  /**
   * Enables headless events for the target.
   */
  suspend fun enable()

  @EventName("needsBeginFramesChanged")
  @Deprecated("Deprecated by protocol")
  fun onNeedsBeginFramesChanged(eventListener: EventHandler<NeedsBeginFramesChanged>): EventListener

  @EventName("needsBeginFramesChanged")
  @Deprecated("Deprecated by protocol")
  fun onNeedsBeginFramesChanged(eventListener: suspend (NeedsBeginFramesChanged) -> Unit): EventListener
}
