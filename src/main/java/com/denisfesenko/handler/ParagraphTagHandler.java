package com.denisfesenko.handler;

import com.denisfesenko.util.Formatting;
import com.denisfesenko.util.RunUtils;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.HpsMeasure;
import org.docx4j.wml.P;
import org.docx4j.wml.PPr;
import org.docx4j.wml.R;
import org.docx4j.wml.RPr;
import org.jsoup.nodes.Node;

import java.math.BigInteger;
import java.util.List;

public class ParagraphTagHandler implements HtmlTagHandler {

    @Override
    public void handleOpenTag(Node node, WordprocessingMLPackage wordMLPackage, Formatting formatting) {
        RunUtils.createParagraph(wordMLPackage);
    }

    @Override
    public void handleCloseTag(Node node, WordprocessingMLPackage wordMLPackage, Formatting formatting) {
        // Do nothing
    }

    @Override
    public void handleTextNode(Node node, WordprocessingMLPackage wordMLPackage, Formatting formatting) {
        if (node instanceof org.jsoup.nodes.TextNode) {
            R run = RunUtils.createRun(((org.jsoup.nodes.TextNode) node).text());
            formatRun(run, wordMLPackage);
            RunUtils.addRunToParagraph(run, wordMLPackage);
        }
    }

    private void formatRun(R run, WordprocessingMLPackage wordMLPackage) {
        P currentParagraph = getCurrentParagraph(wordMLPackage);
        if (currentParagraph != null) {
            PPr ppr = currentParagraph.getPPr();
            if (ppr != null && ppr.getPStyle() != null && "Heading1".equals(ppr.getPStyle().getVal())) {
                applyHeading1(run);
            }
        }
    }

    private P getCurrentParagraph(WordprocessingMLPackage wordMLPackage) {
        List<Object> content = wordMLPackage.getMainDocumentPart().getContent();
        for (int i = content.size() - 1; i >= 0; i--) {
            Object obj = content.get(i);
            if (obj instanceof P) {
                return (P) obj;
            }
        }
        return null;
    }

    private void applyHeading1(R run) {
        RPr rpr = run.getRPr() != null ? run.getRPr() : new RPr();
        HpsMeasure size = new HpsMeasure();
        size.setVal(BigInteger.valueOf(24 * 2));
        rpr.setSz(size);
        rpr.setSzCs(size);
        run.setRPr(rpr);
    }
}
