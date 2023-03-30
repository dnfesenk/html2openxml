package com.denisfesenko.handler;

import com.denisfesenko.core.TagHandler;
import com.denisfesenko.util.RunUtils;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.CTVerticalAlignRun;
import org.docx4j.wml.STVerticalAlignRun;
import org.jsoup.nodes.Node;

/**
 * The SubHandler class is an implementation of the TagHandler interface. It is responsible for handling
 * sub (subscript) nodes in an HTML document and adding them to a WordprocessingMLPackage as a CTVerticalAlignRun
 * with the SUBSCRIPT value.
 */
public class SubHandler implements TagHandler {

    /**
     * Handles a subscript node by creating a CTVerticalAlignRun object with the SUBSCRIPT value and adding it to
     * the WordprocessingMLPackage.
     *
     * @param node          the subscript node to be handled
     * @param wordMLPackage the WordprocessingMLPackage to which the CTVerticalAlignRun object will be added
     */
    @Override
    public void handleTag(Node node, WordprocessingMLPackage wordMLPackage) {
        CTVerticalAlignRun vertAlign = RunUtils.getObjectFactory().createCTVerticalAlignRun();
        vertAlign.setVal(STVerticalAlignRun.SUBSCRIPT);
        RunUtils.getCurrentRPr(wordMLPackage).setVertAlign(vertAlign);
    }

    /**
     * Determines if the tag handler is repeatable. In this implementation, it is repeatable.
     *
     * @return true, as the SubHandler is repeatable
     */
    @Override
    public boolean isRepeatable() {
        return true;
    }
}
