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

/* An hype link in a page. */
public class HypeLink implements Comparable<HypeLink> {

    private GHypeLink hypeLink;

    private HypeLink(@NotNull GHypeLink hypeLink) {
        this.hypeLink = hypeLink;
    }

    public HypeLink(@NotNull String url) {
        this(url, null);
    }

    public HypeLink(@NotNull String url, @Nullable String anchor) {
        this(url, anchor, 0);
    }

    public HypeLink(@NotNull String url, @Nullable String anchor, int order) {
        hypeLink = new GHypeLink();
        hypeLink.setUrl(url);
        hypeLink.setAnchor(anchor);
        hypeLink.setOrder(order);
    }

    @NotNull
    public static HypeLink box(@NotNull GHypeLink hypeLink) {
        return new HypeLink(hypeLink);
    }

    @NotNull
    public static HypeLink parse(@NotNull String link) {
        String[] linkAnchor = link.split("\\s+");
        if (linkAnchor.length == 1) {
            return new HypeLink(linkAnchor[0]);
        } else {
            return new HypeLink(linkAnchor[0], linkAnchor[1]);
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

    public void setAnchor(@NotNull String anchor) {
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
        if (!(o instanceof HypeLink)) {
            return getUrl().equals(o.toString());
        }
        HypeLink other = (HypeLink) o;
        return getUrl().equals(other.getUrl());
    }

    @Override
    public int hashCode() {
        return getUrl().hashCode();
    }

    @Override
    public int compareTo(@NotNull HypeLink hypeLink) {
        int r = getUrl().compareTo(hypeLink.getUrl());
        if (r == 0) {
            r = getAnchor().compareTo(hypeLink.getAnchor());
            if (r == 0) {
                r = getOrder() - hypeLink.getOrder();
            }
        }
        return r;
    }

    @Override
    public String toString() {
        return getUrl() + " " + getAnchor();
    }
}
