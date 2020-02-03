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

import com.github.kklisura.cdt.protocol.events.tracing.BufferUsage;
import com.github.kklisura.cdt.protocol.events.tracing.DataCollected;
import com.github.kklisura.cdt.protocol.events.tracing.TracingComplete;
import com.github.kklisura.cdt.protocol.support.annotations.EventName;
import com.github.kklisura.cdt.protocol.support.annotations.Experimental;
import com.github.kklisura.cdt.protocol.support.annotations.Optional;
import com.github.kklisura.cdt.protocol.support.annotations.ParamName;
import com.github.kklisura.cdt.protocol.support.annotations.ReturnTypeParameter;
import com.github.kklisura.cdt.protocol.support.annotations.Returns;
import com.github.kklisura.cdt.protocol.support.types.EventHandler;
import com.github.kklisura.cdt.protocol.support.types.EventListener;
import com.github.kklisura.cdt.protocol.types.tracing.RequestMemoryDump;
import com.github.kklisura.cdt.protocol.types.tracing.StartTransferMode;
import com.github.kklisura.cdt.protocol.types.tracing.StreamCompression;
import com.github.kklisura.cdt.protocol.types.tracing.StreamFormat;
import com.github.kklisura.cdt.protocol.types.tracing.TraceConfig;
import java.util.List;

@Experimental
public interface Tracing {

  /** Stop trace events collection. */
  void end();

  /** Gets supported tracing categories. */
  @Returns("categories")
  @ReturnTypeParameter(String.class)
  List<String> getCategories();

  /**
   * Record a clock sync marker in the trace.
   *
   * @param syncId The ID of this clock sync marker
   */
  void recordClockSyncMarker(@ParamName("syncId") String syncId);

  /** Request a global memory dump. */
  RequestMemoryDump requestMemoryDump();

  /** Start trace events collection. */
  void start();

  /**
   * Start trace events collection.
   *
   * @param categories Category/tag filter
   * @param options Tracing options
   * @param bufferUsageReportingInterval If set, the agent will issue bufferUsage events at this
   *     interval, specified in milliseconds
   * @param transferMode Whether to report trace events as series of dataCollected events or to save
   *     trace to a stream (defaults to `ReportEvents`).
   * @param streamFormat Trace data format to use. This only applies when using `ReturnAsStream`
   *     transfer mode (defaults to `json`).
   * @param streamCompression Compression format to use. This only applies when using
   *     `ReturnAsStream` transfer mode (defaults to `none`)
   * @param traceConfig
   */
  void start(
      @Deprecated @Optional @ParamName("categories") String categories,
      @Deprecated @Optional @ParamName("options") String options,
      @Optional @ParamName("bufferUsageReportingInterval") Double bufferUsageReportingInterval,
      @Optional @ParamName("transferMode") StartTransferMode transferMode,
      @Optional @ParamName("streamFormat") StreamFormat streamFormat,
      @Optional @ParamName("streamCompression") StreamCompression streamCompression,
      @Optional @ParamName("traceConfig") TraceConfig traceConfig);

  @EventName("bufferUsage")
  EventListener onBufferUsage(EventHandler<BufferUsage> eventListener);

  /**
   * Contains an bucket of collected trace events. When tracing is stopped collected events will be
   * send as a sequence of dataCollected events followed by tracingComplete event.
   */
  @EventName("dataCollected")
  EventListener onDataCollected(EventHandler<DataCollected> eventListener);

  /**
   * Signals that tracing is stopped and there is no trace buffers pending flush, all data were
   * delivered via dataCollected events.
   */
  @EventName("tracingComplete")
  EventListener onTracingComplete(EventHandler<TracingComplete> eventListener);
}
