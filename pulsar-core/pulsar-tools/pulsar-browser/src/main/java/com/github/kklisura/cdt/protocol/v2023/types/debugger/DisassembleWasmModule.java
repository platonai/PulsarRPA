package com.github.kklisura.cdt.protocol.v2023.types.debugger;

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

import com.github.kklisura.cdt.protocol.v2023.support.annotations.Optional;

import java.util.List;

public class DisassembleWasmModule {

  @Optional
  private String streamId;

  private Integer totalNumberOfLines;

  private List<Integer> functionBodyOffsets;

  private WasmDisassemblyChunk chunk;

  /**
   * For large modules, return a stream from which additional chunks of disassembly can be read
   * successively.
   */
  public String getStreamId() {
    return streamId;
  }

  /**
   * For large modules, return a stream from which additional chunks of disassembly can be read
   * successively.
   */
  public void setStreamId(String streamId) {
    this.streamId = streamId;
  }

  /** The total number of lines in the disassembly text. */
  public Integer getTotalNumberOfLines() {
    return totalNumberOfLines;
  }

  /** The total number of lines in the disassembly text. */
  public void setTotalNumberOfLines(Integer totalNumberOfLines) {
    this.totalNumberOfLines = totalNumberOfLines;
  }

  /**
   * The offsets of all function bodies, in the format [start1, end1, start2, end2, ...] where all
   * ends are exclusive.
   */
  public List<Integer> getFunctionBodyOffsets() {
    return functionBodyOffsets;
  }

  /**
   * The offsets of all function bodies, in the format [start1, end1, start2, end2, ...] where all
   * ends are exclusive.
   */
  public void setFunctionBodyOffsets(List<Integer> functionBodyOffsets) {
    this.functionBodyOffsets = functionBodyOffsets;
  }

  /** The first chunk of disassembly. */
  public WasmDisassemblyChunk getChunk() {
    return chunk;
  }

  /** The first chunk of disassembly. */
  public void setChunk(WasmDisassemblyChunk chunk) {
    this.chunk = chunk;
  }
}
