package com.denisfesenko.converter;

import com.denisfesenko.core.TagHandler;
import com.denisfesenko.core.TagHandlerFactory;
import com.denisfesenko.handler.TextTagHandler;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeVisitor;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * A converter that converts HTML content to OpenXML format.
 */
public class HtmlToOpenXMLConverter {

    private final Map<String, TagHandler> tagHandlerMap;
    private final Set<TagHandler> tagHandlers;

    /**
     * Default constructor. Initializes the converter with default tag handlers.
     */
    public HtmlToOpenXMLConverter() {
        this(null);
    }

    /**
     * Constructor with custom tag handlers. Initializes the converter with the provided custom tag handlers.
     *
     * @param customTagHandlerMap a map of custom tag handlers to be added to the default tag handlers
     */
    public HtmlToOpenXMLConverter(Map<String, TagHandler> customTagHandlerMap) {
        tagHandlerMap = new TagHandlerFactory().createTagHandlerMap();
        if (customTagHandlerMap != null) {
            tagHandlerMap.putAll(customTagHandlerMap);
        }
        tagHandlers = new LinkedHashSet<>();
    }

    /**
     * Converts the provided HTML content to OpenXML format and returns a WordprocessingMLPackage.
     *
     * @param html the HTML content to be converted
     * @return a WordprocessingMLPackage containing the converted content
     * @throws Exception if there is an issue during the conversion process
     */
    public WordprocessingMLPackage convert(String html) throws Exception {
        WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.createPackage();
        Document document = Jsoup.parseBodyFragment(html);
        traverseDocument(document, wordMLPackage);
        return wordMLPackage;
    }

    /**
     * Traverses the HTML document and processes each node with the appropriate tag handlers.
     *
     * @param document      the HTML document to be traversed
     * @param wordMLPackage the WordprocessingMLPackage that will store the converted content
     */
    private void traverseDocument(Document document, WordprocessingMLPackage wordMLPackage) {
        document.traverse(new NodeVisitor() {
            @Override
            public void head(Node node, int depth) {
                TagHandler tagHandler = tagHandlerMap.get(node.nodeName());
                if (tagHandler != null) {
                    if (tagHandler instanceof TextTagHandler) {
                        tagHandlers.forEach(handler -> handler.handleTag(node, wordMLPackage));
                        tagHandler.handleTag(node, wordMLPackage);
                    } else if (tagHandler.isRepeatable()) {
                        tagHandlers.add(tagHandler);
                    } else {
                        tagHandler.handleTag(node, wordMLPackage);
                    }
                }
            }

            @Override
            public void tail(Node node, int depth) {
                TagHandler tagHandler = tagHandlerMap.get(node.nodeName());
                if (!tagHandlers.isEmpty()) {
                    tagHandlers.remove(tagHandler);
                }
            }
        });
    }

}
