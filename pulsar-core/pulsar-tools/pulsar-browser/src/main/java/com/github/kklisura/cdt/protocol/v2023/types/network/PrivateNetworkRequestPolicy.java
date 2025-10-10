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

import com.fasterxml.jackson.annotation.JsonProperty;

public enum PrivateNetworkRequestPolicy {
  @JsonProperty("Allow")
  ALLOW,
  // 2025/10/10, vincent:
  // 20:46:46.918 [spatcher#9] WARN  a.p.p.b.d.c.impl.EventDispatcher - Failed converting response to clazz com.github.kklisura.cdt.protocol.v2023.events.network.RequestWillBeSentExtraInfo
  // {"requestId":"55584.2","associatedCookies":[],"headers":{"Accept":"image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8","Accept-Encoding":"gzip, deflate, br, zstd","Accept-Language":"zh-CN,zh;q=0.9","Connection":"keep-alive","Host":"127.0.0.1:8182","Referer":"http://127.0.0.1:8182/generated/interactive-dynamic.html","Sec-Fetch-Dest":"image","Sec-Fetch-Mode":"no-cors","Sec-Fetch-Site":"same-origin","User-Agent":"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36","sec-ch-ua":"\"Google Chrome\";v=\"141\", \"Not?A_Brand\";v=\"8\", \"Chromium\";v=\"141\"","sec-ch-ua-mobile":"?0","sec-ch-ua-platform":"\"Windows\""},"connectTiming":{"requestTime":170380.76074},"clientSecurityState":{"initiatorIsSecureContext":true,"initiatorIPAddressSpace":"Loopback","privateNetworkRequestPolicy":"PermissionBlock"},"siteHasCookieInOtherPartition":false}
  // com.fasterxml.jackson.databind.exc.InvalidFormatException: Cannot deserialize value of type `com.github.kklisura.cdt.protocol.v2023.types.network.PrivateNetworkRequestPolicy` from String "PermissionBlock": not one of the values accepted for Enum class: [BlockFromInsecureToMorePrivate, PreflightWarn, PreflightBlock, WarnFromInsecureToMorePrivate, Allow]
  @JsonProperty("PermissionBlock")
  PERMISSION_BLOCK,
  @JsonProperty("BlockFromInsecureToMorePrivate")
  BLOCK_FROM_INSECURE_TO_MORE_PRIVATE,
  @JsonProperty("WarnFromInsecureToMorePrivate")
  WARN_FROM_INSECURE_TO_MORE_PRIVATE,
  @JsonProperty("PreflightBlock")
  PREFLIGHT_BLOCK,
  @JsonProperty("PreflightWarn")
  PREFLIGHT_WARN
}
