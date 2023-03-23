package com.denisfesenko.handler;

import com.denisfesenko.core.TagHandler;
import com.denisfesenko.util.RunUtils;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.RPr;
import org.jsoup.nodes.Node;

public class BoldTagHandler implements TagHandler {

    @Override
    public void handleTag(Node node, WordprocessingMLPackage wordMLPackage) {
        RPr rPr = RunUtils.getCurrentRPr(wordMLPackage);
        rPr.setB(RunUtils.createBooleanDefaultTrue());
        rPr.setBCs(RunUtils.createBooleanDefaultTrue());
    }

    @Override
    public boolean isRepeatable() {
        return true;
    }
}
