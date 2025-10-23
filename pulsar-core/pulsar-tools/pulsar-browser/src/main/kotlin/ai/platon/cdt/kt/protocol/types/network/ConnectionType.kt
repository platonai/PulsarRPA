@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.network

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * The underlying connection technology that the browser is supposedly using.
 */
public enum class ConnectionType {
  @JsonProperty("none")
  NONE,
  @JsonProperty("cellular2g")
  CELLULAR_2G,
  @JsonProperty("cellular3g")
  CELLULAR_3G,
  @JsonProperty("cellular4g")
  CELLULAR_4G,
  @JsonProperty("bluetooth")
  BLUETOOTH,
  @JsonProperty("ethernet")
  ETHERNET,
  @JsonProperty("wifi")
  WIFI,
  @JsonProperty("wimax")
  WIMAX,
  @JsonProperty("other")
  OTHER,
}
