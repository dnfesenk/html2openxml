package com.denisfesenko.handler;

import com.denisfesenko.core.TagHandler;
import com.denisfesenko.util.RunUtils;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.jsoup.nodes.Node;

public class BreakHandler implements TagHandler {

    @Override
    public void handleTag(Node node, WordprocessingMLPackage wordMLPackage) {
        RunUtils.getCurrentRun(wordMLPackage).getContent().add(RunUtils.getObjectFactory().createBr());
    }

    @Override
    public boolean isRepeatable() {
        return false;
    }
}
