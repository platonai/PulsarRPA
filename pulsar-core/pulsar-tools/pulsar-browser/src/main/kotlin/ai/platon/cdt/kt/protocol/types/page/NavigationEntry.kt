package ai.platon.cdt.kt.protocol.types.page

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

/**
 * Navigation history entry.
 */
public data class NavigationEntry(
  @JsonProperty("id")
  public val id: Int,
  @JsonProperty("url")
  public val url: String,
  @JsonProperty("userTypedURL")
  public val userTypedURL: String,
  @JsonProperty("title")
  public val title: String,
  @JsonProperty("transitionType")
  public val transitionType: TransitionType,
)
