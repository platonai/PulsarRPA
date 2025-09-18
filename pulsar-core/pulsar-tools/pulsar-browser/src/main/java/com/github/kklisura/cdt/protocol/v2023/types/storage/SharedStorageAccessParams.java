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

import com.github.kklisura.cdt.protocol.v2023.support.annotations.Optional;

import java.util.List;

/**
 * Bundles the parameters for shared storage access events whose presence/absence can vary according
 * to SharedStorageAccessType.
 */
public class SharedStorageAccessParams {

  @Optional
  private String scriptSourceUrl;

  @Optional private String operationName;

  @Optional private String serializedData;

  @Optional private List<SharedStorageUrlWithMetadata> urlsWithMetadata;

  @Optional private String key;

  @Optional private String value;

  @Optional private Boolean ignoreIfPresent;

  /** Spec of the module script URL. Present only for SharedStorageAccessType.documentAddModule. */
  public String getScriptSourceUrl() {
    return scriptSourceUrl;
  }

  /** Spec of the module script URL. Present only for SharedStorageAccessType.documentAddModule. */
  public void setScriptSourceUrl(String scriptSourceUrl) {
    this.scriptSourceUrl = scriptSourceUrl;
  }

  /**
   * Name of the registered operation to be run. Present only for
   * SharedStorageAccessType.documentRun and SharedStorageAccessType.documentSelectURL.
   */
  public String getOperationName() {
    return operationName;
  }

  /**
   * Name of the registered operation to be run. Present only for
   * SharedStorageAccessType.documentRun and SharedStorageAccessType.documentSelectURL.
   */
  public void setOperationName(String operationName) {
    this.operationName = operationName;
  }

  /**
   * The operation's serialized data in bytes (converted to a string). Present only for
   * SharedStorageAccessType.documentRun and SharedStorageAccessType.documentSelectURL.
   */
  public String getSerializedData() {
    return serializedData;
  }

  /**
   * The operation's serialized data in bytes (converted to a string). Present only for
   * SharedStorageAccessType.documentRun and SharedStorageAccessType.documentSelectURL.
   */
  public void setSerializedData(String serializedData) {
    this.serializedData = serializedData;
  }

  /**
   * Array of candidate URLs' specs, along with any associated metadata. Present only for
   * SharedStorageAccessType.documentSelectURL.
   */
  public List<SharedStorageUrlWithMetadata> getUrlsWithMetadata() {
    return urlsWithMetadata;
  }

  /**
   * Array of candidate URLs' specs, along with any associated metadata. Present only for
   * SharedStorageAccessType.documentSelectURL.
   */
  public void setUrlsWithMetadata(List<SharedStorageUrlWithMetadata> urlsWithMetadata) {
    this.urlsWithMetadata = urlsWithMetadata;
  }

  /**
   * Key for a specific entry in an origin's shared storage. Present only for
   * SharedStorageAccessType.documentSet, SharedStorageAccessType.documentAppend,
   * SharedStorageAccessType.documentDelete, SharedStorageAccessType.workletSet,
   * SharedStorageAccessType.workletAppend, SharedStorageAccessType.workletDelete, and
   * SharedStorageAccessType.workletGet.
   */
  public String getKey() {
    return key;
  }

  /**
   * Key for a specific entry in an origin's shared storage. Present only for
   * SharedStorageAccessType.documentSet, SharedStorageAccessType.documentAppend,
   * SharedStorageAccessType.documentDelete, SharedStorageAccessType.workletSet,
   * SharedStorageAccessType.workletAppend, SharedStorageAccessType.workletDelete, and
   * SharedStorageAccessType.workletGet.
   */
  public void setKey(String key) {
    this.key = key;
  }

  /**
   * Value for a specific entry in an origin's shared storage. Present only for
   * SharedStorageAccessType.documentSet, SharedStorageAccessType.documentAppend,
   * SharedStorageAccessType.workletSet, and SharedStorageAccessType.workletAppend.
   */
  public String getValue() {
    return value;
  }

  /**
   * Value for a specific entry in an origin's shared storage. Present only for
   * SharedStorageAccessType.documentSet, SharedStorageAccessType.documentAppend,
   * SharedStorageAccessType.workletSet, and SharedStorageAccessType.workletAppend.
   */
  public void setValue(String value) {
    this.value = value;
  }

  /**
   * Whether or not to set an entry for a key if that key is already present. Present only for
   * SharedStorageAccessType.documentSet and SharedStorageAccessType.workletSet.
   */
  public Boolean getIgnoreIfPresent() {
    return ignoreIfPresent;
  }

  /**
   * Whether or not to set an entry for a key if that key is already present. Present only for
   * SharedStorageAccessType.documentSet and SharedStorageAccessType.workletSet.
   */
  public void setIgnoreIfPresent(Boolean ignoreIfPresent) {
    this.ignoreIfPresent = ignoreIfPresent;
  }
}
