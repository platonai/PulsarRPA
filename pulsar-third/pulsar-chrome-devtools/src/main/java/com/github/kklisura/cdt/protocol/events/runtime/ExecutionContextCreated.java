package com.github.kklisura.cdt.protocol.events.runtime;

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

import com.github.kklisura.cdt.protocol.types.runtime.ExecutionContextDescription;

/** Issued when new execution context is created. */
public class ExecutionContextCreated {

  private ExecutionContextDescription context;

  /** A newly created execution context. */
  public ExecutionContextDescription getContext() {
    return context;
  }

  /** A newly created execution context. */
  public void setContext(ExecutionContextDescription context) {
    this.context = context;
  }
}
