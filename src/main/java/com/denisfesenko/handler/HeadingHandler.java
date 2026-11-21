package com.denisfesenko.handler;

import com.denisfesenko.util.RunUtils;
import org.docx4j.XmlUtils;
import org.docx4j.openpackaging.exceptions.InvalidFormatException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.StyleDefinitionsPart;
import org.docx4j.wml.P;
import org.docx4j.wml.PPrBase;
import org.docx4j.wml.Style;
import org.jsoup.nodes.Node;

public final class HeadingHandler extends ParagraphHandler {
    public HeadingHandler(boolean strictValidation) {
        super(strictValidation);
    }

    @Override
    public void handleTag(Node node, WordprocessingMLPackage wordMLPackage) {
        char level = node.nodeName().charAt(1);
        String defaultStyleId = "Heading" + level;
        StyleDefinitionsPart styles = wordMLPackage.getMainDocumentPart().getStyleDefinitionsPart();
        if (styles == null) {
            try {
                styles = new StyleDefinitionsPart();
                var definitions = RunUtils.getObjectFactory().createStyles();
                definitions.getStyle().add(XmlUtils.deepCopy(StyleDefinitionsPart.getKnownStyles().get("Normal")));
                styles.setJaxbElement(definitions);
                wordMLPackage.getMainDocumentPart().addTargetPart(styles);
            } catch (InvalidFormatException e) {
                throw new IllegalStateException("Unable to create Word styles part", e);
            }
        }
        String headingName = "heading " + level;
        String styleId = styles.getJaxbElement().getStyle().stream()
                .filter(style -> "paragraph".equals(style.getType()) && style.getName() != null)
                .filter(style -> headingName.equalsIgnoreCase(style.getName().getVal()))
                .map(Style::getStyleId)
                .filter(id -> id != null && !id.isBlank())
                .findFirst().orElse(defaultStyleId);
        if (styles.getStyleById(styleId) == null) {
            var style = XmlUtils.deepCopy(StyleDefinitionsPart.getKnownStyles().get(defaultStyleId));
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
