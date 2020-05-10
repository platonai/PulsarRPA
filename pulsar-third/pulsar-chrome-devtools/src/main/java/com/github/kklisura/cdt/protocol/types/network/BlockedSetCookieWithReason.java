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

/** A cookie which was not stored from a response with the corresponding reason. */
@Experimental
public class BlockedSetCookieWithReason {

  private SetCookieBlockedReason blockedReason;

  private String cookieLine;

  @Optional private Cookie cookie;

  /** The reason this cookie was blocked. */
  public SetCookieBlockedReason getBlockedReason() {
    return blockedReason;
  }

  /** The reason this cookie was blocked. */
  public void setBlockedReason(SetCookieBlockedReason blockedReason) {
    this.blockedReason = blockedReason;
  }

  /**
   * The string representing this individual cookie as it would appear in the header. This is not
   * the entire "cookie" or "set-cookie" header which could have multiple cookies.
   */
  public String getCookieLine() {
    return cookieLine;
  }

  /**
   * The string representing this individual cookie as it would appear in the header. This is not
   * the entire "cookie" or "set-cookie" header which could have multiple cookies.
   */
  public void setCookieLine(String cookieLine) {
    this.cookieLine = cookieLine;
  }

  /**
   * The cookie object which represents the cookie which was not stored. It is optional because
   * sometimes complete cookie information is not available, such as in the case of parsing errors.
   */
  public Cookie getCookie() {
    return cookie;
  }

  /**
   * The cookie object which represents the cookie which was not stored. It is optional because
   * sometimes complete cookie information is not available, such as in the case of parsing errors.
   */
  public void setCookie(Cookie cookie) {
    this.cookie = cookie;
  }
}
