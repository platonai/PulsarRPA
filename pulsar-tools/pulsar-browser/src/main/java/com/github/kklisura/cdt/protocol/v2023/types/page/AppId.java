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

import com.github.kklisura.cdt.protocol.v2023.support.annotations.Optional;

public class AppId {

  @Optional private String appId;

  @Optional private String recommendedId;

  /** App id, either from manifest's id attribute or computed from start_url */
  public String getAppId() {
    return appId;
  }

  /** App id, either from manifest's id attribute or computed from start_url */
  public void setAppId(String appId) {
    this.appId = appId;
  }

  /** Recommendation for manifest's id attribute to match current id computed from start_url */
  public String getRecommendedId() {
    return recommendedId;
  }

  /** Recommendation for manifest's id attribute to match current id computed from start_url */
  public void setRecommendedId(String recommendedId) {
    this.recommendedId = recommendedId;
  }
}
