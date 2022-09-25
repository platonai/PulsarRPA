/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.platon.pulsar.persist;

import ai.platon.pulsar.persist.gora.generated.GHypeLink;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * <p>An hype link in a page.</p>
 *
 * @author vincent
 * @version $Id: $Id
 */
public class HyperlinkPersistable implements Comparable<HyperlinkPersistable> {

    private GHypeLink hyperlink;

    private HyperlinkPersistable(@NotNull GHypeLink hyperlink) {
        this.hyperlink = hyperlink;
    }

    /**
     * <p>Constructor for Hyperlink.</p>
     *
     * @param url The of the hyper link.
     */
    public HyperlinkPersistable(@NotNull String url) {
        this(url, null);
    }

    /**
     * <p>Constructor for Hyperlink.</p>
     *
     * @param url a {@link java.lang.String} object.
     * @param text a {@link java.lang.String} object.
     */
    public HyperlinkPersistable(@NotNull String url, @Nullable String text) {
        this(url, text, 0);
    }

    /**
     * <p>Constructor for Hyperlink.</p>
     *
     * @param url a {@link java.lang.String} object.
     * @param text a {@link java.lang.String} object.
     * @param order a int.
     */
    public HyperlinkPersistable(@NotNull String url, @Nullable String text, int order) {
        Objects.requireNonNull(url);

        hyperlink = new GHypeLink();
        hyperlink.setUrl(url);
        hyperlink.setAnchor(text);
        hyperlink.setOrder(order);
    }

    /**
     * <p>box.</p>
     *
     * @param hyperlink a {@link ai.platon.pulsar.persist.gora.generated.GHypeLink} object.
     * @return a {@link HyperlinkPersistable} object.
     */
    @NotNull
    public static HyperlinkPersistable box(@NotNull GHypeLink hyperlink) {
        return new HyperlinkPersistable(hyperlink);
    }

    /**
     * <p>parse.</p>
     *
     * @param link a {@link java.lang.String} object.
     * @return a {@link HyperlinkPersistable} object.
     */
    @NotNull
    public static HyperlinkPersistable parse(@NotNull String link) {
        String[] urlText = link.split("\\s+");
        if (urlText.length == 1) {
            return new HyperlinkPersistable(urlText[0]);
        } else {
            return new HyperlinkPersistable(urlText[0], urlText[1]);
        }
    }

    /**
     * <p>equals.</p>
     *
     * @param l a {@link ai.platon.pulsar.persist.gora.generated.GHypeLink} object.
     * @param l2 a {@link ai.platon.pulsar.persist.gora.generated.GHypeLink} object.
     * @return a boolean.
     */
    public static boolean equals(GHypeLink l, GHypeLink l2) {
        return l.getUrl().equals(l2.getUrl());
    }

    /**
     * <p>unbox.</p>
     *
     * @return a {@link ai.platon.pulsar.persist.gora.generated.GHypeLink} object.
     */
    public GHypeLink unbox() {
        return hyperlink;
    }

    /**
     * <p>getUrl.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getUrl() {
        return hyperlink.getUrl().toString();
    }

    /**
     * <p>setUrl.</p>
     *
     * @param url a {@link java.lang.String} object.
     */
    public void setUrl(String url) {
        hyperlink.setUrl(url);
    }

    /**
     * Get the anchor text.
     *
     * The [anchor text](https://en.wikipedia.org/wiki/Anchor_text), link label or link text is the visible,
     * clickable text in an HTML hyperlink.
     * The term "anchor" was used in older versions of the HTML specification[1] for what is currently referred to
     * as the "a element".
     *
     * @return a {@link java.lang.String} object.
     */
    @NotNull
    public String getText() {
        CharSequence anchor = hyperlink.getAnchor();
        return anchor == null ? "" : anchor.toString();
    }

    /**
     * Set the anchor text.
     *
     * The [anchor text](https://en.wikipedia.org/wiki/Anchor_text), link label or link text is the visible,
     * clickable text in an HTML hyperlink.
     * The term "anchor" was used in older versions of the HTML specification[1] for what is currently referred to
     * as the "a element".
     *
     * @param text a {@link java.lang.String} object.
     */
    public void setText(@Nullable String text) {
        hyperlink.setAnchor(text);
    }

    /**
     * <p>getOrder.</p>
     *
     * @return a int.
     */
    public int getOrder() {
        return hyperlink.getOrder();
    }

    /**
     * <p>setOrder.</p>
     *
     * @param order a int.
     */
    public void setOrder(int order) {
        hyperlink.setOrder(order);
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof HyperlinkPersistable)) {
            return getUrl().equals(o.toString());
        }
        HyperlinkPersistable other = (HyperlinkPersistable) o;
        return getUrl().equals(other.getUrl());
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return getUrl().hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(@NotNull HyperlinkPersistable hyperLink) {
        int r = getUrl().compareTo(hyperLink.getUrl());
        if (r == 0) {
            r = getText().compareTo(hyperLink.getText());
            if (r == 0) {
                r = getOrder() - hyperLink.getOrder();
            }
        }
        return r;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return getUrl() + " " + getText() + " odr:" + getOrder();
    }
}
