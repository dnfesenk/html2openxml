package com.denisfesenko.handler;

import com.denisfesenko.core.TagHandler;
import com.denisfesenko.util.RunUtils;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.RPr;
import org.jsoup.nodes.Node;

/**
 * The BoldHandler class is responsible for processing bold HTML tags
 * (both &lt;b&gt; and &lt;strong&gt; tags). It implements the TagHandler interface.
 */
public class BoldHandler implements TagHandler {

    /**
     * Handles the bold HTML tag by applying the bold style to the
     * current run properties (RPr) in the provided WordprocessingMLPackage.
     *
     * @param node          The HTML node representing the bold tag.
     * @param wordMLPackage The WordprocessingMLPackage to which the bold style is applied.
     */
    @Override
    public void handleTag(Node node, WordprocessingMLPackage wordMLPackage) {
        RPr rPr = RunUtils.getCurrentRPr(wordMLPackage);
        rPr.setB(RunUtils.createBooleanDefaultTrue());
        rPr.setBCs(RunUtils.createBooleanDefaultTrue());
    }

    /**
     * Indicates whether the BoldHandler can be applied multiple times to the same content.
     * In this case, it returns true, as the bold style can be applied multiple times.
     *
     * @return A boolean value, true if the handler is repeatable, false otherwise.
     */
    @Override
    public boolean isRepeatable() {
        return true;
    }
}
