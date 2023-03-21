package com.denisfesenko.converter;

import com.denisfesenko.util.Formatting;
import com.denisfesenko.handler.BoldTagHandler;
import com.denisfesenko.handler.HtmlTagHandler;
import com.denisfesenko.handler.ItalicTagHandler;
import com.denisfesenko.handler.ParagraphTagHandler;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeVisitor;

import java.util.HashMap;
import java.util.Map;

public class HtmlToOpenXMLConverter {

    private final Map<String, HtmlTagHandler> tagHandlers;

    public HtmlToOpenXMLConverter() {
        tagHandlers = new HashMap<>();
        tagHandlers.put("p", new ParagraphTagHandler());
        tagHandlers.put("b", new BoldTagHandler());
        tagHandlers.put("strong", new BoldTagHandler());
        tagHandlers.put("i", new ItalicTagHandler());
        tagHandlers.put("em", new ItalicTagHandler());
    }

    public WordprocessingMLPackage convert(String html) throws Exception {
        WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.createPackage();
        Document document = Jsoup.parseBodyFragment(html);

        Formatting formatting = new Formatting();
        document.traverse(new NodeVisitor() {
            @Override
            public void head(Node node, int depth) {
                HtmlTagHandler tagHandler = tagHandlers.get(node.nodeName());
                if (tagHandler != null) {
                    tagHandler.handleOpenTag(node, wordMLPackage, formatting);
                }
            }

            @Override
            public void tail(Node node, int depth) {
                HtmlTagHandler tagHandler = tagHandlers.get(node.nodeName());
                if (tagHandler != null) {
                    tagHandler.handleCloseTag(node, wordMLPackage, formatting);
                } else if (node instanceof org.jsoup.nodes.TextNode) {
                    HtmlTagHandler parentTagHandler = tagHandlers.get(node.parent().nodeName());
                    if (parentTagHandler != null) {
                        parentTagHandler.handleTextNode(node, wordMLPackage, formatting);
                    }
                }
            }
        });

        return wordMLPackage;
    }
}
