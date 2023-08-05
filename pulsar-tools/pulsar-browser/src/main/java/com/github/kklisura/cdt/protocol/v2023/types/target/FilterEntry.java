package com.github.kklisura.cdt.protocol.v2023.types.target;

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

import com.github.kklisura.cdt.protocol.v2023.support.annotations.Experimental;
import com.github.kklisura.cdt.protocol.v2023.support.annotations.Optional;

/** A filter used by target query/discovery/auto-attach operations. */
@Experimental
public class FilterEntry {

  @Optional
  private Boolean exclude;

  @Optional private String type;

  /** If set, causes exclusion of mathcing targets from the list. */
  public Boolean getExclude() {
    return exclude;
  }

  /** If set, causes exclusion of mathcing targets from the list. */
  public void setExclude(Boolean exclude) {
    this.exclude = exclude;
  }

  /** If not present, matches any type. */
  public String getType() {
    return type;
  }

  /** If not present, matches any type. */
  public void setType(String type) {
    this.type = type;
  }
}
