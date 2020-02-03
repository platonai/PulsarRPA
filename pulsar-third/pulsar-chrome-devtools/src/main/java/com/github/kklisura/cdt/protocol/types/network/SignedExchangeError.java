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

/** Information about a signed exchange response. */
@Experimental
public class SignedExchangeError {

  private String message;

  @Optional private Integer signatureIndex;

  @Optional private SignedExchangeErrorField errorField;

  /** Error message. */
  public String getMessage() {
    return message;
  }

  /** Error message. */
  public void setMessage(String message) {
    this.message = message;
  }

  /** The index of the signature which caused the error. */
  public Integer getSignatureIndex() {
    return signatureIndex;
  }

  /** The index of the signature which caused the error. */
  public void setSignatureIndex(Integer signatureIndex) {
    this.signatureIndex = signatureIndex;
  }

  /** The field which caused the error. */
  public SignedExchangeErrorField getErrorField() {
    return errorField;
  }

  /** The field which caused the error. */
  public void setErrorField(SignedExchangeErrorField errorField) {
    this.errorField = errorField;
  }
}
