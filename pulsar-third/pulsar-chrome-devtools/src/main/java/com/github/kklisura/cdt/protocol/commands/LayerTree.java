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

import com.github.kklisura.cdt.protocol.events.layertree.LayerPainted;
import com.github.kklisura.cdt.protocol.events.layertree.LayerTreeDidChange;
import com.github.kklisura.cdt.protocol.support.annotations.EventName;
import com.github.kklisura.cdt.protocol.support.annotations.Experimental;
import com.github.kklisura.cdt.protocol.support.annotations.Optional;
import com.github.kklisura.cdt.protocol.support.annotations.ParamName;
import com.github.kklisura.cdt.protocol.support.annotations.ReturnTypeParameter;
import com.github.kklisura.cdt.protocol.support.annotations.Returns;
import com.github.kklisura.cdt.protocol.support.types.EventHandler;
import com.github.kklisura.cdt.protocol.support.types.EventListener;
import com.github.kklisura.cdt.protocol.types.dom.Rect;
import com.github.kklisura.cdt.protocol.types.layertree.PictureTile;
import java.util.List;

@Experimental
public interface LayerTree {

  /**
   * Provides the reasons why the given layer was composited.
   *
   * @param layerId The id of the layer for which we want to get the reasons it was composited.
   */
  @Returns("compositingReasons")
  @ReturnTypeParameter(String.class)
  List<String> compositingReasons(@ParamName("layerId") String layerId);

  /** Disables compositing tree inspection. */
  void disable();

  /** Enables compositing tree inspection. */
  void enable();

  /**
   * Returns the snapshot identifier.
   *
   * @param tiles An array of tiles composing the snapshot.
   */
  @Returns("snapshotId")
  String loadSnapshot(@ParamName("tiles") List<PictureTile> tiles);

  /**
   * Returns the layer snapshot identifier.
   *
   * @param layerId The id of the layer.
   */
  @Returns("snapshotId")
  String makeSnapshot(@ParamName("layerId") String layerId);

  /** @param snapshotId The id of the layer snapshot. */
  @Returns("timings")
  @ReturnTypeParameter({List.class, Double.class})
  List<List<Double>> profileSnapshot(@ParamName("snapshotId") String snapshotId);

  /**
   * @param snapshotId The id of the layer snapshot.
   * @param minRepeatCount The maximum number of times to replay the snapshot (1, if not specified).
   * @param minDuration The minimum duration (in seconds) to replay the snapshot.
   * @param clipRect The clip rectangle to apply when replaying the snapshot.
   */
  @Returns("timings")
  @ReturnTypeParameter({List.class, Double.class})
  List<List<Double>> profileSnapshot(
      @ParamName("snapshotId") String snapshotId,
      @Optional @ParamName("minRepeatCount") Integer minRepeatCount,
      @Optional @ParamName("minDuration") Double minDuration,
      @Optional @ParamName("clipRect") Rect clipRect);

  /**
   * Releases layer snapshot captured by the back-end.
   *
   * @param snapshotId The id of the layer snapshot.
   */
  void releaseSnapshot(@ParamName("snapshotId") String snapshotId);

  /**
   * Replays the layer snapshot and returns the resulting bitmap.
   *
   * @param snapshotId The id of the layer snapshot.
   */
  @Returns("dataURL")
  String replaySnapshot(@ParamName("snapshotId") String snapshotId);

  /**
   * Replays the layer snapshot and returns the resulting bitmap.
   *
   * @param snapshotId The id of the layer snapshot.
   * @param fromStep The first step to replay from (replay from the very start if not specified).
   * @param toStep The last step to replay to (replay till the end if not specified).
   * @param scale The scale to apply while replaying (defaults to 1).
   */
  @Returns("dataURL")
  String replaySnapshot(
      @ParamName("snapshotId") String snapshotId,
      @Optional @ParamName("fromStep") Integer fromStep,
      @Optional @ParamName("toStep") Integer toStep,
      @Optional @ParamName("scale") Double scale);

  /**
   * Replays the layer snapshot and returns canvas log.
   *
   * @param snapshotId The id of the layer snapshot.
   */
  @Returns("commandLog")
  @ReturnTypeParameter(Object.class)
  List<Object> snapshotCommandLog(@ParamName("snapshotId") String snapshotId);

  @EventName("layerPainted")
  EventListener onLayerPainted(EventHandler<LayerPainted> eventListener);

  @EventName("layerTreeDidChange")
  EventListener onLayerTreeDidChange(EventHandler<LayerTreeDidChange> eventListener);
}
