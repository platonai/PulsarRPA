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

import com.github.kklisura.cdt.protocol.v2023.events.heapprofiler.*;
import com.github.kklisura.cdt.protocol.v2023.support.annotations.*;
import com.github.kklisura.cdt.protocol.v2023.support.types.EventHandler;
import com.github.kklisura.cdt.protocol.v2023.support.types.EventListener;
import com.github.kklisura.cdt.protocol.v2023.types.heapprofiler.SamplingHeapProfile;
import com.github.kklisura.cdt.protocol.v2023.types.runtime.RemoteObject;

@Experimental
public interface HeapProfiler {

  /**
   * Enables console to refer to the node with given id via $x (see Command Line API for more
   * details $x functions).
   *
   * @param heapObjectId Heap snapshot object id to be accessible by means of $x command line API.
   */
  void addInspectedHeapObject(@ParamName("heapObjectId") String heapObjectId);

  void collectGarbage();

  void disable();

  void enable();

  /** @param objectId Identifier of the object to get heap object id for. */
  @Returns("heapSnapshotObjectId")
  String getHeapObjectId(@ParamName("objectId") String objectId);

  /** @param objectId */
  @Returns("result")
  RemoteObject getObjectByHeapObjectId(@ParamName("objectId") String objectId);

  /**
   * @param objectId
   * @param objectGroup Symbolic group name that can be used to release multiple objects.
   */
  @Returns("result")
  RemoteObject getObjectByHeapObjectId(
      @ParamName("objectId") String objectId,
      @Optional @ParamName("objectGroup") String objectGroup);

  @Returns("profile")
  SamplingHeapProfile getSamplingProfile();

  void startSampling();

  /**
   * @param samplingInterval Average sample interval in bytes. Poisson distribution is used for the
   *     intervals. The default value is 32768 bytes.
   * @param includeObjectsCollectedByMajorGC By default, the sampling heap profiler reports only
   *     objects which are still alive when the profile is returned via getSamplingProfile or
   *     stopSampling, which is useful for determining what functions contribute the most to
   *     steady-state memory usage. This flag instructs the sampling heap profiler to also include
   *     information about objects discarded by major GC, which will show which functions cause
   *     large temporary memory usage or long GC pauses.
   * @param includeObjectsCollectedByMinorGC By default, the sampling heap profiler reports only
   *     objects which are still alive when the profile is returned via getSamplingProfile or
   *     stopSampling, which is useful for determining what functions contribute the most to
   *     steady-state memory usage. This flag instructs the sampling heap profiler to also include
   *     information about objects discarded by minor GC, which is useful when tuning a
   *     latency-sensitive application for minimal GC activity.
   */
  void startSampling(
      @Optional @ParamName("samplingInterval") Double samplingInterval,
      @Optional @ParamName("includeObjectsCollectedByMajorGC")
          Boolean includeObjectsCollectedByMajorGC,
      @Optional @ParamName("includeObjectsCollectedByMinorGC")
          Boolean includeObjectsCollectedByMinorGC);

  void startTrackingHeapObjects();

  /** @param trackAllocations */
  void startTrackingHeapObjects(@Optional @ParamName("trackAllocations") Boolean trackAllocations);

  @Returns("profile")
  SamplingHeapProfile stopSampling();

  void stopTrackingHeapObjects();

  /**
   * @param reportProgress If true 'reportHeapSnapshotProgress' events will be generated while
   *     snapshot is being taken when the tracking is stopped.
   * @param treatGlobalObjectsAsRoots Deprecated in favor of `exposeInternals`.
   * @param captureNumericValue If true, numerical values are included in the snapshot
   * @param exposeInternals If true, exposes internals of the snapshot.
   */
  void stopTrackingHeapObjects(
      @Optional @ParamName("reportProgress") Boolean reportProgress,
      @Deprecated @Optional @ParamName("treatGlobalObjectsAsRoots")
          Boolean treatGlobalObjectsAsRoots,
      @Optional @ParamName("captureNumericValue") Boolean captureNumericValue,
      @Experimental @Optional @ParamName("exposeInternals") Boolean exposeInternals);

  void takeHeapSnapshot();

  /**
   * @param reportProgress If true 'reportHeapSnapshotProgress' events will be generated while
   *     snapshot is being taken.
   * @param treatGlobalObjectsAsRoots If true, a raw snapshot without artificial roots will be
   *     generated. Deprecated in favor of `exposeInternals`.
   * @param captureNumericValue If true, numerical values are included in the snapshot
   * @param exposeInternals If true, exposes internals of the snapshot.
   */
  void takeHeapSnapshot(
      @Optional @ParamName("reportProgress") Boolean reportProgress,
      @Deprecated @Optional @ParamName("treatGlobalObjectsAsRoots")
          Boolean treatGlobalObjectsAsRoots,
      @Optional @ParamName("captureNumericValue") Boolean captureNumericValue,
      @Experimental @Optional @ParamName("exposeInternals") Boolean exposeInternals);

  @EventName("addHeapSnapshotChunk")
  EventListener onAddHeapSnapshotChunk(EventHandler<AddHeapSnapshotChunk> eventListener);

  /**
   * If heap objects tracking has been started then backend may send update for one or more
   * fragments
   */
  @EventName("heapStatsUpdate")
  EventListener onHeapStatsUpdate(EventHandler<HeapStatsUpdate> eventListener);

  /**
   * If heap objects tracking has been started then backend regularly sends a current value for last
   * seen object id and corresponding timestamp. If the were changes in the heap since last event
   * then one or more heapStatsUpdate events will be sent before a new lastSeenObjectId event.
   */
  @EventName("lastSeenObjectId")
  EventListener onLastSeenObjectId(EventHandler<LastSeenObjectId> eventListener);

  @EventName("reportHeapSnapshotProgress")
  EventListener onReportHeapSnapshotProgress(
      EventHandler<ReportHeapSnapshotProgress> eventListener);

  @EventName("resetProfiles")
  EventListener onResetProfiles(EventHandler<ResetProfiles> eventListener);
}
