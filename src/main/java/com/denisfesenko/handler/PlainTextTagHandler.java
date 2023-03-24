package com.denisfesenko.handler;

import com.denisfesenko.core.TagHandler;
import com.denisfesenko.util.RunUtils;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.Text;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

public class PlainTextTagHandler implements TagHandler {

    @Override
    public void handleTag(Node node, WordprocessingMLPackage wordMLPackage) {
        Text text = RunUtils.getObjectFactory().createText();
        String nodeText = ((TextNode) node).text();
        text.setValue(nodeText);
        text.setSpace("preserve");
        RunUtils.getCurrentRun(wordMLPackage).getContent().add(text);
    }

    @Override
    public boolean isRepeatable() {
        return false;
    }
}
