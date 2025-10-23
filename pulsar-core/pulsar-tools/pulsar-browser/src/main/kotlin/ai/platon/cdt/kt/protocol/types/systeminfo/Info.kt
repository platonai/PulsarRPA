@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.systeminfo

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

data class Info(
  @param:JsonProperty("gpu")
  val gpu: GPUInfo,
  @param:JsonProperty("modelName")
  val modelName: String,
  @param:JsonProperty("modelVersion")
  val modelVersion: String,
  @param:JsonProperty("commandLine")
  val commandLine: String,
)
