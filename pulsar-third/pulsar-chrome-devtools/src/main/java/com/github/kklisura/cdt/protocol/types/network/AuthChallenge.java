package com.github.kklisura.cdt.protocol.types.network;

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

/** Authorization challenge for HTTP status code 401 or 407. */
@Experimental
public class AuthChallenge {

  @Optional private AuthChallengeSource source;

  private String origin;

  private String scheme;

  private String realm;

  /** Source of the authentication challenge. */
  public AuthChallengeSource getSource() {
    return source;
  }

  /** Source of the authentication challenge. */
  public void setSource(AuthChallengeSource source) {
    this.source = source;
  }

  /** Origin of the challenger. */
  public String getOrigin() {
    return origin;
  }

  /** Origin of the challenger. */
  public void setOrigin(String origin) {
    this.origin = origin;
  }

  /** The authentication scheme used, such as basic or digest */
  public String getScheme() {
    return scheme;
  }

  /** The authentication scheme used, such as basic or digest */
  public void setScheme(String scheme) {
    this.scheme = scheme;
  }

  /** The realm of the challenge. May be empty. */
  public String getRealm() {
    return realm;
  }

  /** The realm of the challenge. May be empty. */
  public void setRealm(String realm) {
    this.realm = realm;
  }
}
