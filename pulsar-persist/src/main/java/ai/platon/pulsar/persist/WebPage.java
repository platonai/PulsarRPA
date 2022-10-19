/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package ai.platon.pulsar.persist;

import ai.platon.pulsar.common.HtmlIntegrity;
import ai.platon.pulsar.common.browser.BrowserType;
import ai.platon.pulsar.common.config.VolatileConfig;
import ai.platon.pulsar.persist.experimental.WebAsset;
import ai.platon.pulsar.persist.gora.generated.GHypeLink;
import ai.platon.pulsar.persist.metadata.FetchMode;
import ai.platon.pulsar.persist.model.ActiveDOMStat;
import ai.platon.pulsar.persist.model.ActiveDOMStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * The core web page structure
 */
public interface WebPage extends Comparable<WebPage>, WebAsset {
    WebPage NIL = new EmptyWebPage();

    default boolean isNil() {
        return this == NIL;
    }
    default boolean isNotNil() {
        return !isNil();
    }

    boolean isInternal();
    boolean isNotInternal();

    /**
     * Check if the page is canceled.
     * <p>
     * If a page is canceled, it should not be updated.
     * */
    boolean isCanceled();
    boolean isCached();
    boolean isLoaded();
    boolean isFetched();
    boolean isContentUpdated();
    Variables getVariables();
    Object getVar(String name);
    boolean hasVar(String name);
    VolatileConfig getConf();
    String getProxy();
    boolean isSeed();
    boolean hasMark(String mark);
    PageCounters getPageCounters();
    int getMaxRetries();
    FetchMode getFetchMode();
    BrowserType getLastBrowser();
    ActiveDOMStatus getActiveDOMStatus();
    Map<String, ActiveDOMStat> getActiveDOMStatTrace();
    @NotNull
    ParseStatus getParseStatus();
    HtmlIntegrity getHtmlIntegrity();
    Map<CharSequence, GHypeLink> getLiveLinks();
    Collection<String> getSimpleLiveLinks();
    void addLiveLink(HyperlinkPersistable hyperLink);
    Map<CharSequence, CharSequence> getVividLinks();
    Collection<String> getSimpleVividLinks();
    List<CharSequence> getDeadLinks();
    List<CharSequence> getLinks();
    int getImpreciseLinkCount();
    Map<CharSequence, CharSequence> getInlinks();
    @NotNull
    CharSequence getAnchor();
    String[] getInlinkAnchors();
    int getAnchorOrder();
}
