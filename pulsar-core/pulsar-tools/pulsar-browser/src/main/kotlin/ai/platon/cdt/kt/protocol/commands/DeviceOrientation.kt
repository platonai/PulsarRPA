@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.commands

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.ParamName
import kotlin.Double

@Experimental
interface DeviceOrientation {
  /**
   * Clears the overridden Device Orientation.
   */
  suspend fun clearDeviceOrientationOverride()

  /**
   * Overrides the Device Orientation.
   * @param alpha Mock alpha
   * @param beta Mock beta
   * @param gamma Mock gamma
   */
  suspend fun setDeviceOrientationOverride(
    @ParamName("alpha") alpha: Double,
    @ParamName("beta") beta: Double,
    @ParamName("gamma") gamma: Double,
  )
}
