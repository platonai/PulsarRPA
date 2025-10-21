package ai.platon.cdt.kt.protocol.types.debugger

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

public data class ScriptSource(
  @JsonProperty("scriptSource")
  public val scriptSource: String,
  @JsonProperty("bytecode")
  @Optional
  public val bytecode: String? = null,
)
