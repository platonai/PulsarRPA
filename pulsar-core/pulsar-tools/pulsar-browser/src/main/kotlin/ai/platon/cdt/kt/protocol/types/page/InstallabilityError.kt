package ai.platon.cdt.kt.protocol.types.page

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String
import kotlin.collections.List

/**
 * The installability error
 */
@Experimental
public data class InstallabilityError(
  @JsonProperty("errorId")
  public val errorId: String,
  @JsonProperty("errorArguments")
  public val errorArguments: List<InstallabilityErrorArgument>,
)
