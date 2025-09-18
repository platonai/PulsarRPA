package com.github.kklisura.cdt.protocol.v2023.types.css;

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

public class MatchedStylesForNode {

  @Optional
  private CSSStyle inlineStyle;

  @Optional private CSSStyle attributesStyle;

  @Optional private List<RuleMatch> matchedCSSRules;

  @Optional private List<PseudoElementMatches> pseudoElements;

  @Optional private List<InheritedStyleEntry> inherited;

  @Optional private List<InheritedPseudoElementMatches> inheritedPseudoElements;

  @Optional private List<CSSKeyframesRule> cssKeyframesRules;

  @Optional private List<CSSPositionFallbackRule> cssPositionFallbackRules;

  @Experimental
  @Optional private Integer parentLayoutNodeId;

  /** Inline style for the specified DOM node. */
  public CSSStyle getInlineStyle() {
    return inlineStyle;
  }

  /** Inline style for the specified DOM node. */
  public void setInlineStyle(CSSStyle inlineStyle) {
    this.inlineStyle = inlineStyle;
  }

  /** Attribute-defined element style (e.g. resulting from "width=20 height=100%"). */
  public CSSStyle getAttributesStyle() {
    return attributesStyle;
  }

  /** Attribute-defined element style (e.g. resulting from "width=20 height=100%"). */
  public void setAttributesStyle(CSSStyle attributesStyle) {
    this.attributesStyle = attributesStyle;
  }

  /** CSS rules matching this node, from all applicable stylesheets. */
  public List<RuleMatch> getMatchedCSSRules() {
    return matchedCSSRules;
  }

  /** CSS rules matching this node, from all applicable stylesheets. */
  public void setMatchedCSSRules(List<RuleMatch> matchedCSSRules) {
    this.matchedCSSRules = matchedCSSRules;
  }

  /** Pseudo style matches for this node. */
  public List<PseudoElementMatches> getPseudoElements() {
    return pseudoElements;
  }

  /** Pseudo style matches for this node. */
  public void setPseudoElements(List<PseudoElementMatches> pseudoElements) {
    this.pseudoElements = pseudoElements;
  }

  /** A chain of inherited styles (from the immediate node parent up to the DOM tree root). */
  public List<InheritedStyleEntry> getInherited() {
    return inherited;
  }

  /** A chain of inherited styles (from the immediate node parent up to the DOM tree root). */
  public void setInherited(List<InheritedStyleEntry> inherited) {
    this.inherited = inherited;
  }

  /**
   * A chain of inherited pseudo element styles (from the immediate node parent up to the DOM tree
   * root).
   */
  public List<InheritedPseudoElementMatches> getInheritedPseudoElements() {
    return inheritedPseudoElements;
  }

  /**
   * A chain of inherited pseudo element styles (from the immediate node parent up to the DOM tree
   * root).
   */
  public void setInheritedPseudoElements(
      List<InheritedPseudoElementMatches> inheritedPseudoElements) {
    this.inheritedPseudoElements = inheritedPseudoElements;
  }

  /** A list of CSS keyframed animations matching this node. */
  public List<CSSKeyframesRule> getCssKeyframesRules() {
    return cssKeyframesRules;
  }

  /** A list of CSS keyframed animations matching this node. */
  public void setCssKeyframesRules(List<CSSKeyframesRule> cssKeyframesRules) {
    this.cssKeyframesRules = cssKeyframesRules;
  }

  /** A list of CSS position fallbacks matching this node. */
  public List<CSSPositionFallbackRule> getCssPositionFallbackRules() {
    return cssPositionFallbackRules;
  }

  /** A list of CSS position fallbacks matching this node. */
  public void setCssPositionFallbackRules(List<CSSPositionFallbackRule> cssPositionFallbackRules) {
    this.cssPositionFallbackRules = cssPositionFallbackRules;
  }

  /** Id of the first parent element that does not have display: contents. */
  public Integer getParentLayoutNodeId() {
    return parentLayoutNodeId;
  }

  /** Id of the first parent element that does not have display: contents. */
  public void setParentLayoutNodeId(Integer parentLayoutNodeId) {
    this.parentLayoutNodeId = parentLayoutNodeId;
  }
}
