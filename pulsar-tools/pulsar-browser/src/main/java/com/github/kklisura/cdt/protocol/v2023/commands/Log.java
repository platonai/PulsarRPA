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

import com.github.kklisura.cdt.protocol.v2023.events.log.EntryAdded;
import com.github.kklisura.cdt.protocol.v2023.support.annotations.EventName;
import com.github.kklisura.cdt.protocol.v2023.support.annotations.ParamName;
import com.github.kklisura.cdt.protocol.v2023.support.types.EventHandler;
import com.github.kklisura.cdt.protocol.v2023.support.types.EventListener;
import com.github.kklisura.cdt.protocol.v2023.types.log.ViolationSetting;

import java.util.List;

/** Provides access to log entries. */
public interface Log {

  /** Clears the log. */
  void clear();

  /** Disables log domain, prevents further log entries from being reported to the client. */
  void disable();

  /**
   * Enables log domain, sends the entries collected so far to the client by means of the
   * `entryAdded` notification.
   */
  void enable();

  /**
   * start violation reporting.
   *
   * @param config Configuration for violations.
   */
  void startViolationsReport(@ParamName("config") List<ViolationSetting> config);

  /** Stop violation reporting. */
  void stopViolationsReport();

  /** Issued when new message was logged. */
  @EventName("entryAdded")
  EventListener onEntryAdded(EventHandler<EntryAdded> eventListener);
}
