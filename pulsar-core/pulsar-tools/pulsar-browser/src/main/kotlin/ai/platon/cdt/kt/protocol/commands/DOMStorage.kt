@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.commands

import ai.platon.cdt.kt.protocol.events.domstorage.DomStorageItemAdded
import ai.platon.cdt.kt.protocol.events.domstorage.DomStorageItemRemoved
import ai.platon.cdt.kt.protocol.events.domstorage.DomStorageItemUpdated
import ai.platon.cdt.kt.protocol.events.domstorage.DomStorageItemsCleared
import ai.platon.cdt.kt.protocol.support.annotations.EventName
import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.ParamName
import ai.platon.cdt.kt.protocol.support.annotations.ReturnTypeParameter
import ai.platon.cdt.kt.protocol.support.annotations.Returns
import ai.platon.cdt.kt.protocol.support.types.EventHandler
import ai.platon.cdt.kt.protocol.support.types.EventListener
import ai.platon.cdt.kt.protocol.types.domstorage.StorageId
import kotlin.String
import kotlin.Unit
import kotlin.collections.List

/**
 * Query and modify DOM storage.
 */
@Experimental
interface DOMStorage {
  /**
   * @param storageId
   */
  suspend fun clear(@ParamName("storageId") storageId: StorageId)

  /**
   * Disables storage tracking, prevents storage events from being sent to the client.
   */
  suspend fun disable()

  /**
   * Enables storage tracking, storage events will now be delivered to the client.
   */
  suspend fun enable()

  /**
   * @param storageId
   */
  @Returns("entries")
  @ReturnTypeParameter(String::class)
  suspend fun getDOMStorageItems(@ParamName("storageId") storageId: StorageId): List<List<String>>

  /**
   * @param storageId
   * @param key
   */
  suspend fun removeDOMStorageItem(@ParamName("storageId") storageId: StorageId, @ParamName("key") key: String)

  /**
   * @param storageId
   * @param key
   * @param value
   */
  suspend fun setDOMStorageItem(
    @ParamName("storageId") storageId: StorageId,
    @ParamName("key") key: String,
    @ParamName("value") `value`: String,
  )

  @EventName("domStorageItemAdded")
  fun onDomStorageItemAdded(eventListener: EventHandler<DomStorageItemAdded>): EventListener

  @EventName("domStorageItemAdded")
  fun onDomStorageItemAdded(eventListener: suspend (DomStorageItemAdded) -> Unit): EventListener

  @EventName("domStorageItemRemoved")
  fun onDomStorageItemRemoved(eventListener: EventHandler<DomStorageItemRemoved>): EventListener

  @EventName("domStorageItemRemoved")
  fun onDomStorageItemRemoved(eventListener: suspend (DomStorageItemRemoved) -> Unit): EventListener

  @EventName("domStorageItemUpdated")
  fun onDomStorageItemUpdated(eventListener: EventHandler<DomStorageItemUpdated>): EventListener

  @EventName("domStorageItemUpdated")
  fun onDomStorageItemUpdated(eventListener: suspend (DomStorageItemUpdated) -> Unit): EventListener

  @EventName("domStorageItemsCleared")
  fun onDomStorageItemsCleared(eventListener: EventHandler<DomStorageItemsCleared>): EventListener

  @EventName("domStorageItemsCleared")
  fun onDomStorageItemsCleared(eventListener: suspend (DomStorageItemsCleared) -> Unit): EventListener
}
