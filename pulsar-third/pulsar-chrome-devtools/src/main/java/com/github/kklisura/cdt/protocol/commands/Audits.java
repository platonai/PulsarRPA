package com.github.kklisura.cdt.protocol.commands;

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
import com.github.kklisura.cdt.protocol.support.annotations.ParamName;
import com.github.kklisura.cdt.protocol.types.audits.EncodedResponse;
import com.github.kklisura.cdt.protocol.types.audits.GetEncodedResponseEncoding;

/** Audits domain allows investigation of page violations and possible improvements. */
@Experimental
public interface Audits {

  /**
   * Returns the response body and size if it were re-encoded with the specified settings. Only
   * applies to images.
   *
   * @param requestId Identifier of the network request to get content for.
   * @param encoding The encoding to use.
   */
  EncodedResponse getEncodedResponse(
      @ParamName("requestId") String requestId,
      @ParamName("encoding") GetEncodedResponseEncoding encoding);

  /**
   * Returns the response body and size if it were re-encoded with the specified settings. Only
   * applies to images.
   *
   * @param requestId Identifier of the network request to get content for.
   * @param encoding The encoding to use.
   * @param quality The quality of the encoding (0-1). (defaults to 1)
   * @param sizeOnly Whether to only return the size information (defaults to false).
   */
  EncodedResponse getEncodedResponse(
      @ParamName("requestId") String requestId,
      @ParamName("encoding") GetEncodedResponseEncoding encoding,
      @Optional @ParamName("quality") Double quality,
      @Optional @ParamName("sizeOnly") Boolean sizeOnly);
}
