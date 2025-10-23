@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.database

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

/**
 * Database error.
 */
data class Error(
  @param:JsonProperty("message")
  val message: String,
  @param:JsonProperty("code")
  val code: Int,
)
