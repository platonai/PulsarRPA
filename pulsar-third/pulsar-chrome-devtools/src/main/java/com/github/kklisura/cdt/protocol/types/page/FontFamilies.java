package com.github.kklisura.cdt.protocol.types.page;

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

/** Generic font families collection. */
@Experimental
public class FontFamilies {

  @Optional private String standard;

  @Optional private String fixed;

  @Optional private String serif;

  @Optional private String sansSerif;

  @Optional private String cursive;

  @Optional private String fantasy;

  @Optional private String pictograph;

  /** The standard font-family. */
  public String getStandard() {
    return standard;
  }

  /** The standard font-family. */
  public void setStandard(String standard) {
    this.standard = standard;
  }

  /** The fixed font-family. */
  public String getFixed() {
    return fixed;
  }

  /** The fixed font-family. */
  public void setFixed(String fixed) {
    this.fixed = fixed;
  }

  /** The serif font-family. */
  public String getSerif() {
    return serif;
  }

  /** The serif font-family. */
  public void setSerif(String serif) {
    this.serif = serif;
  }

  /** The sansSerif font-family. */
  public String getSansSerif() {
    return sansSerif;
  }

  /** The sansSerif font-family. */
  public void setSansSerif(String sansSerif) {
    this.sansSerif = sansSerif;
  }

  /** The cursive font-family. */
  public String getCursive() {
    return cursive;
  }

  /** The cursive font-family. */
  public void setCursive(String cursive) {
    this.cursive = cursive;
  }

  /** The fantasy font-family. */
  public String getFantasy() {
    return fantasy;
  }

  /** The fantasy font-family. */
  public void setFantasy(String fantasy) {
    this.fantasy = fantasy;
  }

  /** The pictograph font-family. */
  public String getPictograph() {
    return pictograph;
  }

  /** The pictograph font-family. */
  public void setPictograph(String pictograph) {
    this.pictograph = pictograph;
  }
}
