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
import com.github.kklisura.cdt.protocol.v2023.support.annotations.Optional;

@Experimental
public class CrossOriginEmbedderPolicyStatus {

  private CrossOriginEmbedderPolicyValue value;

  private CrossOriginEmbedderPolicyValue reportOnlyValue;

  @Optional
  private String reportingEndpoint;

  @Optional private String reportOnlyReportingEndpoint;

  public CrossOriginEmbedderPolicyValue getValue() {
    return value;
  }

  public void setValue(CrossOriginEmbedderPolicyValue value) {
    this.value = value;
  }

  public CrossOriginEmbedderPolicyValue getReportOnlyValue() {
    return reportOnlyValue;
  }

  public void setReportOnlyValue(CrossOriginEmbedderPolicyValue reportOnlyValue) {
    this.reportOnlyValue = reportOnlyValue;
  }

  public String getReportingEndpoint() {
    return reportingEndpoint;
  }

  public void setReportingEndpoint(String reportingEndpoint) {
    this.reportingEndpoint = reportingEndpoint;
  }

  public String getReportOnlyReportingEndpoint() {
    return reportOnlyReportingEndpoint;
  }

  public void setReportOnlyReportingEndpoint(String reportOnlyReportingEndpoint) {
    this.reportOnlyReportingEndpoint = reportOnlyReportingEndpoint;
  }
}
