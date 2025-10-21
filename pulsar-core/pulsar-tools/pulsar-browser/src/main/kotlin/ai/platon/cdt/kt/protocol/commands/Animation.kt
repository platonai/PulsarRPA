package ai.platon.cdt.kt.protocol.commands

import ai.platon.cdt.kt.protocol.events.animation.AnimationCanceled
import ai.platon.cdt.kt.protocol.events.animation.AnimationCreated
import ai.platon.cdt.kt.protocol.events.animation.AnimationStarted
import ai.platon.cdt.kt.protocol.support.annotations.EventName
import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.ParamName
import ai.platon.cdt.kt.protocol.support.annotations.Returns
import ai.platon.cdt.kt.protocol.support.types.EventHandler
import ai.platon.cdt.kt.protocol.support.types.EventListener
import ai.platon.cdt.kt.protocol.types.runtime.RemoteObject
import kotlin.Boolean
import kotlin.Double
import kotlin.String
import kotlin.Unit
import kotlin.collections.List

@Experimental
public interface Animation {
  /**
   * Disables animation domain notifications.
   */
  public suspend fun disable()

  /**
   * Enables animation domain notifications.
   */
  public suspend fun enable()

  /**
   * Returns the current time of the an animation.
   * @param id Id of animation.
   */
  @Returns("currentTime")
  public suspend fun getCurrentTime(@ParamName("id") id: String): Double

  /**
   * Gets the playback rate of the document timeline.
   */
  @Returns("playbackRate")
  public suspend fun getPlaybackRate(): Double

  /**
   * Releases a set of animations to no longer be manipulated.
   * @param animations List of animation ids to seek.
   */
  public suspend fun releaseAnimations(@ParamName("animations") animations: List<String>)

  /**
   * Gets the remote object of the Animation.
   * @param animationId Animation id.
   */
  @Returns("remoteObject")
  public suspend fun resolveAnimation(@ParamName("animationId") animationId: String): RemoteObject

  /**
   * Seek a set of animations to a particular time within each animation.
   * @param animations List of animation ids to seek.
   * @param currentTime Set the current time of each animation.
   */
  public suspend fun seekAnimations(@ParamName("animations") animations: List<String>,
      @ParamName("currentTime") currentTime: Double)

  /**
   * Sets the paused state of a set of animations.
   * @param animations Animations to set the pause state of.
   * @param paused Paused state to set to.
   */
  public suspend fun setPaused(@ParamName("animations") animations: List<String>,
      @ParamName("paused") paused: Boolean)

  /**
   * Sets the playback rate of the document timeline.
   * @param playbackRate Playback rate for animations on page
   */
  public suspend fun setPlaybackRate(@ParamName("playbackRate") playbackRate: Double)

  /**
   * Sets the timing of an animation node.
   * @param animationId Animation id.
   * @param duration Duration of the animation.
   * @param delay Delay of the animation.
   */
  public suspend fun setTiming(
    @ParamName("animationId") animationId: String,
    @ParamName("duration") duration: Double,
    @ParamName("delay") delay: Double,
  )

  @EventName("animationCanceled")
  public fun onAnimationCanceled(eventListener: EventHandler<AnimationCanceled>): EventListener

  @EventName("animationCanceled")
  public fun onAnimationCanceled(eventListener: suspend (AnimationCanceled) -> Unit): EventListener

  @EventName("animationCreated")
  public fun onAnimationCreated(eventListener: EventHandler<AnimationCreated>): EventListener

  @EventName("animationCreated")
  public fun onAnimationCreated(eventListener: suspend (AnimationCreated) -> Unit): EventListener

  @EventName("animationStarted")
  public fun onAnimationStarted(eventListener: EventHandler<AnimationStarted>): EventListener

  @EventName("animationStarted")
  public fun onAnimationStarted(eventListener: suspend (AnimationStarted) -> Unit): EventListener
}
