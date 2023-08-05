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

public class ScriptSource {

  private String scriptSource;

  @Optional
  private String bytecode;

  /** Script source (empty in case of Wasm bytecode). */
  public String getScriptSource() {
    return scriptSource;
  }

  /** Script source (empty in case of Wasm bytecode). */
  public void setScriptSource(String scriptSource) {
    this.scriptSource = scriptSource;
  }

  /** Wasm bytecode. (Encoded as a base64 string when passed over JSON) */
  public String getBytecode() {
    return bytecode;
  }

  /** Wasm bytecode. (Encoded as a base64 string when passed over JSON) */
  public void setBytecode(String bytecode) {
    this.bytecode = bytecode;
  }
}
