package ai.platon.cdt.kt.protocol.commands

import ai.platon.cdt.kt.protocol.events.overlay.InspectModeCanceled
import ai.platon.cdt.kt.protocol.events.overlay.InspectNodeRequested
import ai.platon.cdt.kt.protocol.events.overlay.NodeHighlightRequested
import ai.platon.cdt.kt.protocol.events.overlay.ScreenshotRequested
import ai.platon.cdt.kt.protocol.support.annotations.EventName
import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.support.annotations.ParamName
import ai.platon.cdt.kt.protocol.support.annotations.ReturnTypeParameter
import ai.platon.cdt.kt.protocol.support.annotations.Returns
import ai.platon.cdt.kt.protocol.support.types.EventHandler
import ai.platon.cdt.kt.protocol.support.types.EventListener
import ai.platon.cdt.kt.protocol.types.dom.RGBA
import ai.platon.cdt.kt.protocol.types.overlay.ColorFormat
import ai.platon.cdt.kt.protocol.types.overlay.FlexNodeHighlightConfig
import ai.platon.cdt.kt.protocol.types.overlay.GridNodeHighlightConfig
import ai.platon.cdt.kt.protocol.types.overlay.HighlightConfig
import ai.platon.cdt.kt.protocol.types.overlay.HingeConfig
import ai.platon.cdt.kt.protocol.types.overlay.InspectMode
import ai.platon.cdt.kt.protocol.types.overlay.ScrollSnapHighlightConfig
import ai.platon.cdt.kt.protocol.types.overlay.SourceOrderConfig
import kotlin.Any
import kotlin.Boolean
import kotlin.Double
import kotlin.Int
import kotlin.String
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.Map

/**
 * This domain provides various functionality related to drawing atop the inspected page.
 */
@Experimental
public interface Overlay {
  /**
   * Disables domain notifications.
   */
  public suspend fun disable()

  /**
   * Enables domain notifications.
   */
  public suspend fun enable()

  /**
   * For testing.
   * @param nodeId Id of the node to get highlight object for.
   * @param includeDistance Whether to include distance info.
   * @param includeStyle Whether to include style info.
   * @param colorFormat The color format to get config with (default: hex).
   * @param showAccessibilityInfo Whether to show accessibility info (default: true).
   */
  @Returns("highlight")
  @ReturnTypeParameter(String::class, Any::class)
  public suspend fun getHighlightObjectForTest(
    @ParamName("nodeId") nodeId: Int,
    @ParamName("includeDistance") @Optional includeDistance: Boolean?,
    @ParamName("includeStyle") @Optional includeStyle: Boolean?,
    @ParamName("colorFormat") @Optional colorFormat: ColorFormat?,
    @ParamName("showAccessibilityInfo") @Optional showAccessibilityInfo: Boolean?,
  ): Map<String, Any?>

  @Returns("highlight")
  @ReturnTypeParameter(String::class, Any::class)
  public suspend fun getHighlightObjectForTest(@ParamName("nodeId") nodeId: Int):
      Map<String, Any?> {
    return getHighlightObjectForTest(nodeId, null, null, null, null)
  }

  /**
   * For Persistent Grid testing.
   * @param nodeIds Ids of the node to get highlight object for.
   */
  @Returns("highlights")
  @ReturnTypeParameter(String::class, Any::class)
  public suspend fun getGridHighlightObjectsForTest(@ParamName("nodeIds") nodeIds: List<Int>):
      Map<String, Any?>

  /**
   * For Source Order Viewer testing.
   * @param nodeId Id of the node to highlight.
   */
  @Returns("highlight")
  @ReturnTypeParameter(String::class, Any::class)
  public suspend fun getSourceOrderHighlightObjectForTest(@ParamName("nodeId") nodeId: Int):
      Map<String, Any?>

  /**
   * Hides any highlight.
   */
  public suspend fun hideHighlight()

  /**
   * Highlights owner element of the frame with given id.
   * @param frameId Identifier of the frame to highlight.
   * @param contentColor The content box highlight fill color (default: transparent).
   * @param contentOutlineColor The content box highlight outline color (default: transparent).
   */
  public suspend fun highlightFrame(
    @ParamName("frameId") frameId: String,
    @ParamName("contentColor") @Optional contentColor: RGBA?,
    @ParamName("contentOutlineColor") @Optional contentOutlineColor: RGBA?,
  )

