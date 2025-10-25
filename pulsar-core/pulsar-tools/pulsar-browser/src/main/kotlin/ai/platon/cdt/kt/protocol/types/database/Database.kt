@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.database

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Database object.
 */
data class Database(
  @param:JsonProperty("id")
  val id: String,
  @param:JsonProperty("domain")
  val domain: String,
  @param:JsonProperty("name")
  val name: String,
  @param:JsonProperty("version")
  val version: String,
)
