package ai.platon.cdt.kt.protocol.types.page

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String
import kotlin.collections.List

public data class AppManifest(
  @JsonProperty("url")
  public val url: String,
  @JsonProperty("errors")
  public val errors: List<AppManifestError>,
  @JsonProperty("data")
  @Optional
  public val `data`: String? = null,
  @JsonProperty("parsed")
  @Optional
  @Experimental
  public val parsed: AppManifestParsedProperties? = null,
)
