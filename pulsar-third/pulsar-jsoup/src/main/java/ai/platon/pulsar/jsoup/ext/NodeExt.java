/*
 * Copyright (c) 2022.
 *
 * This program is free software: you can use, redistribute, and/or modify
 * it under the terms of the GNU Affero General Public License, version 3
 * or later ("AGPL"), as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package ai.platon.pulsar.jsoup.ext;

import org.apache.commons.math3.linear.OpenMapRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The pulsar extension.
 *
 * We have two modifications to jsoup:
 * 1. add NodeExt to Node
 * 2. make NodeVisitor and NodeFilter compatible with kotlin lambda
 * */
public class NodeExt {
    public static final RealVector EMPTY_FEATURE = new OpenMapRealVector();

    private final Node node;
    private Node ownerDocumentNode;
    private Node ownerBody;
    private String immutableText;
    private RealVector features;
    private Map<String, Object> variables;
    private Map<String, List<Object>> tuples;

    public NodeExt(Node node) {
        this.node = node;
    }

    public void removeAttrs(String... attrNames) {
        for (String attrName : attrNames) {
            node.removeAttr(attrName);
        }
    }

    @Nullable
    public Node getOwnerDocumentNode() {
        if (ownerDocumentNode == null) {
            ownerDocumentNode = node.ownerDocument();
        }
        return ownerDocumentNode;
    }

    public void setOwnerDocumentNode(Node ownerDocumentNode) {
        this.ownerDocumentNode = ownerDocumentNode;
    }

    @Nullable
    public Node getOwnerBody() {
        if (ownerBody == null) {
            Document doc = node.ownerDocument();
            if (doc != null) {
                return doc.body();
            }
        }
        return ownerBody;
    }

    public void setOwnerBody(Node ownerBody) {
        this.ownerBody = ownerBody;
    }

    @Nonnull
    public String getImmutableText() {
        if (immutableText == null) {
            immutableText = "";
        }
        return immutableText;
    }

    public void setImmutableText(String immutableText) {
        this.immutableText = immutableText;
    }

    @Nonnull
    public RealVector getFeatures() {
        if (features == null) {
            features = new OpenMapRealVector();
        }
        return features;
    }

    public void setFeatures(RealVector features) {
        this.features = features;
    }

    @Nonnull
    public Map<String, Object> getVariables() {
        if (variables == null) {
            variables = new HashMap<>();
        }
        return variables;
    }

    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }

    @Nonnull
    public Map<String, List<Object>> getTuples() {
        if (tuples == null) {
            tuples = new HashMap<>();
        }
        return tuples;
    }

    public void setTuples(Map<String, List<Object>> tuples) {
        this.tuples = tuples;
    }
}
