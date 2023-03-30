package com.denisfesenko.handler;

import com.denisfesenko.core.TagHandler;
import com.denisfesenko.util.RunUtils;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.RPr;
import org.jsoup.nodes.Node;

/**
 * The ItalicHandler class is responsible for processing italic HTML tags
 * (both &lt;i&gt; and &lt;em&gt; tags). It implements the TagHandler interface.
 */
public class ItalicHandler implements TagHandler {

    /**
     * Handles the italic HTML tag by applying the italic style to the
     * current run properties (RPr) in the provided WordprocessingMLPackage.
     *
     * @param node          The HTML node representing the italic tag.
     * @param wordMLPackage The WordprocessingMLPackage to which the italic style is applied.
     */
    @Override
    public void handleTag(Node node, WordprocessingMLPackage wordMLPackage) {
        RPr rPr = RunUtils.getCurrentRPr(wordMLPackage);
        rPr.setI(RunUtils.createBooleanDefaultTrue());
        rPr.setICs(RunUtils.createBooleanDefaultTrue());
    }

    /**
     * Indicates whether the ItalicHandler can be applied multiple times to the same content.
     * In this case, it returns true, as the italic style can be applied multiple times.
     *
     * @return A boolean value, true if the handler is repeatable, false otherwise.
     */
    @Override
    public boolean isRepeatable() {
        return true;
    }
}
