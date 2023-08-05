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

import com.github.kklisura.cdt.protocol.v2023.support.annotations.Experimental;
import com.github.kklisura.cdt.protocol.v2023.support.annotations.Optional;

/** Default font sizes. */
@Experimental
public class FontSizes {

  @Optional
  private Integer standard;

  @Optional private Integer fixed;

  /** Default standard font size. */
  public Integer getStandard() {
    return standard;
  }

  /** Default standard font size. */
  public void setStandard(Integer standard) {
    this.standard = standard;
  }

  /** Default fixed font size. */
  public Integer getFixed() {
    return fixed;
  }

  /** Default fixed font size. */
  public void setFixed(Integer fixed) {
    this.fixed = fixed;
  }
}
