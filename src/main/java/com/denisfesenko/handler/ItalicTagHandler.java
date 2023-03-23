package com.denisfesenko.handler;

import com.denisfesenko.core.TagHandler;
import com.denisfesenko.util.RunUtils;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.RPr;
import org.jsoup.nodes.Node;

public class ItalicTagHandler implements TagHandler {

    @Override
    public void handleTag(Node node, WordprocessingMLPackage wordMLPackage) {
        RPr rPr = RunUtils.getCurrentRPr(wordMLPackage);
        rPr.setI(RunUtils.createBooleanDefaultTrue());
        rPr.setICs(RunUtils.createBooleanDefaultTrue());
    }

    @Override
    public boolean isRepeatable() {
        return true;
    }
}
