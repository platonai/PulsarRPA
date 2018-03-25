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
package fun.platonic.pulsar.persist.graph;

import fun.platonic.pulsar.persist.metadata.MultiMetadata;
import fun.platonic.pulsar.persist.metadata.Name;
import org.jgrapht.graph.DefaultWeightedEdge;
import fun.platonic.pulsar.persist.WebPage;

public class WebEdge extends DefaultWeightedEdge {

    private CharSequence anchor = "";
    private CharSequence options = "";
    private int order = -1;
    private MultiMetadata metadata = new MultiMetadata();

    public WebEdge() {
    }

    public CharSequence getAnchor() {
        return anchor;
    }

    public void setAnchor(CharSequence anchor) {
        this.anchor = anchor;
    }

    public CharSequence getOptions() {
        return options;
    }

    public void setOptions(CharSequence options) {
        this.options = options;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public void putMetadata(Name k, String v) {
        metadata.put(k.text(), v);
    }

    public void putMetadata(String k, String v) {
        metadata.put(k, v);
    }

    public String getMetadata(Name k, String defaultValue) {
        String value = metadata.get(k.text());
        return value == null ? defaultValue : value;
    }

    public boolean hasMetadata(Name k) {
        return metadata.get(k.text()) != null;
    }

    public MultiMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(MultiMetadata metadata) {
        this.metadata = metadata;
    }

    public boolean isLoop() {
        return getSource().equals(getTarget());
    }

    /**
     * Retrieves the source of this edge.
     *
     * @return source of this edge
     */
    public WebVertex getSource() {
        return (WebVertex) super.getSource();
    }

    public String getSourceUrl() {
        return getSource().getUrl();
    }

    public WebPage getSourceWebPage() {
        return getSource().getWebPage();
    }

    public boolean hasSourceWebPage() {
        return getSource().hasWebPage();
    }

    /**
     * Retrieves the target of this edge.
     *
     * @return target of this edge
     */
    public WebVertex getTarget() {
        return (WebVertex) super.getTarget();
    }

    public String getTargetUrl() {
        return getTarget().getUrl();
    }

    public WebPage getTargetWebPage() {
        return getTarget().getWebPage();
    }

    public boolean hasTargetWebPage() {
        return getTarget().hasWebPage();
    }

    @Override
    public String toString() {
        return getSourceUrl() + " -> " + getTargetUrl();
    }
}
