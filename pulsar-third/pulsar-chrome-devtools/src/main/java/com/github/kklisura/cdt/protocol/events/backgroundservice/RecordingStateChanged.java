package com.github.kklisura.cdt.protocol.events.backgroundservice;

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

import com.github.kklisura.cdt.protocol.types.backgroundservice.ServiceName;

/** Called when the recording state for the service has been updated. */
public class RecordingStateChanged {

  private Boolean isRecording;

  private ServiceName service;

  public Boolean getIsRecording() {
    return isRecording;
  }

  public void setIsRecording(Boolean isRecording) {
    this.isRecording = isRecording;
  }

  public ServiceName getService() {
    return service;
  }

  public void setService(ServiceName service) {
    this.service = service;
  }
}
