package com.github.kklisura.cdt.protocol.v2023.types.page;

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

/** Navigation history entry. */
public class NavigationEntry {

  private Integer id;

  private String url;

  private String userTypedURL;

  private String title;

  private TransitionType transitionType;

  /** Unique id of the navigation history entry. */
  public Integer getId() {
    return id;
  }

  /** Unique id of the navigation history entry. */
  public void setId(Integer id) {
    this.id = id;
  }

  /** URL of the navigation history entry. */
  public String getUrl() {
    return url;
  }

  /** URL of the navigation history entry. */
  public void setUrl(String url) {
    this.url = url;
  }

  /** URL that the user typed in the url bar. */
  public String getUserTypedURL() {
    return userTypedURL;
  }

  /** URL that the user typed in the url bar. */
  public void setUserTypedURL(String userTypedURL) {
    this.userTypedURL = userTypedURL;
  }

  /** Title of the navigation history entry. */
  public String getTitle() {
    return title;
  }

  /** Title of the navigation history entry. */
  public void setTitle(String title) {
    this.title = title;
  }

  /** Transition type. */
  public TransitionType getTransitionType() {
    return transitionType;
  }

  /** Transition type. */
  public void setTransitionType(TransitionType transitionType) {
    this.transitionType = transitionType;
  }
}
