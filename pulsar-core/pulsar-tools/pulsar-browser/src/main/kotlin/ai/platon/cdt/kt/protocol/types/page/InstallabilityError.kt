@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.page

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String
import kotlin.collections.List

/**
 * The installability error
 */
@Experimental
data class InstallabilityError(
  @param:JsonProperty("errorId")
  val errorId: String,
  @param:JsonProperty("errorArguments")
  val errorArguments: List<InstallabilityErrorArgument>,
)
