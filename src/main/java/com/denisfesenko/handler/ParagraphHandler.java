package com.denisfesenko.handler;

import com.denisfesenko.core.TagHandler;
import com.denisfesenko.util.Constants;
import com.denisfesenko.util.ConverterUtils;
import com.denisfesenko.util.JcEnumMapper;
import com.denisfesenko.util.RunUtils;
import org.apache.commons.lang3.StringUtils;
import java.math.BigInteger;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.Jc;
import org.docx4j.wml.ObjectFactory;
import org.docx4j.wml.PPr;
import org.docx4j.wml.PPrBase;
import org.jsoup.nodes.Node;

/**
 * This class handles the conversion of HTML paragraphs to WordprocessingMLPackage format.
 */
public class ParagraphHandler implements TagHandler {

    /**
     * Converts the paragraph node to a WordprocessingMLPackage instance.
     *
     * @param node          The paragraph node to be converted.
     * @param wordMLPackage The WordprocessingMLPackage instance to which the converted paragraph will be added.
     */
    @Override
    public void handleTag(Node node, WordprocessingMLPackage wordMLPackage) {
        String style = StringUtils.isNotBlank(node.attr(Constants.STYLE)) ? node.attr(Constants.STYLE) : null;
        String indent = ConverterUtils.getStyleMeasurement(style, "text-indent", "px");
        String align = StringUtils.isNotBlank(node.attr("align")) ? node.attr("align") : null;
        PPr pPr = null;
        if (align != null || indent != null) {
            ObjectFactory objectFactory = RunUtils.getObjectFactory();
            pPr = objectFactory.createPPr();
            if (align != null) {
                Jc jc = objectFactory.createJc();
                jc.setVal(JcEnumMapper.map(align));
                pPr.setJc(jc);
            }
            BigInteger indentDxa = ConverterUtils.scaleDecimal(indent, "15");
            if (indentDxa != null) {
                PPrBase.Ind ind = objectFactory.createPPrBaseInd();
                if (indentDxa.signum() < 0) {
                    ind.setHanging(indentDxa.negate());
                } else {
                    ind.setFirstLine(indentDxa);
                }
                pPr.setInd(ind);
            }
        }
        RunUtils.createParagraph(wordMLPackage, pPr);
    }

    /**
     * Returns whether the tag handler is repeatable.
     *
     * @return false, because the ParagraphHandler is not repeatable.
     */
    @Override
    public boolean isRepeatable() {
        return false;
    }
}
