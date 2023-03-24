package com.denisfesenko.core;

import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.jsoup.nodes.Node;

/**
 * The TagHandler interface defines the contract for handling custom tags
 * within an HTML document and manipulating WordprocessingMLPackage content
 * accordingly. Implementations of this interface are responsible for
 * processing and applying the required modifications based on the tag logic.
 */
public interface TagHandler {

    /**
     * Handles the custom tag within the HTML document and updates the
     * WordprocessingMLPackage content accordingly.
     *
     * @param node          The HTML node representing the custom tag to be processed.
     * @param wordMLPackage The WordprocessingMLPackage to be manipulated
     *                      based on the tag logic.
     */
    void handleTag(Node node, WordprocessingMLPackage wordMLPackage);

    /**
     * Determines whether the tag handler can be applied multiple times within
     * a document.
     *
     * @return True if the tag is repeatable; otherwise, false.
     */
    boolean isRepeatable();
}
