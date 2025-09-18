package com.github.kklisura.cdt.protocol.v2023.types.autofill;

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

public class CreditCard {

  private String number;

  private String name;

  private String expiryMonth;

  private String expiryYear;

  private String cvc;

  /** 16-digit credit card number. */
  public String getNumber() {
    return number;
  }

  /** 16-digit credit card number. */
  public void setNumber(String number) {
    this.number = number;
  }

  /** Name of the credit card owner. */
  public String getName() {
    return name;
  }

  /** Name of the credit card owner. */
  public void setName(String name) {
    this.name = name;
  }

  /** 2-digit expiry month. */
  public String getExpiryMonth() {
    return expiryMonth;
  }

  /** 2-digit expiry month. */
  public void setExpiryMonth(String expiryMonth) {
    this.expiryMonth = expiryMonth;
  }

  /** 4-digit expiry year. */
  public String getExpiryYear() {
    return expiryYear;
  }

  /** 4-digit expiry year. */
  public void setExpiryYear(String expiryYear) {
    this.expiryYear = expiryYear;
  }

  /** 3-digit card verification code. */
  public String getCvc() {
    return cvc;
  }

  /** 3-digit card verification code. */
  public void setCvc(String cvc) {
    this.cvc = cvc;
  }
}
