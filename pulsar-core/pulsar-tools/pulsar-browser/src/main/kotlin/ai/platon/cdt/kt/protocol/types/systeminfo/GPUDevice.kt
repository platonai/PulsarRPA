package ai.platon.cdt.kt.protocol.types.systeminfo

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String

/**
 * Describes a single graphics processor (GPU).
 */
public data class GPUDevice(
  @JsonProperty("vendorId")
  public val vendorId: Double,
  @JsonProperty("deviceId")
  public val deviceId: Double,
  @JsonProperty("subSysId")
  @Optional
  public val subSysId: Double? = null,
  @JsonProperty("revision")
  @Optional
  public val revision: Double? = null,
  @JsonProperty("vendorString")
  public val vendorString: String,
  @JsonProperty("deviceString")
  public val deviceString: String,
  @JsonProperty("driverVendor")
  public val driverVendor: String,
  @JsonProperty("driverVersion")
  public val driverVersion: String,
)
