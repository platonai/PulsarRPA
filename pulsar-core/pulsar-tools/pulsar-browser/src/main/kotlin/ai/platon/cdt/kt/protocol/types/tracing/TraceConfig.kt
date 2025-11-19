@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.tracing

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Any
import kotlin.Boolean
import kotlin.String
import kotlin.collections.List
import kotlin.collections.Map

data class TraceConfig(
  @param:JsonProperty("recordMode")
  @param:Optional
  val recordMode: TraceConfigRecordMode? = null,
  @param:JsonProperty("enableSampling")
  @param:Optional
  val enableSampling: Boolean? = null,
  @param:JsonProperty("enableSystrace")
  @param:Optional
  val enableSystrace: Boolean? = null,
  @param:JsonProperty("enableArgumentFilter")
  @param:Optional
  val enableArgumentFilter: Boolean? = null,
  @param:JsonProperty("includedCategories")
  @param:Optional
  val includedCategories: List<String>? = null,
  @param:JsonProperty("excludedCategories")
  @param:Optional
  val excludedCategories: List<String>? = null,
  @param:JsonProperty("syntheticDelays")
  @param:Optional
  val syntheticDelays: List<String>? = null,
  @param:JsonProperty("memoryDumpConfig")
  @param:Optional
  val memoryDumpConfig: Map<String, Any?>? = null,
)
