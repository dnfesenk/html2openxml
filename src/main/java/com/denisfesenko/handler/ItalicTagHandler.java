package com.denisfesenko.handler;

import com.denisfesenko.util.Formatting;
import com.denisfesenko.util.RunUtils;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.BooleanDefaultTrue;
import org.docx4j.wml.R;
import org.docx4j.wml.RPr;
import org.jsoup.nodes.Node;

public class ItalicTagHandler implements HtmlTagHandler {

    @Override
    public void handleOpenTag(Node node, WordprocessingMLPackage wordMLPackage, Formatting formatting) {
        formatting.setItalic(true);
    }

    @Override
    public void handleCloseTag(Node node, WordprocessingMLPackage wordMLPackage, Formatting formatting) {
        formatting.setItalic(false);
    }

    @Override
    public void handleTextNode(Node node, WordprocessingMLPackage wordMLPackage, Formatting formatting) {
        if (node instanceof org.jsoup.nodes.TextNode) {
            R run = RunUtils.createRun(((org.jsoup.nodes.TextNode) node).text());
            if (formatting.isItalic()) {
                formatRun(run);
            }
            RunUtils.addRunToParagraph(run, wordMLPackage);
        }
    }

    private void formatRun(R run) {
        RPr rpr = run.getRPr() != null ? run.getRPr() : new RPr();
        BooleanDefaultTrue b = new BooleanDefaultTrue();
        rpr.setI(b);
        run.setRPr(rpr);
    }
}
