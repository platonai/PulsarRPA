package com.github.kklisura.cdt.protocol.v2023.events.storage;

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
import com.github.kklisura.cdt.protocol.v2023.types.storage.AttributionReportingSourceRegistration;
import com.github.kklisura.cdt.protocol.v2023.types.storage.AttributionReportingSourceRegistrationResult;

/** TODO(crbug.com/1458532): Add other Attribution Reporting events, e.g. trigger registration. */
@Experimental
public class AttributionReportingSourceRegistered {

  private AttributionReportingSourceRegistration registration;

  private AttributionReportingSourceRegistrationResult result;

  public AttributionReportingSourceRegistration getRegistration() {
    return registration;
  }

  public void setRegistration(AttributionReportingSourceRegistration registration) {
    this.registration = registration;
  }

  public AttributionReportingSourceRegistrationResult getResult() {
    return result;
  }

  public void setResult(AttributionReportingSourceRegistrationResult result) {
    this.result = result;
  }
}
