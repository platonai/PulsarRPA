package com.github.kklisura.cdt.protocol.commands;

/*-
 * #%L
 * cdt-java-client
 * %%
 * Copyright (C) 2018 - 2019 Kenan Klisura
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

import com.github.kklisura.cdt.protocol.events.overlay.InspectModeCanceled;
import com.github.kklisura.cdt.protocol.events.overlay.InspectNodeRequested;
import com.github.kklisura.cdt.protocol.events.overlay.NodeHighlightRequested;
import com.github.kklisura.cdt.protocol.events.overlay.ScreenshotRequested;
import com.github.kklisura.cdt.protocol.support.annotations.EventName;
import com.github.kklisura.cdt.protocol.support.annotations.Experimental;
import com.github.kklisura.cdt.protocol.support.annotations.Optional;
import com.github.kklisura.cdt.protocol.support.annotations.ParamName;
import com.github.kklisura.cdt.protocol.support.annotations.Returns;
import com.github.kklisura.cdt.protocol.support.types.EventHandler;
import com.github.kklisura.cdt.protocol.support.types.EventListener;
import com.github.kklisura.cdt.protocol.types.dom.RGBA;
import com.github.kklisura.cdt.protocol.types.overlay.HighlightConfig;
import com.github.kklisura.cdt.protocol.types.overlay.InspectMode;
import java.util.List;
import java.util.Map;

/** This domain provides various functionality related to drawing atop the inspected page. */
@Experimental
public interface Overlay {

  /** Disables domain notifications. */
  void disable();

  /** Enables domain notifications. */
  void enable();

  /**
   * For testing.
   *
   * @param nodeId Id of the node to get highlight object for.
   */
  @Returns("highlight")
  Map<String, Object> getHighlightObjectForTest(@ParamName("nodeId") Integer nodeId);

  /**
   * For testing.
   *
   * @param nodeId Id of the node to get highlight object for.
   * @param includeDistance Whether to include distance info.
   * @param includeStyle Whether to include style info.
   */
  @Returns("highlight")
  Map<String, Object> getHighlightObjectForTest(
      @ParamName("nodeId") Integer nodeId,
      @Optional @ParamName("includeDistance") Boolean includeDistance,
      @Optional @ParamName("includeStyle") Boolean includeStyle);

  /** Hides any highlight. */
  void hideHighlight();

  /**
   * Highlights owner element of the frame with given id.
   *
   * @param frameId Identifier of the frame to highlight.
   */
  void highlightFrame(@ParamName("frameId") String frameId);

  /**
   * Highlights owner element of the frame with given id.
   *
   * @param frameId Identifier of the frame to highlight.
   * @param contentColor The content box highlight fill color (default: transparent).
   * @param contentOutlineColor The content box highlight outline color (default: transparent).
   */
  void highlightFrame(
      @ParamName("frameId") String frameId,
      @Optional @ParamName("contentColor") RGBA contentColor,
      @Optional @ParamName("contentOutlineColor") RGBA contentOutlineColor);

  /**
   * Highlights DOM node with given id or with the given JavaScript object wrapper. Either nodeId or
   * objectId must be specified.
   *
   * @param highlightConfig A descriptor for the highlight appearance.
   */
  void highlightNode(@ParamName("highlightConfig") HighlightConfig highlightConfig);

  /**
   * Highlights DOM node with given id or with the given JavaScript object wrapper. Either nodeId or
   * objectId must be specified.
   *
   * @param highlightConfig A descriptor for the highlight appearance.
   * @param nodeId Identifier of the node to highlight.
   * @param backendNodeId Identifier of the backend node to highlight.
   * @param objectId JavaScript object id of the node to be highlighted.
   * @param selector Selectors to highlight relevant nodes.
   */
  void highlightNode(
      @ParamName("highlightConfig") HighlightConfig highlightConfig,
      @Optional @ParamName("nodeId") Integer nodeId,
      @Optional @ParamName("backendNodeId") Integer backendNodeId,
      @Optional @ParamName("objectId") String objectId,
      @Optional @ParamName("selector") String selector);

  /**
   * Highlights given quad. Coordinates are absolute with respect to the main frame viewport.
   *
   * @param quad Quad to highlight
   */
  void highlightQuad(@ParamName("quad") List<Double> quad);

  /**
   * Highlights given quad. Coordinates are absolute with respect to the main frame viewport.
   *
   * @param quad Quad to highlight
   * @param color The highlight fill color (default: transparent).
   * @param outlineColor The highlight outline color (default: transparent).
   */
  void highlightQuad(
      @ParamName("quad") List<Double> quad,
      @Optional @ParamName("color") RGBA color,
      @Optional @ParamName("outlineColor") RGBA outlineColor);

