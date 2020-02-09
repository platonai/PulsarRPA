package com.github.kklisura.cdt.protocol.types.target;

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

import com.github.kklisura.cdt.protocol.support.annotations.Experimental;
import com.github.kklisura.cdt.protocol.support.annotations.Optional;

public class TargetInfo {

  private String targetId;

  private String type;

  private String title;

  private String url;

  private Boolean attached;

  @Optional private String openerId;

  @Experimental @Optional private String browserContextId;

  public String getTargetId() {
    return targetId;
  }

  public void setTargetId(String targetId) {
    this.targetId = targetId;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  /** Whether the target has an attached client. */
  public Boolean getAttached() {
    return attached;
  }

  /** Whether the target has an attached client. */
  public void setAttached(Boolean attached) {
    this.attached = attached;
  }

  /** Opener target Id */
  public String getOpenerId() {
    return openerId;
  }

  /** Opener target Id */
  public void setOpenerId(String openerId) {
    this.openerId = openerId;
  }

  public String getBrowserContextId() {
    return browserContextId;
  }

  public void setBrowserContextId(String browserContextId) {
    this.browserContextId = browserContextId;
  }
}
