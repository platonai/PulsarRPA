package ai.platon.cdt.kt.protocol.types.security

import com.fasterxml.jackson.`annotation`.JsonProperty
import java.lang.Deprecated
import kotlin.Boolean

/**
 * Information about insecure content on the page.
 */
@Deprecated
public data class InsecureContentStatus(
  @JsonProperty("ranMixedContent")
  public val ranMixedContent: Boolean,
  @JsonProperty("displayedMixedContent")
  public val displayedMixedContent: Boolean,
  @JsonProperty("containedMixedForm")
  public val containedMixedForm: Boolean,
  @JsonProperty("ranContentWithCertErrors")
  public val ranContentWithCertErrors: Boolean,
  @JsonProperty("displayedContentWithCertErrors")
  public val displayedContentWithCertErrors: Boolean,
  @JsonProperty("ranInsecureContentStyle")
  public val ranInsecureContentStyle: SecurityState,
  @JsonProperty("displayedInsecureContentStyle")
  public val displayedInsecureContentStyle: SecurityState,
)
