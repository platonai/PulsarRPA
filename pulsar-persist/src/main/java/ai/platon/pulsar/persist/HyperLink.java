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

/* An hype link in a page. */
/**
 * <p>HyperLink class.</p>
 *
 * @author vincent
 * @version $Id: $Id
 */
public class HyperLink implements Comparable<HyperLink> {

    private GHypeLink hypeLink;

    private HyperLink(@NotNull GHypeLink hypeLink) {
        this.hypeLink = hypeLink;
    }

    /**
     * <p>Constructor for HyperLink.</p>
     *
     * @param url a {@link java.lang.String} object.
     */
    public HyperLink(@NotNull String url) {
        this(url, null);
    }

    /**
     * <p>Constructor for HyperLink.</p>
     *
     * @param url a {@link java.lang.String} object.
     * @param anchor a {@link java.lang.String} object.
     */
    public HyperLink(@NotNull String url, @Nullable String anchor) {
        this(url, anchor, 0);
    }

    /**
     * <p>Constructor for HyperLink.</p>
     *
     * @param url a {@link java.lang.String} object.
     * @param anchor a {@link java.lang.String} object.
     * @param order a int.
     */
    public HyperLink(@NotNull String url, @Nullable String anchor, int order) {
        Objects.requireNonNull(url);

        hypeLink = new GHypeLink();
        hypeLink.setUrl(url);
        hypeLink.setAnchor(anchor);
        hypeLink.setOrder(order);
    }

    /**
     * <p>box.</p>
     *
     * @param hypeLink a {@link ai.platon.pulsar.persist.gora.generated.GHypeLink} object.
     * @return a {@link ai.platon.pulsar.persist.HyperLink} object.
     */
    @NotNull
    public static HyperLink box(@NotNull GHypeLink hypeLink) {
        return new HyperLink(hypeLink);
    }

    /**
     * <p>parse.</p>
     *
     * @param link a {@link java.lang.String} object.
     * @return a {@link ai.platon.pulsar.persist.HyperLink} object.
     */
    @NotNull
    public static HyperLink parse(@NotNull String link) {
        String[] linkAnchor = link.split("\\s+");
        if (linkAnchor.length == 1) {
            return new HyperLink(linkAnchor[0]);
        } else {
            return new HyperLink(linkAnchor[0], linkAnchor[1]);
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
        return hypeLink;
    }

    /**
     * <p>getUrl.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getUrl() {
        return hypeLink.getUrl().toString();
    }

    /**
     * <p>setUrl.</p>
     *
     * @param url a {@link java.lang.String} object.
     */
    public void setUrl(String url) {
        hypeLink.setUrl(url);
    }

    /**
     * <p>getAnchor.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @NotNull
    public String getAnchor() {
        CharSequence anchor = hypeLink.getAnchor();
        return anchor == null ? "" : anchor.toString();
    }

    /**
     * <p>setAnchor.</p>
     *
     * @param anchor a {@link java.lang.String} object.
     */
    public void setAnchor(@Nullable String anchor) {
        hypeLink.setAnchor(anchor);
    }

    /**
     * <p>getOrder.</p>
     *
     * @return a int.
     */
    public int getOrder() {
        return hypeLink.getOrder();
    }

    /**
     * <p>setOrder.</p>
     *
     * @param order a int.
     */
    public void setOrder(int order) {
        hypeLink.setOrder(order);
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof HyperLink)) {
            return getUrl().equals(o.toString());
        }
        HyperLink other = (HyperLink) o;
        return getUrl().equals(other.getUrl());
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return getUrl().hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(@NotNull HyperLink hyperLink) {
        int r = getUrl().compareTo(hyperLink.getUrl());
        if (r == 0) {
            r = getAnchor().compareTo(hyperLink.getAnchor());
            if (r == 0) {
                r = getOrder() - hyperLink.getOrder();
            }
        }
        return r;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return getUrl() + " " + getAnchor() + " odr:" + getOrder();
    }
}
