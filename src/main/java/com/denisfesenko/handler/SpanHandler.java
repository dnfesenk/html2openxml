package com.denisfesenko.handler;

import com.denisfesenko.core.TagHandler;
import com.denisfesenko.util.Constants;
import com.denisfesenko.util.ConverterUtils;
import com.denisfesenko.util.RunUtils;
import org.apache.commons.lang3.StringUtils;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.Highlight;
import org.jsoup.nodes.Node;

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
        Node spanNode = ConverterUtils.findParentNode(node, "span");
        if (spanNode != null) {
            String style = StringUtils.isNotBlank(spanNode.attr(Constants.STYLE)) ? spanNode.attr(Constants.STYLE) : null;
            String bgColor = StringUtils.substringBetween(style, "background-color: ", ";");
            if (bgColor != null) {
                Highlight highlight = RunUtils.getObjectFactory().createHighlight();
                highlight.setVal(ConverterUtils.isHexColor(bgColor) ? bgColor : ConverterUtils.rgbToHex(bgColor));
                //skip unsupported color
                if (highlight.getVal() == null) {
                    highlight.setVal(Constants.HEX_WHITE_COLOR);
                }
                RunUtils.getCurrentRPr(wordMLPackage).setHighlight(highlight);
            }
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
