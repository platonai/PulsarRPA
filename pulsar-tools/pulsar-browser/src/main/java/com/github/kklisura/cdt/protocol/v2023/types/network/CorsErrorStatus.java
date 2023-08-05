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

public class CorsErrorStatus {

  private CorsError corsError;

  private String failedParameter;

  public CorsError getCorsError() {
    return corsError;
  }

  public void setCorsError(CorsError corsError) {
    this.corsError = corsError;
  }

  public String getFailedParameter() {
    return failedParameter;
  }

  public void setFailedParameter(String failedParameter) {
    this.failedParameter = failedParameter;
  }
}
