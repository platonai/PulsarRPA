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

/** A cookie with was not sent with a request with the corresponding reason. */
@Experimental
public class BlockedCookieWithReason {

  private CookieBlockedReason blockedReason;

  private Cookie cookie;

  /** The reason the cookie was blocked. */
  public CookieBlockedReason getBlockedReason() {
    return blockedReason;
  }

  /** The reason the cookie was blocked. */
  public void setBlockedReason(CookieBlockedReason blockedReason) {
    this.blockedReason = blockedReason;
  }

  /** The cookie object representing the cookie which was not sent. */
  public Cookie getCookie() {
    return cookie;
  }

  /** The cookie object representing the cookie which was not sent. */
  public void setCookie(Cookie cookie) {
    this.cookie = cookie;
  }
}