  public suspend fun highlightFrame(@ParamName("frameId") frameId: String) {
    return highlightFrame(frameId, null, null)
  }

  /**
   * Highlights DOM node with given id or with the given JavaScript object wrapper. Either nodeId or
   * objectId must be specified.
   * @param highlightConfig A descriptor for the highlight appearance.
   * @param nodeId Identifier of the node to highlight.
   * @param backendNodeId Identifier of the backend node to highlight.
   * @param objectId JavaScript object id of the node to be highlighted.
   * @param selector Selectors to highlight relevant nodes.
   */
  public suspend fun highlightNode(
    @ParamName("highlightConfig") highlightConfig: HighlightConfig,
    @ParamName("nodeId") @Optional nodeId: Int?,
    @ParamName("backendNodeId") @Optional backendNodeId: Int?,
    @ParamName("objectId") @Optional objectId: String?,
    @ParamName("selector") @Optional selector: String?,
  )

  public suspend fun highlightNode(@ParamName("highlightConfig") highlightConfig: HighlightConfig) {
    return highlightNode(highlightConfig, null, null, null, null)
  }

  /**
   * Highlights given quad. Coordinates are absolute with respect to the main frame viewport.
   * @param quad Quad to highlight
   * @param color The highlight fill color (default: transparent).
   * @param outlineColor The highlight outline color (default: transparent).
   */
  public suspend fun highlightQuad(
    @ParamName("quad") quad: List<Double>,
    @ParamName("color") @Optional color: RGBA?,
    @ParamName("outlineColor") @Optional outlineColor: RGBA?,
  )

  public suspend fun highlightQuad(@ParamName("quad") quad: List<Double>) {
    return highlightQuad(quad, null, null)
  }

  /**
   * Highlights given rectangle. Coordinates are absolute with respect to the main frame viewport.
   * @param x X coordinate
   * @param y Y coordinate
   * @param width Rectangle width
   * @param height Rectangle height
   * @param color The highlight fill color (default: transparent).
   * @param outlineColor The highlight outline color (default: transparent).
   */
  public suspend fun highlightRect(
    @ParamName("x") x: Int,
    @ParamName("y") y: Int,
    @ParamName("width") width: Int,
    @ParamName("height") height: Int,
    @ParamName("color") @Optional color: RGBA?,
    @ParamName("outlineColor") @Optional outlineColor: RGBA?,
  )

  public suspend fun highlightRect(
    @ParamName("x") x: Int,
    @ParamName("y") y: Int,
    @ParamName("width") width: Int,
    @ParamName("height") height: Int,
  ) {
    return highlightRect(x, y, width, height, null, null)
  }

  /**
   * Highlights the source order of the children of the DOM node with given id or with the given
   * JavaScript object wrapper. Either nodeId or objectId must be specified.
   * @param sourceOrderConfig A descriptor for the appearance of the overlay drawing.
   * @param nodeId Identifier of the node to highlight.
   * @param backendNodeId Identifier of the backend node to highlight.
   * @param objectId JavaScript object id of the node to be highlighted.
   */
  public suspend fun highlightSourceOrder(
    @ParamName("sourceOrderConfig") sourceOrderConfig: SourceOrderConfig,
    @ParamName("nodeId") @Optional nodeId: Int?,
    @ParamName("backendNodeId") @Optional backendNodeId: Int?,
    @ParamName("objectId") @Optional objectId: String?,
  )

  public suspend fun highlightSourceOrder(@ParamName("sourceOrderConfig")
      sourceOrderConfig: SourceOrderConfig) {
    return highlightSourceOrder(sourceOrderConfig, null, null, null)
  }

  /**
   * Enters the 'inspect' mode. In this mode, elements that user is hovering over are highlighted.
   * Backend then generates 'inspectNodeRequested' event upon element selection.
   * @param mode Set an inspection mode.
   * @param highlightConfig A descriptor for the highlight appearance of hovered-over nodes. May be
   * omitted if `enabled
   * == false`.
   */
  public suspend fun setInspectMode(@ParamName("mode") mode: InspectMode,
      @ParamName("highlightConfig") @Optional highlightConfig: HighlightConfig?)

  public suspend fun setInspectMode(@ParamName("mode") mode: InspectMode) {
    return setInspectMode(mode, null)
  }

  /**
   * Highlights owner element of all frames detected to be ads.
   * @param show True for showing ad highlights
   */
  public suspend fun setShowAdHighlights(@ParamName("show") show: Boolean)

