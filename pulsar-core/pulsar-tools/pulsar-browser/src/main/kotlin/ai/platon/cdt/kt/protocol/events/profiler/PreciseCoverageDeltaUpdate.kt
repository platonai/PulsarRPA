package ai.platon.cdt.kt.protocol.events.profiler

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.types.profiler.ScriptCoverage
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String
import kotlin.collections.List

/**
 * Reports coverage delta since the last poll (either from an event like this, or from
 * `takePreciseCoverage` for the current isolate. May only be sent if precise code
 * coverage has been started. This event can be trigged by the embedder to, for example,
 * trigger collection of coverage data immediatelly at a certain point in time.
 */
@Experimental
public data class PreciseCoverageDeltaUpdate(
  @JsonProperty("timestamp")
  public val timestamp: Double,
  @JsonProperty("occassion")
  public val occassion: String,
  @JsonProperty("result")
  public val result: List<ScriptCoverage>,
)
