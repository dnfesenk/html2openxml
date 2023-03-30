package com.denisfesenko.handler;

import com.denisfesenko.core.TagHandler;
import com.denisfesenko.util.RunUtils;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.STBrType;
import org.jsoup.nodes.Node;

/**
 * The PageBreakHandler class is responsible for processing the custom
 * HTML page break tag (&lt;pb&gt;). It implements the TagHandler interface.
 */
public class PageBreakHandler implements TagHandler {

    /**
     * Handles the custom HTML page break tag by inserting a page break
     * in the provided WordprocessingMLPackage.
     *
     * @param node          The HTML node representing the page break tag.
     * @param wordMLPackage The WordprocessingMLPackage to which the page break is added.
     */
    @Override
    public void handleTag(Node node, WordprocessingMLPackage wordMLPackage) {
        var br = RunUtils.getObjectFactory().createBr();
        br.setType(STBrType.PAGE);
        RunUtils.getCurrentRun(wordMLPackage).getContent().add(br);
    }

    /**
     * Indicates whether the PageBreakHandler can be applied multiple times to the same content.
     * In this case, it returns false, as the page break should not be applied multiple times.
     *
     * @return A boolean value, true if the handler is repeatable, false otherwise.
     */
    @Override
    public boolean isRepeatable() {
        return false;
    }
}
