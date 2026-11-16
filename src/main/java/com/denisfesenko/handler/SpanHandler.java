package com.denisfesenko.handler;

import com.denisfesenko.core.TagHandler;
import com.denisfesenko.util.Constants;
import com.denisfesenko.util.ConverterUtils;
import com.denisfesenko.util.RunUtils;
import org.apache.commons.lang3.StringUtils;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.CTShd;
import org.docx4j.wml.STShd;
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
        handleTag(node, ConverterUtils.findParentNode(node, "span"), wordMLPackage);
    }

    @Override
    public void handleTag(Node node, Node spanNode, WordprocessingMLPackage wordMLPackage) {
        String style = StringUtils.isNotBlank(spanNode.attr(Constants.STYLE)) ? spanNode.attr(Constants.STYLE) : null;
        String bgColor = StringUtils.substringBetween(style, "background-color: ", ";");
        if (bgColor != null) {
            CTShd shading = RunUtils.getObjectFactory().createCTShd();
            shading.setVal(STShd.CLEAR);
            shading.setColor("auto");
            String color = ConverterUtils.isHexColor(bgColor)
                    ? bgColor : ConverterUtils.rgbToHex(bgColor, Constants.HEX_WHITE_COLOR);
            shading.setFill(color.replace("#", ""));
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
