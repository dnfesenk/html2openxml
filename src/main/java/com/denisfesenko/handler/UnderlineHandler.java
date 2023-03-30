package com.denisfesenko.handler;

import com.denisfesenko.core.TagHandler;
import com.denisfesenko.util.RunUtils;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.RPr;
import org.docx4j.wml.U;
import org.docx4j.wml.UnderlineEnumeration;
import org.jsoup.nodes.Node;

/**
 * This class handles the conversion of HTML underline tags to WordprocessingMLPackage format.
 */
public class UnderlineHandler implements TagHandler {

    /**
     * This method is called to process the input node and apply the underline formatting to
     * the corresponding WordprocessingMLPackage format.
     *
     * @param node          The input HTML node to be processed.
     * @param wordMLPackage The WordprocessingMLPackage instance where the underline formatting
     *                      will be applied.
     */
    @Override
    public void handleTag(Node node, WordprocessingMLPackage wordMLPackage) {
        RPr rPr = RunUtils.getCurrentRPr(wordMLPackage);
        U u = RunUtils.getObjectFactory().createU();
        u.setVal(UnderlineEnumeration.SINGLE);
        rPr.setU(u);
    }

    /**
     * This method returns whether the tag handler can be applied to multiple tags.
     *
     * @return true, as the UnderlineHandler can be applied to multiple tags.
     */
    @Override
    public boolean isRepeatable() {
        return true;
    }
}
