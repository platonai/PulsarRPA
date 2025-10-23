@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.input

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.Int

data class TouchPoint(
  @param:JsonProperty("x")
  val x: Double,
  @param:JsonProperty("y")
  val y: Double,
  @param:JsonProperty("radiusX")
  @param:Optional
  val radiusX: Double? = null,
  @param:JsonProperty("radiusY")
  @param:Optional
  val radiusY: Double? = null,
  @param:JsonProperty("rotationAngle")
  @param:Optional
  val rotationAngle: Double? = null,
  @param:JsonProperty("force")
  @param:Optional
  val force: Double? = null,
  @param:JsonProperty("tangentialPressure")
  @param:Optional
  @param:Experimental
  val tangentialPressure: Double? = null,
  @param:JsonProperty("tiltX")
  @param:Optional
  @param:Experimental
  val tiltX: Int? = null,
  @param:JsonProperty("tiltY")
  @param:Optional
  @param:Experimental
  val tiltY: Int? = null,
  @param:JsonProperty("twist")
  @param:Optional
  @param:Experimental
  val twist: Int? = null,
  @param:JsonProperty("id")
  @param:Optional
  val id: Double? = null,
)
