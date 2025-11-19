@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.browser

import com.fasterxml.jackson.`annotation`.JsonProperty

public enum class PermissionType {
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
  @JsonProperty("videoCapture")
  VIDEO_CAPTURE,
  @JsonProperty("videoCapturePanTiltZoom")
  VIDEO_CAPTURE_PAN_TILT_ZOOM,
  @JsonProperty("idleDetection")
  IDLE_DETECTION,
  @JsonProperty("wakeLockScreen")
  WAKE_LOCK_SCREEN,
  @JsonProperty("wakeLockSystem")
  WAKE_LOCK_SYSTEM,
}
