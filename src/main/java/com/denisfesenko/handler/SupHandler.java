package com.denisfesenko.handler;

import com.denisfesenko.core.TagHandler;
import com.denisfesenko.util.RunUtils;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.CTVerticalAlignRun;
import org.docx4j.wml.STVerticalAlignRun;
import org.jsoup.nodes.Node;

/**
 * The SupHandler class is an implementation of the TagHandler interface. It is responsible for handling
 * sup (superscript) nodes in an HTML document and adding them to a WordprocessingMLPackage as a CTVerticalAlignRun
 * with the SUPERSCRIPT value.
 */
public class SupHandler implements TagHandler {

    /**
     * Handles a superscript node by creating a CTVerticalAlignRun object with the SUPERSCRIPT value and adding it to
     * the WordprocessingMLPackage.
     *
     * @param node          the superscript node to be handled
     * @param wordMLPackage the WordprocessingMLPackage to which the CTVerticalAlignRun object will be added
     */
    @Override
    public void handleTag(Node node, WordprocessingMLPackage wordMLPackage) {
        CTVerticalAlignRun vertAlign = RunUtils.getObjectFactory().createCTVerticalAlignRun();
        vertAlign.setVal(STVerticalAlignRun.SUPERSCRIPT);
        RunUtils.getCurrentRPr(wordMLPackage).setVertAlign(vertAlign);
    }

    /**
     * Determines if the tag handler is repeatable. In this implementation, it is repeatable.
     *
     * @return true, as the SupHandler is repeatable
     */
    @Override
    public boolean isRepeatable() {
        return true;
    }
}
