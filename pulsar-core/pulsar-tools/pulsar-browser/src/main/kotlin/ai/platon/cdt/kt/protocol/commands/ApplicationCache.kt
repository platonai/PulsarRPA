@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.commands

import ai.platon.cdt.kt.protocol.events.applicationcache.ApplicationCacheStatusUpdated
import ai.platon.cdt.kt.protocol.events.applicationcache.NetworkStateUpdated
import ai.platon.cdt.kt.protocol.support.annotations.EventName
import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.ParamName
import ai.platon.cdt.kt.protocol.support.annotations.ReturnTypeParameter
import ai.platon.cdt.kt.protocol.support.annotations.Returns
import ai.platon.cdt.kt.protocol.support.types.EventHandler
import ai.platon.cdt.kt.protocol.support.types.EventListener
import ai.platon.cdt.kt.protocol.types.applicationcache.FrameWithManifest
import kotlin.String
import kotlin.Unit
import kotlin.collections.List

@Experimental
interface ApplicationCache {
  /**
   * Enables application cache domain notifications.
   */
  suspend fun enable()

  /**
   * Returns relevant application cache data for the document in given frame.
   * @param frameId Identifier of the frame containing document whose application cache is retrieved.
   */
  @Returns("applicationCache")
  suspend fun getApplicationCacheForFrame(@ParamName("frameId") frameId: String): ai.platon.cdt.kt.protocol.types.applicationcache.ApplicationCache

  /**
   * Returns array of frame identifiers with manifest urls for each frame containing a document
   * associated with some application cache.
   */
  @Returns("frameIds")
  @ReturnTypeParameter(FrameWithManifest::class)
  suspend fun getFramesWithManifests(): List<FrameWithManifest>

  /**
   * Returns manifest URL for document in the given frame.
   * @param frameId Identifier of the frame containing document whose manifest is retrieved.
   */
  @Returns("manifestURL")
  suspend fun getManifestForFrame(@ParamName("frameId") frameId: String): String

  @EventName("applicationCacheStatusUpdated")
  fun onApplicationCacheStatusUpdated(eventListener: EventHandler<ApplicationCacheStatusUpdated>): EventListener

  @EventName("applicationCacheStatusUpdated")
  fun onApplicationCacheStatusUpdated(eventListener: suspend (ApplicationCacheStatusUpdated) -> Unit): EventListener

  @EventName("networkStateUpdated")
  fun onNetworkStateUpdated(eventListener: EventHandler<NetworkStateUpdated>): EventListener

  @EventName("networkStateUpdated")
  fun onNetworkStateUpdated(eventListener: suspend (NetworkStateUpdated) -> Unit): EventListener
}
