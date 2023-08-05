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

/** Timing information for the request. */
public class ResourceTiming {

  private Double requestTime;

  private Double proxyStart;

  private Double proxyEnd;

  private Double dnsStart;

  private Double dnsEnd;

  private Double connectStart;

  private Double connectEnd;

  private Double sslStart;

  private Double sslEnd;

  @Experimental
  private Double workerStart;

  @Experimental private Double workerReady;

  @Experimental private Double workerFetchStart;

  @Experimental private Double workerRespondWithSettled;

  private Double sendStart;

  private Double sendEnd;

  @Experimental private Double pushStart;

  @Experimental private Double pushEnd;

  @Experimental private Double receiveHeadersStart;

  private Double receiveHeadersEnd;

  /**
   * Timing's requestTime is a baseline in seconds, while the other numbers are ticks in
   * milliseconds relatively to this requestTime.
   */
  public Double getRequestTime() {
    return requestTime;
  }

  /**
   * Timing's requestTime is a baseline in seconds, while the other numbers are ticks in
   * milliseconds relatively to this requestTime.
   */
  public void setRequestTime(Double requestTime) {
    this.requestTime = requestTime;
  }

  /** Started resolving proxy. */
  public Double getProxyStart() {
    return proxyStart;
  }

  /** Started resolving proxy. */
  public void setProxyStart(Double proxyStart) {
    this.proxyStart = proxyStart;
  }

  /** Finished resolving proxy. */
  public Double getProxyEnd() {
    return proxyEnd;
  }

  /** Finished resolving proxy. */
  public void setProxyEnd(Double proxyEnd) {
    this.proxyEnd = proxyEnd;
  }

  /** Started DNS address resolve. */
  public Double getDnsStart() {
    return dnsStart;
  }

  /** Started DNS address resolve. */
  public void setDnsStart(Double dnsStart) {
    this.dnsStart = dnsStart;
  }

  /** Finished DNS address resolve. */
  public Double getDnsEnd() {
    return dnsEnd;
  }

  /** Finished DNS address resolve. */
  public void setDnsEnd(Double dnsEnd) {
    this.dnsEnd = dnsEnd;
  }

  /** Started connecting to the remote host. */
  public Double getConnectStart() {
    return connectStart;
  }

  /** Started connecting to the remote host. */
  public void setConnectStart(Double connectStart) {
    this.connectStart = connectStart;
  }

  /** Connected to the remote host. */
  public Double getConnectEnd() {
    return connectEnd;
  }

  /** Connected to the remote host. */
  public void setConnectEnd(Double connectEnd) {
    this.connectEnd = connectEnd;
  }

  /** Started SSL handshake. */
  public Double getSslStart() {
    return sslStart;
  }

  /** Started SSL handshake. */
  public void setSslStart(Double sslStart) {
    this.sslStart = sslStart;
  }

  /** Finished SSL handshake. */
  public Double getSslEnd() {
    return sslEnd;
  }

  /** Finished SSL handshake. */
  public void setSslEnd(Double sslEnd) {
    this.sslEnd = sslEnd;
  }

  /** Started running ServiceWorker. */
  public Double getWorkerStart() {
    return workerStart;
  }

  /** Started running ServiceWorker. */
  public void setWorkerStart(Double workerStart) {
    this.workerStart = workerStart;
  }

  /** Finished Starting ServiceWorker. */
  public Double getWorkerReady() {
    return workerReady;
  }

  /** Finished Starting ServiceWorker. */
  public void setWorkerReady(Double workerReady) {
    this.workerReady = workerReady;
  }

  /** Started fetch event. */
  public Double getWorkerFetchStart() {
    return workerFetchStart;
  }

  /** Started fetch event. */
  public void setWorkerFetchStart(Double workerFetchStart) {
    this.workerFetchStart = workerFetchStart;
  }

  /** Settled fetch event respondWith promise. */
  public Double getWorkerRespondWithSettled() {
    return workerRespondWithSettled;
  }

  /** Settled fetch event respondWith promise. */
  public void setWorkerRespondWithSettled(Double workerRespondWithSettled) {
    this.workerRespondWithSettled = workerRespondWithSettled;
  }

  /** Started sending request. */
  public Double getSendStart() {
    return sendStart;
  }

  /** Started sending request. */
  public void setSendStart(Double sendStart) {
    this.sendStart = sendStart;
  }

  /** Finished sending request. */
  public Double getSendEnd() {
    return sendEnd;
  }

  /** Finished sending request. */
  public void setSendEnd(Double sendEnd) {
    this.sendEnd = sendEnd;
  }

  /** Time the server started pushing request. */
  public Double getPushStart() {
    return pushStart;
  }

  /** Time the server started pushing request. */
  public void setPushStart(Double pushStart) {
    this.pushStart = pushStart;
  }

  /** Time the server finished pushing request. */
  public Double getPushEnd() {
    return pushEnd;
  }

  /** Time the server finished pushing request. */
  public void setPushEnd(Double pushEnd) {
    this.pushEnd = pushEnd;
  }

  /** Started receiving response headers. */
  public Double getReceiveHeadersStart() {
    return receiveHeadersStart;
  }

  /** Started receiving response headers. */
  public void setReceiveHeadersStart(Double receiveHeadersStart) {
    this.receiveHeadersStart = receiveHeadersStart;
  }

  /** Finished receiving response headers. */
  public Double getReceiveHeadersEnd() {
    return receiveHeadersEnd;
  }

  /** Finished receiving response headers. */
  public void setReceiveHeadersEnd(Double receiveHeadersEnd) {
    this.receiveHeadersEnd = receiveHeadersEnd;
  }
}
