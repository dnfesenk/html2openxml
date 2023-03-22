package com.denisfesenko.core;

import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.jsoup.nodes.Node;

public interface TagHandler {
    void handleTag(Node node, WordprocessingMLPackage wordMLPackage);
}
