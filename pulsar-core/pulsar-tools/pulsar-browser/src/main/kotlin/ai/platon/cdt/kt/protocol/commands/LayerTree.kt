package ai.platon.cdt.kt.protocol.commands

import ai.platon.cdt.kt.protocol.events.layertree.LayerPainted
import ai.platon.cdt.kt.protocol.events.layertree.LayerTreeDidChange
import ai.platon.cdt.kt.protocol.support.annotations.EventName
import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.support.annotations.ParamName
import ai.platon.cdt.kt.protocol.support.annotations.ReturnTypeParameter
import ai.platon.cdt.kt.protocol.support.annotations.Returns
import ai.platon.cdt.kt.protocol.support.types.EventHandler
import ai.platon.cdt.kt.protocol.support.types.EventListener
import ai.platon.cdt.kt.protocol.types.dom.Rect
import ai.platon.cdt.kt.protocol.types.layertree.CompositingReasons
import ai.platon.cdt.kt.protocol.types.layertree.PictureTile
import kotlin.Any
import kotlin.Double
import kotlin.Int
import kotlin.String
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.Map

@Experimental
public interface LayerTree {
  /**
   * Provides the reasons why the given layer was composited.
   * @param layerId The id of the layer for which we want to get the reasons it was composited.
   */
  public suspend fun compositingReasons(@ParamName("layerId") layerId: String): CompositingReasons

  /**
   * Disables compositing tree inspection.
   */
  public suspend fun disable()

  /**
   * Enables compositing tree inspection.
   */
  public suspend fun enable()

  /**
   * Returns the snapshot identifier.
   * @param tiles An array of tiles composing the snapshot.
   */
  @Returns("snapshotId")
  public suspend fun loadSnapshot(@ParamName("tiles") tiles: List<PictureTile>): String

  /**
   * Returns the layer snapshot identifier.
   * @param layerId The id of the layer.
   */
  @Returns("snapshotId")
  public suspend fun makeSnapshot(@ParamName("layerId") layerId: String): String

  /**
   * @param snapshotId The id of the layer snapshot.
   * @param minRepeatCount The maximum number of times to replay the snapshot (1, if not specified).
   * @param minDuration The minimum duration (in seconds) to replay the snapshot.
   * @param clipRect The clip rectangle to apply when replaying the snapshot.
   */
  @Returns("timings")
  @ReturnTypeParameter(Double::class)
  public suspend fun profileSnapshot(
    @ParamName("snapshotId") snapshotId: String,
    @ParamName("minRepeatCount") @Optional minRepeatCount: Int?,
    @ParamName("minDuration") @Optional minDuration: Double?,
    @ParamName("clipRect") @Optional clipRect: Rect?,
  ): List<List<Double>>

  @Returns("timings")
  @ReturnTypeParameter(Double::class)
  public suspend fun profileSnapshot(@ParamName("snapshotId") snapshotId: String):
      List<List<Double>> {
    return profileSnapshot(snapshotId, null, null, null)
  }

  /**
   * Releases layer snapshot captured by the back-end.
   * @param snapshotId The id of the layer snapshot.
   */
  public suspend fun releaseSnapshot(@ParamName("snapshotId") snapshotId: String)

  /**
   * Replays the layer snapshot and returns the resulting bitmap.
   * @param snapshotId The id of the layer snapshot.
   * @param fromStep The first step to replay from (replay from the very start if not specified).
   * @param toStep The last step to replay to (replay till the end if not specified).
   * @param scale The scale to apply while replaying (defaults to 1).
   */
  @Returns("dataURL")
  public suspend fun replaySnapshot(
    @ParamName("snapshotId") snapshotId: String,
    @ParamName("fromStep") @Optional fromStep: Int?,
    @ParamName("toStep") @Optional toStep: Int?,
    @ParamName("scale") @Optional scale: Double?,
  ): String

  @Returns("dataURL")
  public suspend fun replaySnapshot(@ParamName("snapshotId") snapshotId: String): String {
    return replaySnapshot(snapshotId, null, null, null)
  }

  /**
   * Replays the layer snapshot and returns canvas log.
   * @param snapshotId The id of the layer snapshot.
   */
  @Returns("commandLog")
  @ReturnTypeParameter(String::class, Any::class)
  public suspend fun snapshotCommandLog(@ParamName("snapshotId") snapshotId: String):
      List<Map<String, Any?>>

  @EventName("layerPainted")
  public fun onLayerPainted(eventListener: EventHandler<LayerPainted>): EventListener

  @EventName("layerPainted")
  public fun onLayerPainted(eventListener: suspend (LayerPainted) -> Unit): EventListener

  @EventName("layerTreeDidChange")
  public fun onLayerTreeDidChange(eventListener: EventHandler<LayerTreeDidChange>): EventListener

  @EventName("layerTreeDidChange")
  public fun onLayerTreeDidChange(eventListener: suspend (LayerTreeDidChange) -> Unit):
      EventListener
}
