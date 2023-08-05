package com.github.kklisura.cdt.protocol.v2023.events.deviceaccess;

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

import com.github.kklisura.cdt.protocol.v2023.types.deviceaccess.PromptDevice;

import java.util.List;

/**
 * A device request opened a user prompt to select a device. Respond with the selectPrompt or
 * cancelPrompt command.
 */
public class DeviceRequestPrompted {

  private String id;

  private List<PromptDevice> devices;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public List<PromptDevice> getDevices() {
    return devices;
  }

  public void setDevices(List<PromptDevice> devices) {
    this.devices = devices;
  }
}
