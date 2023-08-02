package com.github.kklisura.cdt.protocol.v2023.types.preload;

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

import com.fasterxml.jackson.annotation.JsonProperty;

/** List of FinalStatus reasons for Prerender2. */
public enum PrerenderFinalStatus {
  @JsonProperty("Activated")
  ACTIVATED,
  @JsonProperty("Destroyed")
  DESTROYED,
  @JsonProperty("LowEndDevice")
  LOW_END_DEVICE,
  @JsonProperty("InvalidSchemeRedirect")
  INVALID_SCHEME_REDIRECT,
  @JsonProperty("InvalidSchemeNavigation")
  INVALID_SCHEME_NAVIGATION,
  @JsonProperty("InProgressNavigation")
  IN_PROGRESS_NAVIGATION,
  @JsonProperty("NavigationRequestBlockedByCsp")
  NAVIGATION_REQUEST_BLOCKED_BY_CSP,
  @JsonProperty("MainFrameNavigation")
  MAIN_FRAME_NAVIGATION,
  @JsonProperty("MojoBinderPolicy")
  MOJO_BINDER_POLICY,
  @JsonProperty("RendererProcessCrashed")
  RENDERER_PROCESS_CRASHED,
  @JsonProperty("RendererProcessKilled")
  RENDERER_PROCESS_KILLED,
  @JsonProperty("Download")
  DOWNLOAD,
  @JsonProperty("TriggerDestroyed")
  TRIGGER_DESTROYED,
  @JsonProperty("NavigationNotCommitted")
  NAVIGATION_NOT_COMMITTED,
  @JsonProperty("NavigationBadHttpStatus")
  NAVIGATION_BAD_HTTP_STATUS,
  @JsonProperty("ClientCertRequested")
  CLIENT_CERT_REQUESTED,
  @JsonProperty("NavigationRequestNetworkError")
  NAVIGATION_REQUEST_NETWORK_ERROR,
  @JsonProperty("MaxNumOfRunningPrerendersExceeded")
  MAX_NUM_OF_RUNNING_PRERENDERS_EXCEEDED,
  @JsonProperty("CancelAllHostsForTesting")
  CANCEL_ALL_HOSTS_FOR_TESTING,
  @JsonProperty("DidFailLoad")
  DID_FAIL_LOAD,
  @JsonProperty("Stop")
  STOP,
  @JsonProperty("SslCertificateError")
  SSL_CERTIFICATE_ERROR,
  @JsonProperty("LoginAuthRequested")
  LOGIN_AUTH_REQUESTED,
  @JsonProperty("UaChangeRequiresReload")
  UA_CHANGE_REQUIRES_RELOAD,
  @JsonProperty("BlockedByClient")
  BLOCKED_BY_CLIENT,
  @JsonProperty("AudioOutputDeviceRequested")
  AUDIO_OUTPUT_DEVICE_REQUESTED,
  @JsonProperty("MixedContent")
  MIXED_CONTENT,
  @JsonProperty("TriggerBackgrounded")
  TRIGGER_BACKGROUNDED,
  @JsonProperty("MemoryLimitExceeded")
  MEMORY_LIMIT_EXCEEDED,
  @JsonProperty("FailToGetMemoryUsage")
  FAIL_TO_GET_MEMORY_USAGE,
  @JsonProperty("DataSaverEnabled")
  DATA_SAVER_ENABLED,
  @JsonProperty("HasEffectiveUrl")
  HAS_EFFECTIVE_URL,
  @JsonProperty("ActivatedBeforeStarted")
  ACTIVATED_BEFORE_STARTED,
  @JsonProperty("InactivePageRestriction")
  INACTIVE_PAGE_RESTRICTION,
  @JsonProperty("StartFailed")
  START_FAILED,
  @JsonProperty("TimeoutBackgrounded")
  TIMEOUT_BACKGROUNDED,
  @JsonProperty("CrossSiteRedirectInInitialNavigation")
  CROSS_SITE_REDIRECT_IN_INITIAL_NAVIGATION,
  @JsonProperty("CrossSiteNavigationInInitialNavigation")
  CROSS_SITE_NAVIGATION_IN_INITIAL_NAVIGATION,
  @JsonProperty("SameSiteCrossOriginRedirectNotOptInInInitialNavigation")
  SAME_SITE_CROSS_ORIGIN_REDIRECT_NOT_OPT_IN_IN_INITIAL_NAVIGATION,
  @JsonProperty("SameSiteCrossOriginNavigationNotOptInInInitialNavigation")
  SAME_SITE_CROSS_ORIGIN_NAVIGATION_NOT_OPT_IN_IN_INITIAL_NAVIGATION,
  @JsonProperty("ActivationNavigationParameterMismatch")
  ACTIVATION_NAVIGATION_PARAMETER_MISMATCH,
  @JsonProperty("ActivatedInBackground")
  ACTIVATED_IN_BACKGROUND,
  @JsonProperty("EmbedderHostDisallowed")
  EMBEDDER_HOST_DISALLOWED,
  @JsonProperty("ActivationNavigationDestroyedBeforeSuccess")
  ACTIVATION_NAVIGATION_DESTROYED_BEFORE_SUCCESS,
  @JsonProperty("TabClosedByUserGesture")
  TAB_CLOSED_BY_USER_GESTURE,
  @JsonProperty("TabClosedWithoutUserGesture")
  TAB_CLOSED_WITHOUT_USER_GESTURE,
  @JsonProperty("PrimaryMainFrameRendererProcessCrashed")
  PRIMARY_MAIN_FRAME_RENDERER_PROCESS_CRASHED,
  @JsonProperty("PrimaryMainFrameRendererProcessKilled")
  PRIMARY_MAIN_FRAME_RENDERER_PROCESS_KILLED,
  @JsonProperty("ActivationFramePolicyNotCompatible")
  ACTIVATION_FRAME_POLICY_NOT_COMPATIBLE,
  @JsonProperty("PreloadingDisabled")
  PRELOADING_DISABLED,
  @JsonProperty("BatterySaverEnabled")
  BATTERY_SAVER_ENABLED,
  @JsonProperty("ActivatedDuringMainFrameNavigation")
  ACTIVATED_DURING_MAIN_FRAME_NAVIGATION,
  @JsonProperty("PreloadingUnsupportedByWebContents")
  PRELOADING_UNSUPPORTED_BY_WEB_CONTENTS,
  @JsonProperty("CrossSiteRedirectInMainFrameNavigation")
  CROSS_SITE_REDIRECT_IN_MAIN_FRAME_NAVIGATION,
  @JsonProperty("CrossSiteNavigationInMainFrameNavigation")
  CROSS_SITE_NAVIGATION_IN_MAIN_FRAME_NAVIGATION,
  @JsonProperty("SameSiteCrossOriginRedirectNotOptInInMainFrameNavigation")
  SAME_SITE_CROSS_ORIGIN_REDIRECT_NOT_OPT_IN_IN_MAIN_FRAME_NAVIGATION,
  @JsonProperty("SameSiteCrossOriginNavigationNotOptInInMainFrameNavigation")
  SAME_SITE_CROSS_ORIGIN_NAVIGATION_NOT_OPT_IN_IN_MAIN_FRAME_NAVIGATION,
  @JsonProperty("MemoryPressureOnTrigger")
  MEMORY_PRESSURE_ON_TRIGGER,
  @JsonProperty("MemoryPressureAfterTriggered")
  MEMORY_PRESSURE_AFTER_TRIGGERED,
  @JsonProperty("PrerenderingDisabledByDevTools")
  PRERENDERING_DISABLED_BY_DEV_TOOLS,
  @JsonProperty("ResourceLoadBlockedByClient")
  RESOURCE_LOAD_BLOCKED_BY_CLIENT,
  @JsonProperty("SpeculationRuleRemoved")
  SPECULATION_RULE_REMOVED,
  @JsonProperty("ActivatedWithAuxiliaryBrowsingContexts")
  ACTIVATED_WITH_AUXILIARY_BROWSING_CONTEXTS
}
