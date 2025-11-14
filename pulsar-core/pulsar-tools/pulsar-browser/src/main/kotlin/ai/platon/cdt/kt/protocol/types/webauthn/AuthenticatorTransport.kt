@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.webauthn

import com.fasterxml.jackson.`annotation`.JsonProperty

public enum class AuthenticatorTransport {
  @JsonProperty("usb")
  USB,
  @JsonProperty("nfc")
  NFC,
  @JsonProperty("ble")
  BLE,
  @JsonProperty("cable")
  CABLE,
  @JsonProperty("internal")
  INTERNAL,
}
