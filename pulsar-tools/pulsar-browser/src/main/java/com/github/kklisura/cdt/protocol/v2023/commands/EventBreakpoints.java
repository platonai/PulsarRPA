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

import com.github.kklisura.cdt.protocol.v2023.support.annotations.Experimental;
import com.github.kklisura.cdt.protocol.v2023.support.annotations.ParamName;

/**
 * EventBreakpoints permits setting breakpoints on particular operations and events in targets that
 * run JavaScript but do not have a DOM. JavaScript execution will stop on these operations as if
 * there was a regular breakpoint set.
 */
@Experimental
public interface EventBreakpoints {

  /**
   * Sets breakpoint on particular native event.
   *
   * @param eventName Instrumentation name to stop on.
   */
  void setInstrumentationBreakpoint(@ParamName("eventName") String eventName);

  /**
   * Removes breakpoint on particular native event.
   *
   * @param eventName Instrumentation name to stop on.
   */
  void removeInstrumentationBreakpoint(@ParamName("eventName") String eventName);
}
