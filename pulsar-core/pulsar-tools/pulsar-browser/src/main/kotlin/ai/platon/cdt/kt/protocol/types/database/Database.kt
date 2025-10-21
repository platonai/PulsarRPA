package ai.platon.cdt.kt.protocol.types.database

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Database object.
 */
public data class Database(
  @JsonProperty("id")
  public val id: String,
  @JsonProperty("domain")
  public val domain: String,
  @JsonProperty("name")
  public val name: String,
  @JsonProperty("version")
  public val version: String,
)
