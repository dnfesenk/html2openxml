package com.denisfesenko.handler;

import com.denisfesenko.core.TagHandler;
import com.denisfesenko.util.RunUtils;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.STBrType;
import org.jsoup.nodes.Node;

public class PageBreakHandler implements TagHandler {

    @Override
    public void handleTag(Node node, WordprocessingMLPackage wordMLPackage) {
        var br = RunUtils.getObjectFactory().createBr();
        br.setType(STBrType.PAGE);
        RunUtils.getCurrentRun(wordMLPackage).getContent().add(br);
    }

    @Override
    public boolean isRepeatable() {
        return false;
    }
}
