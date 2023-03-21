package com.denisfesenko.handler;

import com.denisfesenko.util.Formatting;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.jsoup.nodes.Node;

public interface HtmlTagHandler {
    void handleOpenTag(Node node, WordprocessingMLPackage wordMLPackage, Formatting formatting);

    void handleCloseTag(Node node, WordprocessingMLPackage wordMLPackage, Formatting formatting);

    void handleTextNode(Node node, WordprocessingMLPackage wordMLPackage, Formatting formatting);
}
