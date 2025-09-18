package com.github.kklisura.cdt.protocol.v2023.types.network;

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

import java.util.List;

/** A cookie with was not sent with a request with the corresponding reason. */
@Experimental
public class BlockedCookieWithReason {

  private List<CookieBlockedReason> blockedReasons;

  private Cookie cookie;

  /** The reason(s) the cookie was blocked. */
  public List<CookieBlockedReason> getBlockedReasons() {
    return blockedReasons;
  }

  /** The reason(s) the cookie was blocked. */
  public void setBlockedReasons(List<CookieBlockedReason> blockedReasons) {
    this.blockedReasons = blockedReasons;
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
