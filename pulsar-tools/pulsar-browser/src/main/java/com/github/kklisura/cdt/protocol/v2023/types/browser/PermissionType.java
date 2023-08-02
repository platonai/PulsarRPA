package com.github.kklisura.cdt.protocol.v2023.types.browser;

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

import com.fasterxml.jackson.annotation.JsonProperty;

public enum PermissionType {
  @JsonProperty("accessibilityEvents")
  ACCESSIBILITY_EVENTS,
  @JsonProperty("audioCapture")
  AUDIO_CAPTURE,
  @JsonProperty("backgroundSync")
  BACKGROUND_SYNC,
  @JsonProperty("backgroundFetch")
  BACKGROUND_FETCH,
  @JsonProperty("clipboardReadWrite")
  CLIPBOARD_READ_WRITE,
  @JsonProperty("clipboardSanitizedWrite")
  CLIPBOARD_SANITIZED_WRITE,
  @JsonProperty("displayCapture")
  DISPLAY_CAPTURE,
  @JsonProperty("durableStorage")
  DURABLE_STORAGE,
  @JsonProperty("flash")
  FLASH,
  @JsonProperty("geolocation")
  GEOLOCATION,
  @JsonProperty("idleDetection")
  IDLE_DETECTION,
  @JsonProperty("localFonts")
  LOCAL_FONTS,
  @JsonProperty("midi")
  MIDI,
  @JsonProperty("midiSysex")
  MIDI_SYSEX,
  @JsonProperty("nfc")
  NFC,
  @JsonProperty("notifications")
  NOTIFICATIONS,
  @JsonProperty("paymentHandler")
  PAYMENT_HANDLER,
  @JsonProperty("periodicBackgroundSync")
  PERIODIC_BACKGROUND_SYNC,
  @JsonProperty("protectedMediaIdentifier")
  PROTECTED_MEDIA_IDENTIFIER,
  @JsonProperty("sensors")
  SENSORS,
  @JsonProperty("storageAccess")
  STORAGE_ACCESS,
  @JsonProperty("topLevelStorageAccess")
  TOP_LEVEL_STORAGE_ACCESS,
  @JsonProperty("videoCapture")
  VIDEO_CAPTURE,
  @JsonProperty("videoCapturePanTiltZoom")
  VIDEO_CAPTURE_PAN_TILT_ZOOM,
  @JsonProperty("wakeLockScreen")
  WAKE_LOCK_SCREEN,
  @JsonProperty("wakeLockSystem")
  WAKE_LOCK_SYSTEM,
  @JsonProperty("windowManagement")
  WINDOW_MANAGEMENT
}
