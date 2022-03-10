/**
 * Created by vincent on 16-5-17.
 */

"use strict";

/**
 * Create a new traversor.
 *
 * @param visitor {Object} a class implementing the {@link NodeFeatureCalculator} interface, to be called when visiting each node.
 */
function PulsarNodeTraversor(visitor) {
    this.visitor = visitor;
    this.options = {
        diagnosis : false
    };

    if (arguments.length > 1) {
        this.options = arguments[1];
    }
}

/**
 * Start a depth-first traverse of the root and all of its descendants.
 * @param root {Node} the root node point to traverse.
 */
PulsarNodeTraversor.prototype.traverse = function(root) {
    let node = root;
    let depth = 0;
    let visitor = this.visitor;
    if (!visitor.tail) {
        // empty function
        visitor.tail = function () {}
    }

    while (node != null) {
        visitor.head(node, depth);
        if (node.childNodes.length > 0) {
            node = node.childNodes[0];
            depth++;
        } else {
            while (node.nextSibling == null && depth > 0) {
                visitor.tail(node, depth);
                node = node.parentNode;
                depth--;
            }
            visitor.tail(node, depth);
            if (node === root)
                break;
            node = node.nextSibling;
        }
    }
};
