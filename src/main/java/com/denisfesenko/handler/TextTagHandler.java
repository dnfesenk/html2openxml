package com.denisfesenko.handler;

import com.denisfesenko.core.TagHandler;
import com.denisfesenko.util.RunUtils;
import org.docx4j.jaxb.Context;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.ObjectFactory;
import org.docx4j.wml.Text;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import java.util.Optional;

public class TextTagHandler implements TagHandler {

    private static final ObjectFactory OBJECT_FACTORY = Context.getWmlObjectFactory();

    @Override
    public void handleTag(Node node, WordprocessingMLPackage wordMLPackage) {
        Text text = OBJECT_FACTORY.createText();
        String nodeText = ((TextNode) node).text();
        text.setValue(nodeText);
        text.setSpace("preserve");
        RunUtils.getCurrentRun(wordMLPackage).getContent().add(text);
        Optional.ofNullable(RunUtils.getCurrentParagraph(wordMLPackage)).ifPresent(p -> p.getContent()
                .add(OBJECT_FACTORY.createR()));
    }
}