  /**
   * Highlights given rectangle. Coordinates are absolute with respect to the main frame viewport.
   *
   * @param x X coordinate
   * @param y Y coordinate
   * @param width Rectangle width
   * @param height Rectangle height
   */
  void highlightRect(
      @ParamName("x") Integer x,
      @ParamName("y") Integer y,
      @ParamName("width") Integer width,
      @ParamName("height") Integer height);

  /**
   * Highlights given rectangle. Coordinates are absolute with respect to the main frame viewport.
   *
   * @param x X coordinate
   * @param y Y coordinate
   * @param width Rectangle width
   * @param height Rectangle height
   * @param color The highlight fill color (default: transparent).
   * @param outlineColor The highlight outline color (default: transparent).
   */
  void highlightRect(
      @ParamName("x") Integer x,
      @ParamName("y") Integer y,
      @ParamName("width") Integer width,
      @ParamName("height") Integer height,
      @Optional @ParamName("color") RGBA color,
      @Optional @ParamName("outlineColor") RGBA outlineColor);

  /**
   * Enters the 'inspect' mode. In this mode, elements that user is hovering over are highlighted.
   * Backend then generates 'inspectNodeRequested' event upon element selection.
   *
   * @param mode Set an inspection mode.
   */
  void setInspectMode(@ParamName("mode") InspectMode mode);

  /**
   * Enters the 'inspect' mode. In this mode, elements that user is hovering over are highlighted.
   * Backend then generates 'inspectNodeRequested' event upon element selection.
   *
   * @param mode Set an inspection mode.
   * @param highlightConfig A descriptor for the highlight appearance of hovered-over nodes. May be
   *     omitted if `enabled == false`.
   */
  void setInspectMode(
      @ParamName("mode") InspectMode mode,
      @Optional @ParamName("highlightConfig") HighlightConfig highlightConfig);

  /**
   * Highlights owner element of all frames detected to be ads.
   *
   * @param show True for showing ad highlights
   */
  void setShowAdHighlights(@ParamName("show") Boolean show);

  void setPausedInDebuggerMessage();

  /** @param message The message to display, also triggers resume and step over controls. */
  void setPausedInDebuggerMessage(@Optional @ParamName("message") String message);

  /**
   * Requests that backend shows debug borders on layers
   *
   * @param show True for showing debug borders
   */
  void setShowDebugBorders(@ParamName("show") Boolean show);

  /**
   * Requests that backend shows the FPS counter
   *
   * @param show True for showing the FPS counter
   */
  void setShowFPSCounter(@ParamName("show") Boolean show);

  /**
   * Requests that backend shows paint rectangles
   *
   * @param result True for showing paint rectangles
   */
  void setShowPaintRects(@ParamName("result") Boolean result);

  /**
   * Requests that backend shows layout shift regions
   *
   * @param result True for showing layout shift regions
   */
  void setShowLayoutShiftRegions(@ParamName("result") Boolean result);

  /**
   * Requests that backend shows scroll bottleneck rects
   *
   * @param show True for showing scroll bottleneck rects
   */
  void setShowScrollBottleneckRects(@ParamName("show") Boolean show);

  /**
   * Requests that backend shows hit-test borders on layers
   *
   * @param show True for showing hit-test borders
   */
  void setShowHitTestBorders(@ParamName("show") Boolean show);

  /**
   * Paints viewport size upon main frame resize.
   *
   * @param show Whether to paint size or not.
   */
  void setShowViewportSizeOnResize(@ParamName("show") Boolean show);

  /**
   * Fired when the node should be inspected. This happens after call to `setInspectMode` or when
   * user manually inspects an element.
   */
  @EventName("inspectNodeRequested")
  EventListener onInspectNodeRequested(EventHandler<InspectNodeRequested> eventListener);

  /** Fired when the node should be highlighted. This happens after call to `setInspectMode`. */
  @EventName("nodeHighlightRequested")
  EventListener onNodeHighlightRequested(EventHandler<NodeHighlightRequested> eventListener);

  /** Fired when user asks to capture screenshot of some area on the page. */
  @EventName("screenshotRequested")
  EventListener onScreenshotRequested(EventHandler<ScreenshotRequested> eventListener);

  /** Fired when user cancels the inspect mode. */
  @EventName("inspectModeCanceled")
  EventListener onInspectModeCanceled(EventHandler<InspectModeCanceled> eventListener);
}
