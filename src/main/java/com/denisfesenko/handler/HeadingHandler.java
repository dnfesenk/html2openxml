package com.denisfesenko.handler;

import com.denisfesenko.util.RunUtils;
import org.docx4j.XmlUtils;
import org.docx4j.openpackaging.exceptions.InvalidFormatException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.StyleDefinitionsPart;
import org.docx4j.wml.P;
import org.docx4j.wml.PPrBase;
import org.jsoup.nodes.Node;

public final class HeadingHandler extends ParagraphHandler {
    public HeadingHandler(boolean strictValidation) {
        super(strictValidation);
    }

    @Override
    public void handleTag(Node node, WordprocessingMLPackage wordMLPackage) {
        String styleId = "Heading" + node.nodeName().charAt(1);
        StyleDefinitionsPart styles = wordMLPackage.getMainDocumentPart().getStyleDefinitionsPart();
        if (styles == null) {
            try {
                styles = new StyleDefinitionsPart();
                styles.setJaxbElement(RunUtils.getObjectFactory().createStyles());
                wordMLPackage.getMainDocumentPart().addTargetPart(styles);
            } catch (InvalidFormatException e) {
                throw new IllegalStateException("Unable to create Word styles part", e);
            }
        }
        if (styles.getStyleById(styleId) == null) {
            var style = XmlUtils.deepCopy(StyleDefinitionsPart.getKnownStyles().get(styleId));
            style.getPPr().setNumPr(null);
            style.setLink(null);
            styles.getJaxbElement().getStyle().add(style);
        }

        super.handleTag(node, wordMLPackage);
        P paragraph = RunUtils.getCurrentParagraph(wordMLPackage);
        if (paragraph.getPPr() == null) {
            paragraph.setPPr(RunUtils.getObjectFactory().createPPr());
        }
        PPrBase.PStyle style = RunUtils.getObjectFactory().createPPrBasePStyle();
        style.setVal(styleId);
        paragraph.getPPr().setPStyle(style);
    }
}
