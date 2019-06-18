/**
 * Created by vincent on 16-5-17.
 */

"use strict";

/**
 * Create a new traversor.
 *
 * @param visitor {PlatonNodeVisitor} a class implementing the {@link PlatonNodeVisitor} interface, to be called when visiting each node.
 */
function PlatonNodeTraversor(visitor) {
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
 * @param root {Node} the root ele point to traverse.
 */
PlatonNodeTraversor.prototype.traverse = function(root) {
    let node = root;
    let depth = 0;
    let visitor = this.visitor;

    while (node != null) {
        if (visitor.head) {
            visitor.head(node, depth);
        }
        if (node.childNodes.length > 0) {
            node = node.childNodes[0];
            depth++;
        } else {
            while (node.nextSibling == null && depth > 0) {
                if (visitor.tail) {
                    visitor.tail(node, depth);
                }
                node = node.parentNode;
                depth--;
            }
            if (visitor.tail) {
                visitor.tail(node, depth);
            }
            if (node === root)
                break;
            node = node.nextSibling;
        }
    }
};
