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

import com.github.kklisura.cdt.protocol.events.media.PlayerEventsAdded;
import com.github.kklisura.cdt.protocol.events.media.PlayerPropertiesChanged;
import com.github.kklisura.cdt.protocol.events.media.PlayersCreated;
import com.github.kklisura.cdt.protocol.support.annotations.EventName;
import com.github.kklisura.cdt.protocol.support.annotations.Experimental;
import com.github.kklisura.cdt.protocol.support.types.EventHandler;
import com.github.kklisura.cdt.protocol.support.types.EventListener;

/** This domain allows detailed inspection of media elements */
@Experimental
public interface Media {

  /** Enables the Media domain */
  void enable();

  /** Disables the Media domain. */
  void disable();

  /**
   * This can be called multiple times, and can be used to set / override / remove player
   * properties. A null propValue indicates removal.
   */
  @EventName("playerPropertiesChanged")
  EventListener onPlayerPropertiesChanged(EventHandler<PlayerPropertiesChanged> eventListener);

  /**
   * Send events as a list, allowing them to be batched on the browser for less congestion. If
   * batched, events must ALWAYS be in chronological order.
   */
  @EventName("playerEventsAdded")
  EventListener onPlayerEventsAdded(EventHandler<PlayerEventsAdded> eventListener);

  /**
   * Called whenever a player is created, or when a new agent joins and recieves a list of active
   * players. If an agent is restored, it will recieve the full list of player ids and all events
   * again.
   */
  @EventName("playersCreated")
  EventListener onPlayersCreated(EventHandler<PlayersCreated> eventListener);
}
