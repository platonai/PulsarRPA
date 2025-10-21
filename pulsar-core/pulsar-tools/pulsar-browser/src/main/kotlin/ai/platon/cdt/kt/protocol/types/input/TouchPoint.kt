package ai.platon.cdt.kt.protocol.types.input

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.Int

public data class TouchPoint(
  @JsonProperty("x")
  public val x: Double,
  @JsonProperty("y")
  public val y: Double,
  @JsonProperty("radiusX")
  @Optional
  public val radiusX: Double? = null,
  @JsonProperty("radiusY")
  @Optional
  public val radiusY: Double? = null,
  @JsonProperty("rotationAngle")
  @Optional
  public val rotationAngle: Double? = null,
  @JsonProperty("force")
  @Optional
  public val force: Double? = null,
  @JsonProperty("tangentialPressure")
  @Optional
  @Experimental
  public val tangentialPressure: Double? = null,
  @JsonProperty("tiltX")
  @Optional
  @Experimental
  public val tiltX: Int? = null,
  @JsonProperty("tiltY")
  @Optional
  @Experimental
  public val tiltY: Int? = null,
  @JsonProperty("twist")
  @Optional
  @Experimental
  public val twist: Int? = null,
  @JsonProperty("id")
  @Optional
  public val id: Double? = null,
)
