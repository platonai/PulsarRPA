package com.github.kklisura.cdt.protocol.events.tracing;

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

import com.github.kklisura.cdt.protocol.support.annotations.Optional;
import com.github.kklisura.cdt.protocol.types.tracing.StreamCompression;
import com.github.kklisura.cdt.protocol.types.tracing.StreamFormat;

/**
 * Signals that tracing is stopped and there is no trace buffers pending flush, all data were
 * delivered via dataCollected events.
 */
public class TracingComplete {

  private Boolean dataLossOccurred;

  @Optional private String stream;

  @Optional private StreamFormat traceFormat;

  @Optional private StreamCompression streamCompression;

  /**
   * Indicates whether some trace data is known to have been lost, e.g. because the trace ring
   * buffer wrapped around.
   */
  public Boolean getDataLossOccurred() {
    return dataLossOccurred;
  }

  /**
   * Indicates whether some trace data is known to have been lost, e.g. because the trace ring
   * buffer wrapped around.
   */
  public void setDataLossOccurred(Boolean dataLossOccurred) {
    this.dataLossOccurred = dataLossOccurred;
  }

  /** A handle of the stream that holds resulting trace data. */
  public String getStream() {
    return stream;
  }

  /** A handle of the stream that holds resulting trace data. */
  public void setStream(String stream) {
    this.stream = stream;
  }

  /** Trace data format of returned stream. */
  public StreamFormat getTraceFormat() {
    return traceFormat;
  }

  /** Trace data format of returned stream. */
  public void setTraceFormat(StreamFormat traceFormat) {
    this.traceFormat = traceFormat;
  }

  /** Compression format of returned stream. */
  public StreamCompression getStreamCompression() {
    return streamCompression;
  }

  /** Compression format of returned stream. */
  public void setStreamCompression(StreamCompression streamCompression) {
    this.streamCompression = streamCompression;
  }
}
