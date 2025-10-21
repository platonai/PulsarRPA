package ai.platon.cdt.kt.protocol.events.applicationcache

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

public data class ApplicationCacheStatusUpdated(
  @JsonProperty("frameId")
  public val frameId: String,
  @JsonProperty("manifestURL")
  public val manifestURL: String,
  @JsonProperty("status")
  public val status: Int,
)
