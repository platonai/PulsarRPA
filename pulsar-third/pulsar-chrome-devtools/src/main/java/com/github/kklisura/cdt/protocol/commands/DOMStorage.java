package com.github.kklisura.cdt.protocol.commands;

/*-
 * #%L
 * cdt-java-client
 * %%
 * Copyright (C) 2018 - 2019 Kenan Klisura
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.github.kklisura.cdt.protocol.events.domstorage.DomStorageItemAdded;
import com.github.kklisura.cdt.protocol.events.domstorage.DomStorageItemRemoved;
import com.github.kklisura.cdt.protocol.events.domstorage.DomStorageItemUpdated;
import com.github.kklisura.cdt.protocol.events.domstorage.DomStorageItemsCleared;
import com.github.kklisura.cdt.protocol.support.annotations.EventName;
import com.github.kklisura.cdt.protocol.support.annotations.Experimental;
import com.github.kklisura.cdt.protocol.support.annotations.ParamName;
import com.github.kklisura.cdt.protocol.support.annotations.ReturnTypeParameter;
import com.github.kklisura.cdt.protocol.support.annotations.Returns;
import com.github.kklisura.cdt.protocol.support.types.EventHandler;
import com.github.kklisura.cdt.protocol.support.types.EventListener;
import com.github.kklisura.cdt.protocol.types.domstorage.StorageId;
import java.util.List;

/** Query and modify DOM storage. */
@Experimental
public interface DOMStorage {

  /** @param storageId */
  void clear(@ParamName("storageId") StorageId storageId);

  /** Disables storage tracking, prevents storage events from being sent to the client. */
  void disable();

  /** Enables storage tracking, storage events will now be delivered to the client. */
  void enable();

  /** @param storageId */
  @Returns("entries")
  @ReturnTypeParameter({List.class, String.class})
  List<List<String>> getDOMStorageItems(@ParamName("storageId") StorageId storageId);

  /**
   * @param storageId
   * @param key
   */
  void removeDOMStorageItem(
      @ParamName("storageId") StorageId storageId, @ParamName("key") String key);

  /**
   * @param storageId
   * @param key
   * @param value
   */
  void setDOMStorageItem(
      @ParamName("storageId") StorageId storageId,
      @ParamName("key") String key,
      @ParamName("value") String value);

  @EventName("domStorageItemAdded")
  EventListener onDomStorageItemAdded(EventHandler<DomStorageItemAdded> eventListener);

  @EventName("domStorageItemRemoved")
  EventListener onDomStorageItemRemoved(EventHandler<DomStorageItemRemoved> eventListener);

  @EventName("domStorageItemUpdated")
  EventListener onDomStorageItemUpdated(EventHandler<DomStorageItemUpdated> eventListener);

  @EventName("domStorageItemsCleared")
  EventListener onDomStorageItemsCleared(EventHandler<DomStorageItemsCleared> eventListener);
}
