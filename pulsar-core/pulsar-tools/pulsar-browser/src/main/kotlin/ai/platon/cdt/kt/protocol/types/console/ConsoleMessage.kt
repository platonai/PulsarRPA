package ai.platon.cdt.kt.protocol.types.console

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

/**
 * Console message.
 */
public data class ConsoleMessage(
  @JsonProperty("source")
  public val source: ConsoleMessageSource,
  @JsonProperty("level")
  public val level: ConsoleMessageLevel,
  @JsonProperty("text")
  public val text: String,
  @JsonProperty("url")
  @Optional
  public val url: String? = null,
  @JsonProperty("line")
  @Optional
  public val line: Int? = null,
  @JsonProperty("column")
  @Optional
  public val column: Int? = null,
)
