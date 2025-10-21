package ai.platon.cdt.kt.protocol.events.applicationcache

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean

public data class NetworkStateUpdated(
  @JsonProperty("isNowOnline")
  public val isNowOnline: Boolean,
)
