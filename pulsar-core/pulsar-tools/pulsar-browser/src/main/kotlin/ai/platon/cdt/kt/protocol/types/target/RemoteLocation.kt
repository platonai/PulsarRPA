package ai.platon.cdt.kt.protocol.types.target

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

@Experimental
public data class RemoteLocation(
  @JsonProperty("host")
  public val host: String,
  @JsonProperty("port")
  public val port: Int,
)