  /**
   * @param message The message to display, also triggers resume and step over controls.
   */
  public suspend fun setPausedInDebuggerMessage(@ParamName("message") @Optional message: String?)

  public suspend fun setPausedInDebuggerMessage() {
    return setPausedInDebuggerMessage(null)
  }

  /**
   * Requests that backend shows debug borders on layers
   * @param show True for showing debug borders
   */
  public suspend fun setShowDebugBorders(@ParamName("show") show: Boolean)

  /**
   * Requests that backend shows the FPS counter
   * @param show True for showing the FPS counter
   */
  public suspend fun setShowFPSCounter(@ParamName("show") show: Boolean)

  /**
   * Highlight multiple elements with the CSS Grid overlay.
   * @param gridNodeHighlightConfigs An array of node identifiers and descriptors for the highlight
   * appearance.
   */
  public suspend fun setShowGridOverlays(@ParamName("gridNodeHighlightConfigs")
      gridNodeHighlightConfigs: List<GridNodeHighlightConfig>)

  /**
   * @param flexNodeHighlightConfigs An array of node identifiers and descriptors for the highlight
   * appearance.
   */
  public suspend fun setShowFlexOverlays(@ParamName("flexNodeHighlightConfigs")
      flexNodeHighlightConfigs: List<FlexNodeHighlightConfig>)

  /**
   * @param scrollSnapHighlightConfigs An array of node identifiers and descriptors for the
   * highlight appearance.
   */
  public suspend fun setShowScrollSnapOverlays(@ParamName("scrollSnapHighlightConfigs")
      scrollSnapHighlightConfigs: List<ScrollSnapHighlightConfig>)

  /**
   * Requests that backend shows paint rectangles
   * @param result True for showing paint rectangles
   */
  public suspend fun setShowPaintRects(@ParamName("result") result: Boolean)

  /**
   * Requests that backend shows layout shift regions
   * @param result True for showing layout shift regions
   */
  public suspend fun setShowLayoutShiftRegions(@ParamName("result") result: Boolean)

  /**
   * Requests that backend shows scroll bottleneck rects
   * @param show True for showing scroll bottleneck rects
   */
  public suspend fun setShowScrollBottleneckRects(@ParamName("show") show: Boolean)

  /**
   * Requests that backend shows hit-test borders on layers
   * @param show True for showing hit-test borders
   */
  public suspend fun setShowHitTestBorders(@ParamName("show") show: Boolean)

  /**
   * Request that backend shows an overlay with web vital metrics.
   * @param show
   */
  public suspend fun setShowWebVitals(@ParamName("show") show: Boolean)

  /**
   * Paints viewport size upon main frame resize.
   * @param show Whether to paint size or not.
   */
  public suspend fun setShowViewportSizeOnResize(@ParamName("show") show: Boolean)

  /**
   * Add a dual screen device hinge
   * @param hingeConfig hinge data, null means hideHinge
   */
  public suspend fun setShowHinge(@ParamName("hingeConfig") @Optional hingeConfig: HingeConfig?)

  public suspend fun setShowHinge() {
    return setShowHinge(null)
  }

  @EventName("inspectNodeRequested")
  public fun onInspectNodeRequested(eventListener: EventHandler<InspectNodeRequested>):
      EventListener

  @EventName("inspectNodeRequested")
  public fun onInspectNodeRequested(eventListener: suspend (InspectNodeRequested) -> Unit):
      EventListener

  @EventName("nodeHighlightRequested")
  public fun onNodeHighlightRequested(eventListener: EventHandler<NodeHighlightRequested>):
      EventListener

  @EventName("nodeHighlightRequested")
  public fun onNodeHighlightRequested(eventListener: suspend (NodeHighlightRequested) -> Unit):
      EventListener

  @EventName("screenshotRequested")
  public fun onScreenshotRequested(eventListener: EventHandler<ScreenshotRequested>): EventListener

  @EventName("screenshotRequested")
  public fun onScreenshotRequested(eventListener: suspend (ScreenshotRequested) -> Unit):
      EventListener

  @EventName("inspectModeCanceled")
  public fun onInspectModeCanceled(eventListener: EventHandler<InspectModeCanceled>): EventListener

  @EventName("inspectModeCanceled")
  public fun onInspectModeCanceled(eventListener: suspend (InspectModeCanceled) -> Unit):
      EventListener
}
