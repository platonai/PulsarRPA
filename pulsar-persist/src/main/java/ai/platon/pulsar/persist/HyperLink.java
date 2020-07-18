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
public class HyperLink implements Comparable<HyperLink> {

    private GHypeLink hypeLink;

    private HyperLink(@NotNull GHypeLink hypeLink) {
        this.hypeLink = hypeLink;
    }

    public HyperLink(@NotNull String url) {
        this(url, null);
    }

    public HyperLink(@NotNull String url, @Nullable String anchor) {
        this(url, anchor, 0);
    }

    public HyperLink(@NotNull String url, @Nullable String anchor, int order) {
        Objects.requireNonNull(url);

        hypeLink = new GHypeLink();
        hypeLink.setUrl(url);
        hypeLink.setAnchor(anchor);
        hypeLink.setOrder(order);
    }

    @NotNull
    public static HyperLink box(@NotNull GHypeLink hypeLink) {
        return new HyperLink(hypeLink);
    }

    @NotNull
    public static HyperLink parse(@NotNull String link) {
        String[] linkAnchor = link.split("\\s+");
        if (linkAnchor.length == 1) {
            return new HyperLink(linkAnchor[0]);
        } else {
            return new HyperLink(linkAnchor[0], linkAnchor[1]);
        }
    }

    public static boolean equals(GHypeLink l, GHypeLink l2) {
        return l.getUrl().equals(l2.getUrl());
    }

    public GHypeLink unbox() {
        return hypeLink;
    }

    public String getUrl() {
        return hypeLink.getUrl().toString();
    }

    public void setUrl(String url) {
        hypeLink.setUrl(url);
    }

    @NotNull
    public String getAnchor() {
        CharSequence anchor = hypeLink.getAnchor();
        return anchor == null ? "" : anchor.toString();
    }

    public void setAnchor(@Nullable String anchor) {
        hypeLink.setAnchor(anchor);
    }

    public int getOrder() {
        return hypeLink.getOrder();
    }

    public void setOrder(int order) {
        hypeLink.setOrder(order);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof HyperLink)) {
            return getUrl().equals(o.toString());
        }
        HyperLink other = (HyperLink) o;
        return getUrl().equals(other.getUrl());
    }

    @Override
    public int hashCode() {
        return getUrl().hashCode();
    }

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

    @Override
    public String toString() {
        return getUrl() + " " + getAnchor() + " odr:" + getOrder();
    }
}
