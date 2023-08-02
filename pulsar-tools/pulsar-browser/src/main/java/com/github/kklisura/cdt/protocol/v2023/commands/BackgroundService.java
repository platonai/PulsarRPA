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

import com.github.kklisura.cdt.protocol.v2023.events.backgroundservice.BackgroundServiceEventReceived;
import com.github.kklisura.cdt.protocol.v2023.events.backgroundservice.RecordingStateChanged;
import com.github.kklisura.cdt.protocol.v2023.support.annotations.EventName;
import com.github.kklisura.cdt.protocol.v2023.support.annotations.Experimental;
import com.github.kklisura.cdt.protocol.v2023.support.annotations.ParamName;
import com.github.kklisura.cdt.protocol.v2023.support.types.EventHandler;
import com.github.kklisura.cdt.protocol.v2023.support.types.EventListener;
import com.github.kklisura.cdt.protocol.v2023.types.backgroundservice.ServiceName;

/** Defines events for background web platform features. */
@Experimental
public interface BackgroundService {

  /**
   * Enables event updates for the service.
   *
   * @param service
   */
  void startObserving(@ParamName("service") ServiceName service);

  /**
   * Disables event updates for the service.
   *
   * @param service
   */
  void stopObserving(@ParamName("service") ServiceName service);

  /**
   * Set the recording state for the service.
   *
   * @param shouldRecord
   * @param service
   */
  void setRecording(
      @ParamName("shouldRecord") Boolean shouldRecord, @ParamName("service") ServiceName service);

  /**
   * Clears all stored data for the service.
   *
   * @param service
   */
  void clearEvents(@ParamName("service") ServiceName service);

  /** Called when the recording state for the service has been updated. */
  @EventName("recordingStateChanged")
  EventListener onRecordingStateChanged(EventHandler<RecordingStateChanged> eventListener);

  /**
   * Called with all existing backgroundServiceEvents when enabled, and all new events afterwards if
   * enabled and recording.
   */
  @EventName("backgroundServiceEventReceived")
  EventListener onBackgroundServiceEventReceived(
      EventHandler<BackgroundServiceEventReceived> eventListener);
}
