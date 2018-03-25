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

package fun.platonic.pulsar.persist;

import fun.platonic.pulsar.persist.gora.generated.GHypeLink;

import javax.annotation.Nonnull;

/* An hype link in a page. */
public class HypeLink implements Comparable<HypeLink> {

    private GHypeLink hypeLink;

    private HypeLink(GHypeLink hypeLink) {
        this.hypeLink = hypeLink;
    }

    public HypeLink(String url, String anchor) {
        hypeLink = new GHypeLink();
        hypeLink.setUrl(url);
        hypeLink.setAnchor(anchor);
        hypeLink.setOrder(0);
    }

    public HypeLink(String url) {
        this(url, "");
    }

    @Nonnull
    public static HypeLink box(GHypeLink hypeLink) {
        return new HypeLink(hypeLink);
    }

    @Nonnull
    public static HypeLink parse(String link) {
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

    public String getAnchor() {
        return hypeLink.getAnchor().toString();
    }

    public void setAnchor(String anchor) {
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
    public int compareTo(@Nonnull HypeLink hypeLink) {
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
