package com.github.kklisura.cdt.protocol.v2023.types.audits;

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

public enum GenericIssueErrorType {
  @JsonProperty("CrossOriginPortalPostMessageError")
  CROSS_ORIGIN_PORTAL_POST_MESSAGE_ERROR,
  @JsonProperty("FormLabelForNameError")
  FORM_LABEL_FOR_NAME_ERROR,
  @JsonProperty("FormDuplicateIdForInputError")
  FORM_DUPLICATE_ID_FOR_INPUT_ERROR,
  @JsonProperty("FormInputWithNoLabelError")
  FORM_INPUT_WITH_NO_LABEL_ERROR,
  @JsonProperty("FormAutocompleteAttributeEmptyError")
  FORM_AUTOCOMPLETE_ATTRIBUTE_EMPTY_ERROR,
  @JsonProperty("FormEmptyIdAndNameAttributesForInputError")
  FORM_EMPTY_ID_AND_NAME_ATTRIBUTES_FOR_INPUT_ERROR,
  @JsonProperty("FormAriaLabelledByToNonExistingId")
  FORM_ARIA_LABELLED_BY_TO_NON_EXISTING_ID,
  @JsonProperty("FormInputAssignedAutocompleteValueToIdOrNameAttributeError")
  FORM_INPUT_ASSIGNED_AUTOCOMPLETE_VALUE_TO_ID_OR_NAME_ATTRIBUTE_ERROR,
  @JsonProperty("FormLabelHasNeitherForNorNestedInput")
  FORM_LABEL_HAS_NEITHER_FOR_NOR_NESTED_INPUT,
  @JsonProperty("FormLabelForMatchesNonExistingIdError")
  FORM_LABEL_FOR_MATCHES_NON_EXISTING_ID_ERROR,
  @JsonProperty("FormInputHasWrongButWellIntendedAutocompleteValueError")
  FORM_INPUT_HAS_WRONG_BUT_WELL_INTENDED_AUTOCOMPLETE_VALUE_ERROR,
  @JsonProperty("ResponseWasBlockedByORB")
  RESPONSE_WAS_BLOCKED_BY_ORB
}
