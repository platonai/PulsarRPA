package com.github.kklisura.cdt.protocol.v2023.types.systeminfo;

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

import com.github.kklisura.cdt.protocol.v2023.support.annotations.Optional;

/** Describes a single graphics processor (GPU). */
public class GPUDevice {

  private Double vendorId;

  private Double deviceId;

  @Optional
  private Double subSysId;

  @Optional private Double revision;

  private String vendorString;

  private String deviceString;

  private String driverVendor;

  private String driverVersion;

  /** PCI ID of the GPU vendor, if available; 0 otherwise. */
  public Double getVendorId() {
    return vendorId;
  }

  /** PCI ID of the GPU vendor, if available; 0 otherwise. */
  public void setVendorId(Double vendorId) {
    this.vendorId = vendorId;
  }

  /** PCI ID of the GPU device, if available; 0 otherwise. */
  public Double getDeviceId() {
    return deviceId;
  }

  /** PCI ID of the GPU device, if available; 0 otherwise. */
  public void setDeviceId(Double deviceId) {
    this.deviceId = deviceId;
  }

  /** Sub sys ID of the GPU, only available on Windows. */
  public Double getSubSysId() {
    return subSysId;
  }

  /** Sub sys ID of the GPU, only available on Windows. */
  public void setSubSysId(Double subSysId) {
    this.subSysId = subSysId;
  }

  /** Revision of the GPU, only available on Windows. */
  public Double getRevision() {
    return revision;
  }

  /** Revision of the GPU, only available on Windows. */
  public void setRevision(Double revision) {
    this.revision = revision;
  }

  /** String description of the GPU vendor, if the PCI ID is not available. */
  public String getVendorString() {
    return vendorString;
  }

  /** String description of the GPU vendor, if the PCI ID is not available. */
  public void setVendorString(String vendorString) {
    this.vendorString = vendorString;
  }

  /** String description of the GPU device, if the PCI ID is not available. */
  public String getDeviceString() {
    return deviceString;
  }

  /** String description of the GPU device, if the PCI ID is not available. */
  public void setDeviceString(String deviceString) {
    this.deviceString = deviceString;
  }

  /** String description of the GPU driver vendor. */
  public String getDriverVendor() {
    return driverVendor;
  }

  /** String description of the GPU driver vendor. */
  public void setDriverVendor(String driverVendor) {
    this.driverVendor = driverVendor;
  }

  /** String description of the GPU driver version. */
  public String getDriverVersion() {
    return driverVersion;
  }

  /** String description of the GPU driver version. */
  public void setDriverVersion(String driverVersion) {
    this.driverVersion = driverVersion;
  }
}
