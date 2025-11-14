@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.network

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Post data entry for HTTP request
 */
data class PostDataEntry(
  @param:JsonProperty("bytes")
  @param:Optional
  val bytes: String? = null,
)
