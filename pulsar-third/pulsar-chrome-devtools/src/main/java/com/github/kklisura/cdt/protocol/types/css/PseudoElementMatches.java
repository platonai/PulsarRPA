package com.github.kklisura.cdt.protocol.types.css;

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

import com.github.kklisura.cdt.protocol.types.dom.PseudoType;
import java.util.List;

/** CSS rule collection for a single pseudo style. */
public class PseudoElementMatches {

  private PseudoType pseudoType;

  private List<RuleMatch> matches;

  /** Pseudo element type. */
  public PseudoType getPseudoType() {
    return pseudoType;
  }

  /** Pseudo element type. */
  public void setPseudoType(PseudoType pseudoType) {
    this.pseudoType = pseudoType;
  }

  /** Matches of CSS rules applicable to the pseudo style. */
  public List<RuleMatch> getMatches() {
    return matches;
  }

  /** Matches of CSS rules applicable to the pseudo style. */
  public void setMatches(List<RuleMatch> matches) {
    this.matches = matches;
  }
}
