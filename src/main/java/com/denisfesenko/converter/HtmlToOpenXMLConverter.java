package com.denisfesenko.converter;

import com.denisfesenko.core.TagHandler;
import com.denisfesenko.core.TagHandlerFactory;
import com.denisfesenko.handler.PlainTextHandler;
import com.denisfesenko.handler.TableHandler;
import com.denisfesenko.util.RunUtils;
import org.docx4j.openpackaging.exceptions.InvalidFormatException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeFilter;
import org.jsoup.select.NodeTraversor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * A converter that converts HTML content to OpenXML format.
 *
 * @author Denis Fesenko &lt;fesenkoden@gmail.com&gt;
 */
public class HtmlToOpenXMLConverter {

    private final Map<String, TagHandler> tagHandlerMap;

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
    }

    /**
     * Converts the provided HTML content to OpenXML format and returns a WordprocessingMLPackage.
     *
     * @param html the HTML content to be converted
     * @return a WordprocessingMLPackage containing the converted content
     * @throws InvalidFormatException if there is an issue during the conversion process
     */
    public WordprocessingMLPackage convert(String html) throws InvalidFormatException {
        WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.createPackage();
        return convert(html, wordMLPackage);
    }

    /**
     * Converts the given HTML content into a WordprocessingMLPackage.
     *
     * @param html           The input HTML content as a string.
     * @param wordMLPackage  The WordprocessingMLPackage instance to populate with the converted content.
     * @return The modified WordprocessingMLPackage containing the converted content from the input HTML.
     */
    public WordprocessingMLPackage convert(String html, WordprocessingMLPackage wordMLPackage) {
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
        List<Node> activeTags = new ArrayList<>();
        NodeTraversor.filter(new NodeFilter() {
            private boolean paragraphEnded;
            @Override
            public FilterResult head(Node node, int depth) {
                TagHandler tagHandler = tagHandlerMap.get(node.nodeName());
                if (tagHandler != null) {
                    if (paragraphEnded && node instanceof TextNode && ((TextNode) node).isBlank()) {
                        return FilterResult.CONTINUE;
                    }
                    if (paragraphEnded && !(tagHandler instanceof TableHandler) && !node.nodeName().equals("p")
                            && !tagHandler.isRepeatable()) {
                        RunUtils.createParagraph(wordMLPackage, null);
                        paragraphEnded = false;
                    }
                    if (node.nodeName().equals("p")) paragraphEnded = false;
                    if (tagHandler instanceof PlainTextHandler) {
                        RunUtils.getCurrentParagraph(wordMLPackage).getContent().add(RunUtils.getObjectFactory().createR());
                        activeTags.forEach(source -> tagHandlerMap.get(source.nodeName()).handleTag(node, source, wordMLPackage));
                        tagHandler.handleTag(node, wordMLPackage);
                    } else if (tagHandler instanceof TableHandler) {
                        ((TableHandler) tagHandler).addConverter(HtmlToOpenXMLConverter.this).handleTag(node, wordMLPackage);
                        return FilterResult.SKIP_CHILDREN;
                    } else if (tagHandler.isRepeatable()) {
                        activeTags.add(node);
                    } else {
                        tagHandler.handleTag(node, wordMLPackage);
                    }
                }
                return FilterResult.CONTINUE;
            }

            @Override
            public FilterResult tail(Node node, int depth) {
                activeTags.remove(node);
                if (node.nodeName().equals("p")) paragraphEnded = true;
                return FilterResult.CONTINUE;
            }
        }, document);
    }
}
