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

import com.github.kklisura.cdt.protocol.v2023.support.annotations.Experimental;

import java.util.List;

@Experimental
public class WasmDisassemblyChunk {

  private List<String> lines;

  private List<Integer> bytecodeOffsets;

  /** The next chunk of disassembled lines. */
  public List<String> getLines() {
    return lines;
  }

  /** The next chunk of disassembled lines. */
  public void setLines(List<String> lines) {
    this.lines = lines;
  }

  /** The bytecode offsets describing the start of each line. */
  public List<Integer> getBytecodeOffsets() {
    return bytecodeOffsets;
  }

  /** The bytecode offsets describing the start of each line. */
  public void setBytecodeOffsets(List<Integer> bytecodeOffsets) {
    this.bytecodeOffsets = bytecodeOffsets;
  }
}
