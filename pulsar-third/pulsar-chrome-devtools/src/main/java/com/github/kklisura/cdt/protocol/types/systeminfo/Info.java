package com.github.kklisura.cdt.protocol.types.systeminfo;

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

public class Info {

  private GPUInfo gpu;

  private String modelName;

  private String modelVersion;

  private String commandLine;

  /** Information about the GPUs on the system. */
  public GPUInfo getGpu() {
    return gpu;
  }

  /** Information about the GPUs on the system. */
  public void setGpu(GPUInfo gpu) {
    this.gpu = gpu;
  }

  /**
   * A platform-dependent description of the model of the machine. On Mac OS, this is, for example,
   * 'MacBookPro'. Will be the empty string if not supported.
   */
  public String getModelName() {
    return modelName;
  }

  /**
   * A platform-dependent description of the model of the machine. On Mac OS, this is, for example,
   * 'MacBookPro'. Will be the empty string if not supported.
   */
  public void setModelName(String modelName) {
    this.modelName = modelName;
  }

  /**
   * A platform-dependent description of the version of the machine. On Mac OS, this is, for
   * example, '10.1'. Will be the empty string if not supported.
   */
  public String getModelVersion() {
    return modelVersion;
  }

  /**
   * A platform-dependent description of the version of the machine. On Mac OS, this is, for
   * example, '10.1'. Will be the empty string if not supported.
   */
  public void setModelVersion(String modelVersion) {
    this.modelVersion = modelVersion;
  }

  /**
   * The command line string used to launch the browser. Will be the empty string if not supported.
   */
  public String getCommandLine() {
    return commandLine;
  }

  /**
   * The command line string used to launch the browser. Will be the empty string if not supported.
   */
  public void setCommandLine(String commandLine) {
    this.commandLine = commandLine;
  }
}
