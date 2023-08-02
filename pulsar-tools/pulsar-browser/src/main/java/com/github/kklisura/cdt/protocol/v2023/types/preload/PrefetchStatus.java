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

/**
 * TODO(https://crbug.com/1384419): revisit the list of PrefetchStatus and filter out the ones that
 * aren't necessary to the developers.
 */
public enum PrefetchStatus {
  @JsonProperty("PrefetchAllowed")
  PREFETCH_ALLOWED,
  @JsonProperty("PrefetchFailedIneligibleRedirect")
  PREFETCH_FAILED_INELIGIBLE_REDIRECT,
  @JsonProperty("PrefetchFailedInvalidRedirect")
  PREFETCH_FAILED_INVALID_REDIRECT,
  @JsonProperty("PrefetchFailedMIMENotSupported")
  PREFETCH_FAILED_MIME_NOT_SUPPORTED,
  @JsonProperty("PrefetchFailedNetError")
  PREFETCH_FAILED_NET_ERROR,
  @JsonProperty("PrefetchFailedNon2XX")
  PREFETCH_FAILED_NON_2XX,
  @JsonProperty("PrefetchFailedPerPageLimitExceeded")
  PREFETCH_FAILED_PER_PAGE_LIMIT_EXCEEDED,
  @JsonProperty("PrefetchEvicted")
  PREFETCH_EVICTED,
  @JsonProperty("PrefetchHeldback")
  PREFETCH_HELDBACK,
  @JsonProperty("PrefetchIneligibleRetryAfter")
  PREFETCH_INELIGIBLE_RETRY_AFTER,
  @JsonProperty("PrefetchIsPrivacyDecoy")
  PREFETCH_IS_PRIVACY_DECOY,
  @JsonProperty("PrefetchIsStale")
  PREFETCH_IS_STALE,
  @JsonProperty("PrefetchNotEligibleBrowserContextOffTheRecord")
  PREFETCH_NOT_ELIGIBLE_BROWSER_CONTEXT_OFF_THE_RECORD,
  @JsonProperty("PrefetchNotEligibleDataSaverEnabled")
  PREFETCH_NOT_ELIGIBLE_DATA_SAVER_ENABLED,
  @JsonProperty("PrefetchNotEligibleExistingProxy")
  PREFETCH_NOT_ELIGIBLE_EXISTING_PROXY,
  @JsonProperty("PrefetchNotEligibleHostIsNonUnique")
  PREFETCH_NOT_ELIGIBLE_HOST_IS_NON_UNIQUE,
  @JsonProperty("PrefetchNotEligibleNonDefaultStoragePartition")
  PREFETCH_NOT_ELIGIBLE_NON_DEFAULT_STORAGE_PARTITION,
  @JsonProperty("PrefetchNotEligibleSameSiteCrossOriginPrefetchRequiredProxy")
  PREFETCH_NOT_ELIGIBLE_SAME_SITE_CROSS_ORIGIN_PREFETCH_REQUIRED_PROXY,
  @JsonProperty("PrefetchNotEligibleSchemeIsNotHttps")
  PREFETCH_NOT_ELIGIBLE_SCHEME_IS_NOT_HTTPS,
  @JsonProperty("PrefetchNotEligibleUserHasCookies")
  PREFETCH_NOT_ELIGIBLE_USER_HAS_COOKIES,
  @JsonProperty("PrefetchNotEligibleUserHasServiceWorker")
  PREFETCH_NOT_ELIGIBLE_USER_HAS_SERVICE_WORKER,
  @JsonProperty("PrefetchNotEligibleBatterySaverEnabled")
  PREFETCH_NOT_ELIGIBLE_BATTERY_SAVER_ENABLED,
  @JsonProperty("PrefetchNotEligiblePreloadingDisabled")
  PREFETCH_NOT_ELIGIBLE_PRELOADING_DISABLED,
  @JsonProperty("PrefetchNotFinishedInTime")
  PREFETCH_NOT_FINISHED_IN_TIME,
  @JsonProperty("PrefetchNotStarted")
  PREFETCH_NOT_STARTED,
  @JsonProperty("PrefetchNotUsedCookiesChanged")
  PREFETCH_NOT_USED_COOKIES_CHANGED,
  @JsonProperty("PrefetchProxyNotAvailable")
  PREFETCH_PROXY_NOT_AVAILABLE,
  @JsonProperty("PrefetchResponseUsed")
  PREFETCH_RESPONSE_USED,
  @JsonProperty("PrefetchSuccessfulButNotUsed")
  PREFETCH_SUCCESSFUL_BUT_NOT_USED,
  @JsonProperty("PrefetchNotUsedProbeFailed")
  PREFETCH_NOT_USED_PROBE_FAILED
}
