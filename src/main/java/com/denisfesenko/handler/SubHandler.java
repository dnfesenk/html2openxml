package com.denisfesenko.handler;

import com.denisfesenko.core.TagHandler;
import com.denisfesenko.util.RunUtils;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.CTVerticalAlignRun;
import org.docx4j.wml.STVerticalAlignRun;
import org.jsoup.nodes.Node;

public class SubHandler implements TagHandler {

    @Override
    public void handleTag(Node node, WordprocessingMLPackage wordMLPackage) {
        CTVerticalAlignRun vertAlign = RunUtils.getObjectFactory().createCTVerticalAlignRun();
        vertAlign.setVal(STVerticalAlignRun.SUBSCRIPT);
        RunUtils.getCurrentRPr(wordMLPackage).setVertAlign(vertAlign);
    }

    @Override
    public boolean isRepeatable() {
        return true;
    }
}
