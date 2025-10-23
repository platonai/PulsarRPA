@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.database

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Any
import kotlin.String
import kotlin.collections.List

data class ExecuteSQL(
  @param:JsonProperty("columnNames")
  @param:Optional
  val columnNames: List<String>? = null,
  @param:JsonProperty("values")
  @param:Optional
  val values: List<Any?>? = null,
  @param:JsonProperty("sqlError")
  @param:Optional
  val sqlError: Error? = null,
)
