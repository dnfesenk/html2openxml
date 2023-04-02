package com.denisfesenko.handler;

import com.denisfesenko.core.TagHandler;
import com.denisfesenko.util.Constants;
import com.denisfesenko.util.ConverterUtils;
import com.denisfesenko.util.RunUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.Color;
import org.docx4j.wml.HpsMeasure;
import org.docx4j.wml.ObjectFactory;
import org.docx4j.wml.RPr;
import org.jsoup.nodes.Node;

/**
 * Handles the font-related properties of an HTML node and updates the corresponding
 * properties in a WordprocessingMLPackage instance.
 */
public class FontHandler implements TagHandler {

    /**
     * Handles the font-related properties of the given HTML node and updates the
     * corresponding properties in the provided WordprocessingMLPackage instance.
     *
     * @param node          The HTML node to handle.
     * @param wordMLPackage The WordprocessingMLPackage instance to update.
     */
    @Override
    public void handleTag(Node node, WordprocessingMLPackage wordMLPackage) {
        Node fontNode = ConverterUtils.findParentNode(node, "font");
        ObjectFactory objectFactory = RunUtils.getObjectFactory();
        RPr rPr = RunUtils.getCurrentRPr(wordMLPackage);

        String nodeColor = fontNode.attr("color");
        if (StringUtils.isNotBlank(nodeColor)) {
            String fontColor = ConverterUtils.isHexColor(nodeColor)
                    ? nodeColor : ConverterUtils.rgbToHex(nodeColor, Constants.HEX_BLACK_COLOR);
            Color color = objectFactory.createColor();
            color.setVal(fontColor);
            rPr.setColor(color);
        }

        String nodeSize = StringUtils.substringBefore(fontNode.attr("size"), "px");
        if (NumberUtils.isCreatable(nodeSize)) {
            int fontSize = Integer.parseInt(nodeSize);
            HpsMeasure hpsMeasure = objectFactory.createHpsMeasure();
            hpsMeasure.setVal(ConverterUtils.pxToHalfPoints(fontSize));
            rPr.setSz(hpsMeasure);
            rPr.setSzCs(hpsMeasure);
        }
    }

    /**
     * Indicates whether the FontHandler can be applied multiple times to the same node.
     *
     * @return true if the FontHandler can be applied multiple times; false otherwise.
     */
    @Override
    public boolean isRepeatable() {
        return true;
    }
}
