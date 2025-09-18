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

/** Font families collection for a script. */
@Experimental
public class ScriptFontFamilies {

  private String script;

  private FontFamilies fontFamilies;

  /** Name of the script which these font families are defined for. */
  public String getScript() {
    return script;
  }

  /** Name of the script which these font families are defined for. */
  public void setScript(String script) {
    this.script = script;
  }

  /** Generic font families collection for the script. */
  public FontFamilies getFontFamilies() {
    return fontFamilies;
  }

  /** Generic font families collection for the script. */
  public void setFontFamilies(FontFamilies fontFamilies) {
    this.fontFamilies = fontFamilies;
  }
}
