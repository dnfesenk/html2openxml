package com.denisfesenko.handler;

import com.denisfesenko.core.TagHandler;
import com.denisfesenko.util.RunUtils;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.RPr;
import org.docx4j.wml.U;
import org.docx4j.wml.UnderlineEnumeration;
import org.jsoup.nodes.Node;

public class UnderlineHandler implements TagHandler {

    @Override
    public void handleTag(Node node, WordprocessingMLPackage wordMLPackage) {
        RPr rPr = RunUtils.getCurrentRPr(wordMLPackage);
        U u = RunUtils.getObjectFactory().createU();
        u.setVal(UnderlineEnumeration.SINGLE);
        rPr.setU(u);
    }

    @Override
    public boolean isRepeatable() {
        return true;
    }
}
