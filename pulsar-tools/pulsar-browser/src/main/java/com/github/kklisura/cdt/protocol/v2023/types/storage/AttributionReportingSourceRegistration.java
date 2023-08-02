package com.github.kklisura.cdt.protocol.v2023.types.storage;

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

import java.util.List;

@Experimental
public class AttributionReportingSourceRegistration {

  private Double time;

  @Optional
  private Integer expiry;

  @Optional private Integer eventReportWindow;

  @Optional private Integer aggregatableReportWindow;

  private AttributionReportingSourceType type;

  private String sourceOrigin;

  private String reportingOrigin;

  private List<String> destinationSites;

  private String eventId;

  private String priority;

  private List<AttributionReportingFilterDataEntry> filterData;

  private List<AttributionReportingAggregationKeysEntry> aggregationKeys;

  @Optional private String debugKey;

  public Double getTime() {
    return time;
  }

  public void setTime(Double time) {
    this.time = time;
  }

  /** duration in seconds */
  public Integer getExpiry() {
    return expiry;
  }

  /** duration in seconds */
  public void setExpiry(Integer expiry) {
    this.expiry = expiry;
  }

  /** duration in seconds */
  public Integer getEventReportWindow() {
    return eventReportWindow;
  }

  /** duration in seconds */
  public void setEventReportWindow(Integer eventReportWindow) {
    this.eventReportWindow = eventReportWindow;
  }

  /** duration in seconds */
  public Integer getAggregatableReportWindow() {
    return aggregatableReportWindow;
  }

  /** duration in seconds */
  public void setAggregatableReportWindow(Integer aggregatableReportWindow) {
    this.aggregatableReportWindow = aggregatableReportWindow;
  }

  public AttributionReportingSourceType getType() {
    return type;
  }

  public void setType(AttributionReportingSourceType type) {
    this.type = type;
  }

  public String getSourceOrigin() {
    return sourceOrigin;
  }

  public void setSourceOrigin(String sourceOrigin) {
    this.sourceOrigin = sourceOrigin;
  }

  public String getReportingOrigin() {
    return reportingOrigin;
  }

  public void setReportingOrigin(String reportingOrigin) {
    this.reportingOrigin = reportingOrigin;
  }

  public List<String> getDestinationSites() {
    return destinationSites;
  }

  public void setDestinationSites(List<String> destinationSites) {
    this.destinationSites = destinationSites;
  }

  public String getEventId() {
    return eventId;
  }

  public void setEventId(String eventId) {
    this.eventId = eventId;
  }

  public String getPriority() {
    return priority;
  }

  public void setPriority(String priority) {
    this.priority = priority;
  }

  public List<AttributionReportingFilterDataEntry> getFilterData() {
    return filterData;
  }

  public void setFilterData(List<AttributionReportingFilterDataEntry> filterData) {
    this.filterData = filterData;
  }

  public List<AttributionReportingAggregationKeysEntry> getAggregationKeys() {
    return aggregationKeys;
  }

  public void setAggregationKeys(List<AttributionReportingAggregationKeysEntry> aggregationKeys) {
    this.aggregationKeys = aggregationKeys;
  }

  public String getDebugKey() {
    return debugKey;
  }

  public void setDebugKey(String debugKey) {
    this.debugKey = debugKey;
  }
}
