package com.denisfesenko.handler;

import com.denisfesenko.core.TagHandler;
import com.denisfesenko.util.RunUtils;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.Text;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

/**
 * The PlainTextHandler class is an implementation of the TagHandler interface. It is responsible for handling plain text
 * nodes in an HTML document and adding them to a WordprocessingMLPackage as a Text object.
 */
public class PlainTextHandler implements TagHandler {

    /**
     * Handles a plain text node by creating a Text object and adding it to the WordprocessingMLPackage.
     *
     * @param node          the plain text node to be handled
     * @param wordMLPackage the WordprocessingMLPackage to which the Text object will be added
     */
    @Override
    public void handleTag(Node node, WordprocessingMLPackage wordMLPackage) {
        Text text = RunUtils.getObjectFactory().createText();
        String nodeText = ((TextNode) node).text();
        text.setValue(nodeText);
        text.setSpace("preserve");
        RunUtils.getCurrentRun(wordMLPackage).getContent().add(text);
    }

    /**
     * Determines if the tag handler is repeatable. In this implementation, it is not repeatable.
     *
     * @return false, as the PlainTextHandler is not repeatable
     */
    @Override
    public boolean isRepeatable() {
        return false;
    }
}
