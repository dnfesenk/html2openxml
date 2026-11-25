package com.denisfesenko.handler;

import com.denisfesenko.util.RunUtils;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.P;
import org.docx4j.wml.PPrBase;
import org.jsoup.nodes.Node;

public final class HeadingHandler extends ParagraphHandler {
    public HeadingHandler(boolean strictValidation) {
        super(strictValidation);
    }

    @Override
    public void handleTag(Node node, WordprocessingMLPackage wordMLPackage) {
        char level = node.nodeName().charAt(1);
        String defaultStyleId = "Heading" + level;
        var styles = RunUtils.getOrCreateStyles(wordMLPackage);
        String styleId = RunUtils.getOrCreateStyleId(styles, "paragraph", "heading " + level, defaultStyleId, style -> {
            style.getPPr().setNumPr(null);
            style.setLink(null);
        });

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
