@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.systeminfo

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String

/**
 * Describes a single graphics processor (GPU).
 */
data class GPUDevice(
  @param:JsonProperty("vendorId")
  val vendorId: Double,
  @param:JsonProperty("deviceId")
  val deviceId: Double,
  @param:JsonProperty("subSysId")
  @param:Optional
  val subSysId: Double? = null,
  @param:JsonProperty("revision")
  @param:Optional
  val revision: Double? = null,
  @param:JsonProperty("vendorString")
  val vendorString: String,
  @param:JsonProperty("deviceString")
  val deviceString: String,
  @param:JsonProperty("driverVendor")
  val driverVendor: String,
  @param:JsonProperty("driverVersion")
  val driverVersion: String,
)
