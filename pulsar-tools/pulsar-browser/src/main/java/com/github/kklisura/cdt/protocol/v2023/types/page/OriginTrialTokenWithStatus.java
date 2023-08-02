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

@Experimental
public class OriginTrialTokenWithStatus {

  private String rawTokenText;

  @Optional private OriginTrialToken parsedToken;

  private OriginTrialTokenStatus status;

  public String getRawTokenText() {
    return rawTokenText;
  }

  public void setRawTokenText(String rawTokenText) {
    this.rawTokenText = rawTokenText;
  }

  /** `parsedToken` is present only when the token is extractable and parsable. */
  public OriginTrialToken getParsedToken() {
    return parsedToken;
  }

  /** `parsedToken` is present only when the token is extractable and parsable. */
  public void setParsedToken(OriginTrialToken parsedToken) {
    this.parsedToken = parsedToken;
  }

  public OriginTrialTokenStatus getStatus() {
    return status;
  }

  public void setStatus(OriginTrialTokenStatus status) {
    this.status = status;
  }
}
