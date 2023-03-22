package com.denisfesenko.converter;

import com.denisfesenko.core.TagHandler;
import com.denisfesenko.core.TagHandlerFactory;
import com.denisfesenko.handler.TextTagHandler;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeVisitor;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

/**
 * A converter that converts HTML content to OpenXML format.
 */
public class HtmlToOpenXMLConverter {

    private final Map<String, TagHandler> tagHandlerMap;
    private final Deque<TagHandler> tagHandlers;

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
        tagHandlers = new ArrayDeque<>();
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
                    tagHandlers.push(tagHandler);
                }
            }

            @Override
            public void tail(Node node, int depth) {
                if (!tagHandlers.isEmpty()) {
                    TagHandler tagHandler = tagHandlers.pop();
                    handleTagAndClearTextTagHandlers(node, wordMLPackage, tagHandler);
                    tagHandler.handleTag(node, wordMLPackage);
                }
            }
        });
    }

    /**
     * Handles the current HTML node and clears the tag handlers if the current tag handler is an instance of TextTagHandler.
     *
     * @param node          the HTML node to be processed
     * @param wordMLPackage the WordprocessingMLPackage that will store the converted content
     * @param tagHandler    the current tag handler
     */
    private void handleTagAndClearTextTagHandlers(Node node, WordprocessingMLPackage wordMLPackage, TagHandler tagHandler) {
        if (tagHandler instanceof TextTagHandler) {
            while (!tagHandlers.isEmpty()) {
                tagHandlers.pop().handleTag(node, wordMLPackage);
            }
        }
    }
}
