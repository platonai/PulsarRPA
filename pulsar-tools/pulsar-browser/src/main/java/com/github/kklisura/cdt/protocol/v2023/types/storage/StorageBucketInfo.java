package com.github.kklisura.cdt.protocol.v2023.types.storage;

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

public class StorageBucketInfo {

  private StorageBucket bucket;

  private String id;

  private Double expiration;

  private Double quota;

  private Boolean persistent;

  private StorageBucketsDurability durability;

  public StorageBucket getBucket() {
    return bucket;
  }

  public void setBucket(StorageBucket bucket) {
    this.bucket = bucket;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Double getExpiration() {
    return expiration;
  }

  public void setExpiration(Double expiration) {
    this.expiration = expiration;
  }

  /** Storage quota (bytes). */
  public Double getQuota() {
    return quota;
  }

  /** Storage quota (bytes). */
  public void setQuota(Double quota) {
    this.quota = quota;
  }

  public Boolean getPersistent() {
    return persistent;
  }

  public void setPersistent(Boolean persistent) {
    this.persistent = persistent;
  }

  public StorageBucketsDurability getDurability() {
    return durability;
  }

  public void setDurability(StorageBucketsDurability durability) {
    this.durability = durability;
  }
}
