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

import com.github.kklisura.cdt.protocol.events.inspector.Detached;
import com.github.kklisura.cdt.protocol.events.inspector.TargetCrashed;
import com.github.kklisura.cdt.protocol.events.inspector.TargetReloadedAfterCrash;
import com.github.kklisura.cdt.protocol.support.annotations.EventName;
import com.github.kklisura.cdt.protocol.support.annotations.Experimental;
import com.github.kklisura.cdt.protocol.support.types.EventHandler;
import com.github.kklisura.cdt.protocol.support.types.EventListener;

@Experimental
public interface Inspector {

  /** Disables inspector domain notifications. */
  void disable();

  /** Enables inspector domain notifications. */
  void enable();

  /** Fired when remote debugging connection is about to be terminated. Contains detach reason. */
  @EventName("detached")
  EventListener onDetached(EventHandler<Detached> eventListener);

  /** Fired when debugging target has crashed */
  @EventName("targetCrashed")
  EventListener onTargetCrashed(EventHandler<TargetCrashed> eventListener);

  /** Fired when debugging target has reloaded after crash */
  @EventName("targetReloadedAfterCrash")
  EventListener onTargetReloadedAfterCrash(EventHandler<TargetReloadedAfterCrash> eventListener);
}
