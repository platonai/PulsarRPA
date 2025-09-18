package com.github.kklisura.cdt.protocol.v2023.events.page;

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

import java.util.List;

/**
 * Fired when a new window is going to be opened, via window.open(), link click, form submission,
 * etc.
 */
public class WindowOpen {

  private String url;

  private String windowName;

  private List<String> windowFeatures;

  private Boolean userGesture;

  /** The URL for the new window. */
  public String getUrl() {
    return url;
  }

  /** The URL for the new window. */
  public void setUrl(String url) {
    this.url = url;
  }

  /** Window name. */
  public String getWindowName() {
    return windowName;
  }

  /** Window name. */
  public void setWindowName(String windowName) {
    this.windowName = windowName;
  }

  /** An array of enabled window features. */
  public List<String> getWindowFeatures() {
    return windowFeatures;
  }

  /** An array of enabled window features. */
  public void setWindowFeatures(List<String> windowFeatures) {
    this.windowFeatures = windowFeatures;
  }

  /** Whether or not it was triggered by user gesture. */
  public Boolean getUserGesture() {
    return userGesture;
  }

  /** Whether or not it was triggered by user gesture. */
  public void setUserGesture(Boolean userGesture) {
    this.userGesture = userGesture;
  }
}
