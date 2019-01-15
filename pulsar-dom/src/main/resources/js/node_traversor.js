/**
 * Created by vincent on 16-5-17.
 */

"use strict";

/**
 * Create a new traversor.
 *
 * @param visitor {WarpsNodeVisitor} a class implementing the {@link WarpsNodeVisitor} interface, to be called when visiting each node.
 * @param options {Object} options, currently, only one option : options.diagnosis(= false);
 */
function WarpsNodeTraversor(visitor, options) {
    this.visitor = visitor;
    this.options = {
        diagnosis : false
    };

    if (arguments.length > 1) {
        options = arguments[1];
    }

    if (options) {
        // override default options
        for (let prop in options) {
            if (options[prop]) {
                this.options[prop] = options[prop];
            }
        }
    }
}

/**
 * Start a depth-first traverse of the root and all of its descendants.
 * @param root {Node} the root ele point to traverse.
 */
WarpsNodeTraversor.prototype.traverse = function(root) {
    let node = root;
    let depth = 0;

    while (node != null) {
        this.visitor.head(node, depth);
        if (node.childNodes.length > 0) {
            node = node.childNodes[0];
            depth++;
        } else {
            while (node.nextSibling == null && depth > 0) {
                this.visitor.tail(node, depth);
                node = node.parentNode;
                depth--;
            }
            this.visitor.tail(node, depth);
            if (node === root)
                break;
            node = node.nextSibling;
        }
    }
};
