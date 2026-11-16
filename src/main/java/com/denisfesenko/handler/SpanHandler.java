package com.denisfesenko.handler;

import com.denisfesenko.core.TagHandler;
import com.denisfesenko.util.Constants;
import com.denisfesenko.util.ConverterUtils;
import com.denisfesenko.util.RunUtils;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.CTShd;
import org.docx4j.wml.STShd;
import org.jsoup.nodes.Node;
import java.util.List;

/**
 * The SpanHandler class is an implementation of the TagHandler interface. It is responsible for handling
 * span nodes in an HTML document, specifically dealing with background colors and adding them as highlights
 * in a WordprocessingMLPackage.
 */
public class SpanHandler implements TagHandler {

    /**
     * Handles a span node by adding its background color as a highlight in the WordprocessingMLPackage.
     *
     * @param node          the span node to be handled
     * @param wordMLPackage the WordprocessingMLPackage to which the highlight will be added
     */
    @Override
    public void handleTag(Node node, WordprocessingMLPackage wordMLPackage) {
        applyFormatting(ConverterUtils.findParentNode(node, "span"), wordMLPackage);
    }

    @Override
    public void handleTag(Node node, List<Node> sources, WordprocessingMLPackage wordMLPackage) {
        sources.forEach(source -> applyFormatting(source, wordMLPackage));
    }

    private void applyFormatting(Node spanNode, WordprocessingMLPackage wordMLPackage) {
        String color = ConverterUtils.parseColor(ConverterUtils.getStyleProperty(spanNode.attr(Constants.STYLE), "background-color"));
        if (color != null) {
            CTShd shading = RunUtils.getObjectFactory().createCTShd();
            shading.setVal(STShd.CLEAR);
            shading.setColor("auto");
            shading.setFill(color);
            RunUtils.getCurrentRPr(wordMLPackage).setShd(shading);
        }
    }

    /**
     * Determines if the tag handler is repeatable. In this implementation, it is repeatable.
     *
     * @return true, as the SpanHandler is repeatable
     */
    @Override
    public boolean isRepeatable() {
        return true;
    }
}
