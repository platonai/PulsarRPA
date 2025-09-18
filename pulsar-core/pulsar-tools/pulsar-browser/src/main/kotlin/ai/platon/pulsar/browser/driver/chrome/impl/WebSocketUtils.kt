package ai.platon.pulsar.browser.driver.chrome.impl

import javax.websocket.CloseReason

/*-
 * #%L
 * cdt-java-client
 * %%
 * Copyright (C) 2018 - 2021 Kenan Klisura
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
 */ /**
 * Web socket related utils.
 *
 * @author Kenan Klisura
 */
object WebSocketUtils {
    private const val TYRUS_BUFFER_OVERFLOW = "Buffer overflow."

    /**
     * Is the reason for closing tyrus buffer overflow.
     *
     * @param closeReason Close reason.
     * @return True if this is unexpected close due to buffer overflow.
     */
    fun isTyrusBufferOverflowCloseReason(closeReason: CloseReason): Boolean {
        return CloseReason.CloseCodes.UNEXPECTED_CONDITION == closeReason.closeCode && TYRUS_BUFFER_OVERFLOW == closeReason.reasonPhrase
    }
}