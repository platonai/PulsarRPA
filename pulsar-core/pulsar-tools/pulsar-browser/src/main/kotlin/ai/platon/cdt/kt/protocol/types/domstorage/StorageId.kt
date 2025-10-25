@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.domstorage

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.String

/**
 * DOM Storage identifier.
 */
data class StorageId(
  @param:JsonProperty("securityOrigin")
  val securityOrigin: String,
  @param:JsonProperty("isLocalStorage")
  val isLocalStorage: Boolean,
)
