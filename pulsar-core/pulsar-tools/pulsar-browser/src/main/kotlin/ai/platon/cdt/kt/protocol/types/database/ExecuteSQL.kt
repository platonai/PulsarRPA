package ai.platon.cdt.kt.protocol.types.database

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Any
import kotlin.String
import kotlin.collections.List

public data class ExecuteSQL(
  @JsonProperty("columnNames")
  @Optional
  public val columnNames: List<String>? = null,
  @JsonProperty("values")
  @Optional
  public val values: List<Any?>? = null,
  @JsonProperty("sqlError")
  @Optional
  public val sqlError: Error? = null,
)
