package com.denisfesenko.handler;

import com.denisfesenko.core.TagHandler;
import com.denisfesenko.util.RunUtils;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.jsoup.nodes.Node;

/**
 * The BreakHandler class is responsible for processing the HTML line break tag (&lt;br&gt;).
 * It implements the TagHandler interface.
 */
public class BreakHandler implements TagHandler {

    /**
     * Handles the HTML line break tag by adding a break to the current run in the
     * provided WordprocessingMLPackage.
     *
     * @param node          The HTML node representing the line break tag.
     * @param wordMLPackage The WordprocessingMLPackage to which the break is added.
     */
    @Override
    public void handleTag(Node node, WordprocessingMLPackage wordMLPackage) {
        RunUtils.getCurrentRun(wordMLPackage).getContent().add(RunUtils.getObjectFactory().createBr());
    }

    /**
     * Indicates whether the BreakHandler can be applied multiple times to the same content.
     * In this case, it returns false, as the line break should not be applied multiple times.
     *
     * @return A boolean value, true if the handler is repeatable, false otherwise.
     */
    @Override
    public boolean isRepeatable() {
        return false;
    }
}
