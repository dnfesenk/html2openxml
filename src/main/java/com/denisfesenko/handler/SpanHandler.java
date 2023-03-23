package com.denisfesenko.handler;

import com.denisfesenko.core.TagHandler;
import com.denisfesenko.util.ConverterUtils;
import com.denisfesenko.util.RunUtils;
import org.apache.commons.lang3.StringUtils;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.Highlight;
import org.jsoup.nodes.Node;

public class SpanHandler implements TagHandler {

    @Override
    public void handleTag(Node node, WordprocessingMLPackage wordMLPackage) {
        Node spanNode = ConverterUtils.findParentNode(node, "span");
        if (spanNode != null) {
            String style = StringUtils.isNotBlank(spanNode.attr("style")) ? spanNode.attr("style") : null;
            String bgColor = StringUtils.substringBetween(style, "background-color: ", ";");
            if (bgColor != null) {
                Highlight highlight = RunUtils.getObjectFactory().createHighlight();
                highlight.setVal(ConverterUtils.rgbToHex(bgColor));
                //skip unsupported color
                if (highlight.getVal() == null) {
                    highlight.setVal("#ffffff");
                }
                RunUtils.getCurrentRPr(wordMLPackage).setHighlight(highlight);
            }
        }
    }

    @Override
    public boolean isRepeatable() {
        return true;
    }
}
