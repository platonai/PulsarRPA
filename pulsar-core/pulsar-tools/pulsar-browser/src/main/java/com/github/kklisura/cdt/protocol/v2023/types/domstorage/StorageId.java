package com.github.kklisura.cdt.protocol.v2023.types.domstorage;

/*-
 * #%L
 * cdt-java-client
 * %%
 * Copyright (C) 2018 - 2023 Kenan Klisura
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

import com.github.kklisura.cdt.protocol.v2023.support.annotations.Optional;

/** DOM Storage identifier. */
public class StorageId {

  @Optional
  private String securityOrigin;

  @Optional private String storageKey;

  private Boolean isLocalStorage;

  /** Security origin for the storage. */
  public String getSecurityOrigin() {
    return securityOrigin;
  }

  /** Security origin for the storage. */
  public void setSecurityOrigin(String securityOrigin) {
    this.securityOrigin = securityOrigin;
  }

  /** Represents a key by which DOM Storage keys its CachedStorageAreas */
  public String getStorageKey() {
    return storageKey;
  }

  /** Represents a key by which DOM Storage keys its CachedStorageAreas */
  public void setStorageKey(String storageKey) {
    this.storageKey = storageKey;
  }

  /** Whether the storage is local storage (not session storage). */
  public Boolean getIsLocalStorage() {
    return isLocalStorage;
  }

  /** Whether the storage is local storage (not session storage). */
  public void setIsLocalStorage(Boolean isLocalStorage) {
    this.isLocalStorage = isLocalStorage;
  }
}
