@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.commands

import ai.platon.cdt.kt.protocol.events.heapprofiler.AddHeapSnapshotChunk
import ai.platon.cdt.kt.protocol.events.heapprofiler.HeapStatsUpdate
import ai.platon.cdt.kt.protocol.events.heapprofiler.LastSeenObjectId
import ai.platon.cdt.kt.protocol.events.heapprofiler.ReportHeapSnapshotProgress
import ai.platon.cdt.kt.protocol.events.heapprofiler.ResetProfiles
import ai.platon.cdt.kt.protocol.support.annotations.EventName
import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.support.annotations.ParamName
import ai.platon.cdt.kt.protocol.support.annotations.Returns
import ai.platon.cdt.kt.protocol.support.types.EventHandler
import ai.platon.cdt.kt.protocol.support.types.EventListener
import ai.platon.cdt.kt.protocol.types.heapprofiler.SamplingHeapProfile
import ai.platon.cdt.kt.protocol.types.runtime.RemoteObject
import kotlin.Boolean
import kotlin.Double
import kotlin.String
import kotlin.Unit

@Experimental
interface HeapProfiler {
  /**
   * Enables console to refer to the node with given id via $x (see Command Line API for more details
   * $x functions).
   * @param heapObjectId Heap snapshot object id to be accessible by means of $x command line API.
   */
  suspend fun addInspectedHeapObject(@ParamName("heapObjectId") heapObjectId: String)

  suspend fun collectGarbage()

  suspend fun disable()

  suspend fun enable()

  /**
   * @param objectId Identifier of the object to get heap object id for.
   */
  @Returns("heapSnapshotObjectId")
  suspend fun getHeapObjectId(@ParamName("objectId") objectId: String): String

  /**
   * @param objectId
   * @param objectGroup Symbolic group name that can be used to release multiple objects.
   */
  @Returns("result")
  suspend fun getObjectByHeapObjectId(@ParamName("objectId") objectId: String, @ParamName("objectGroup") @Optional objectGroup: String? = null): RemoteObject

  @Returns("result")
  suspend fun getObjectByHeapObjectId(@ParamName("objectId") objectId: String): RemoteObject {
    return getObjectByHeapObjectId(objectId, null)
  }

  @Returns("profile")
  suspend fun getSamplingProfile(): SamplingHeapProfile

  /**
   * @param samplingInterval Average sample interval in bytes. Poisson distribution is used for the intervals. The
   * default value is 32768 bytes.
   */
  suspend fun startSampling(@ParamName("samplingInterval") @Optional samplingInterval: Double? = null)

  suspend fun startSampling() {
    return startSampling(null)
  }

  /**
   * @param trackAllocations
   */
  suspend fun startTrackingHeapObjects(@ParamName("trackAllocations") @Optional trackAllocations: Boolean? = null)

  suspend fun startTrackingHeapObjects() {
    return startTrackingHeapObjects(null)
  }

  @Returns("profile")
  suspend fun stopSampling(): SamplingHeapProfile

  /**
   * @param reportProgress If true 'reportHeapSnapshotProgress' events will be generated while snapshot is being taken
   * when the tracking is stopped.
   * @param treatGlobalObjectsAsRoots
   */
  suspend fun stopTrackingHeapObjects(@ParamName("reportProgress") @Optional reportProgress: Boolean? = null, @ParamName("treatGlobalObjectsAsRoots") @Optional treatGlobalObjectsAsRoots: Boolean? = null)

  suspend fun stopTrackingHeapObjects() {
    return stopTrackingHeapObjects(null, null)
  }

  /**
   * @param reportProgress If true 'reportHeapSnapshotProgress' events will be generated while snapshot is being taken.
   * @param treatGlobalObjectsAsRoots If true, a raw snapshot without artifical roots will be generated
   */
  suspend fun takeHeapSnapshot(@ParamName("reportProgress") @Optional reportProgress: Boolean? = null, @ParamName("treatGlobalObjectsAsRoots") @Optional treatGlobalObjectsAsRoots: Boolean? = null)

  suspend fun takeHeapSnapshot() {
    return takeHeapSnapshot(null, null)
  }

  @EventName("addHeapSnapshotChunk")
  fun onAddHeapSnapshotChunk(eventListener: EventHandler<AddHeapSnapshotChunk>): EventListener

  @EventName("addHeapSnapshotChunk")
  fun onAddHeapSnapshotChunk(eventListener: suspend (AddHeapSnapshotChunk) -> Unit): EventListener

  @EventName("heapStatsUpdate")
  fun onHeapStatsUpdate(eventListener: EventHandler<HeapStatsUpdate>): EventListener

  @EventName("heapStatsUpdate")
  fun onHeapStatsUpdate(eventListener: suspend (HeapStatsUpdate) -> Unit): EventListener

  @EventName("lastSeenObjectId")
  fun onLastSeenObjectId(eventListener: EventHandler<LastSeenObjectId>): EventListener

  @EventName("lastSeenObjectId")
  fun onLastSeenObjectId(eventListener: suspend (LastSeenObjectId) -> Unit): EventListener

  @EventName("reportHeapSnapshotProgress")
  fun onReportHeapSnapshotProgress(eventListener: EventHandler<ReportHeapSnapshotProgress>): EventListener

  @EventName("reportHeapSnapshotProgress")
  fun onReportHeapSnapshotProgress(eventListener: suspend (ReportHeapSnapshotProgress) -> Unit): EventListener

  @EventName("resetProfiles")
  fun onResetProfiles(eventListener: EventHandler<ResetProfiles>): EventListener

  @EventName("resetProfiles")
  fun onResetProfiles(eventListener: suspend (ResetProfiles) -> Unit): EventListener
}
