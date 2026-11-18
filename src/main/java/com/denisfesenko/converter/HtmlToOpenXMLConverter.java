package com.denisfesenko.converter;

import com.denisfesenko.core.TagHandler;
import com.denisfesenko.core.TagHandlerFactory;
import com.denisfesenko.handler.PlainTextHandler;
import com.denisfesenko.handler.ParagraphHandler;
import com.denisfesenko.handler.BoldHandler;
import com.denisfesenko.handler.ImageHandler;
import com.denisfesenko.handler.HeadingHandler;
import com.denisfesenko.handler.BreakHandler;
import com.denisfesenko.handler.ItalicHandler;
import com.denisfesenko.handler.FontHandler;
import com.denisfesenko.handler.SpanHandler;
import com.denisfesenko.handler.UnderlineHandler;
import com.denisfesenko.handler.SubHandler;
import com.denisfesenko.handler.SupHandler;
import com.denisfesenko.handler.TableHandler;
import com.denisfesenko.util.RunUtils;
import org.docx4j.openpackaging.exceptions.InvalidFormatException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.P;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeFilter;
import org.jsoup.select.NodeTraversor;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;


/**
 * A converter that converts HTML content to OpenXML format.
 *
 * @author Denis Fesenko &lt;fesenkoden@gmail.com&gt;
 */
public class HtmlToOpenXMLConverter {

    private static final Set<Class<?>> BUILT_IN_FORMATTING = Set.of(BoldHandler.class, ItalicHandler.class,
            FontHandler.class, SpanHandler.class, UnderlineHandler.class, SubHandler.class, SupHandler.class);

    private final Map<String, TagHandler> tagHandlerMap;

    /**
     * Initializes default tag handlers with lenient validation of alignment and merge values.
     */
    public HtmlToOpenXMLConverter() {
        this(null, false);
    }

    /**
     * Constructor with custom tag handlers. Initializes the converter with the provided custom tag handlers.
     *
     * @param customTagHandlerMap a map of custom tag handlers to be added to the default tag handlers
     */
    public HtmlToOpenXMLConverter(Map<String, TagHandler> customTagHandlerMap) {
        this(customTagHandlerMap, false);
    }

    /**
     * Initializes default handlers with optional strict validation.
     *
     * @param strictValidation true to reject unknown paragraph align and cell merge values;
     *                         false to ignore these values and preserve document content
     */
    public HtmlToOpenXMLConverter(boolean strictValidation) {
        this(null, strictValidation);
    }

    /**
     * Initializes built-in handlers with optional strict validation and adds custom handlers.
     * The validation option applies to built-in paragraph and table handlers. Custom handlers
     * retain responsibility for their own validation and exceptions.
     * Image validation and resource limits are always enforced in either mode.
     *
     * @param customTagHandlerMap handlers that override or extend the defaults, or null
     * @param strictValidation whether unknown paragraph align and cell merge values cause errors
     */
    public HtmlToOpenXMLConverter(Map<String, TagHandler> customTagHandlerMap, boolean strictValidation) {
        tagHandlerMap = new TagHandlerFactory().createTagHandlerMap(strictValidation);
        if (customTagHandlerMap != null) {
            if (customTagHandlerMap.keySet().stream().anyMatch(TableHandler.INHERITED_STYLE_TAG::equalsIgnoreCase)) {
                throw new IllegalArgumentException("Tag " + TableHandler.INHERITED_STYLE_TAG + " is reserved for inherited cell styles");
            }
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

    private static P lastParagraph(List<Object> content) {
        if (content == null || content.isEmpty()) return null;
        Object last = content.get(content.size() - 1);
        return last instanceof P ? (P) last : null;
    }

    /**
     * Traverses the HTML document and processes each node with the appropriate tag handlers.
     *
     * @param document      the HTML document to be traversed
     * @param wordMLPackage the WordprocessingMLPackage that will store the converted content
     */
    private void traverseDocument(Document document, WordprocessingMLPackage wordMLPackage) {
        List<Node> activeTags = new ArrayList<>();
        ListNumbering listNumbering = new ListNumbering();
        NodeTraversor.filter(new NodeFilter() {
            private boolean paragraphEnded;
            private Node lastInlineBlock;
            private P pendingListParagraph;
            private Node pendingListItem;

            private boolean isBuiltInListItem(Node node) {
                return "li".equals(node.nodeName()) && ListNumbering.isList(node.parentNode())
                        && !tagHandlerMap.containsKey("li") && !tagHandlerMap.containsKey(node.parentNode().nodeName());
            }

            private void indentListContinuation(Node node, P paragraph) {
                for (Node ancestor = node.parentNode(); ancestor != null; ancestor = ancestor.parentNode()) {
                    if ("li".equals(ancestor.nodeName())) {
                        if (isBuiltInListItem(ancestor)) {
                            ListNumbering.indentContinuation(ancestor, paragraph);
                        }
                        return;
                    }
                }
            }

            private void handleImage(Node node, TagHandler handler) {
                List<Object> content = wordMLPackage.getMainDocumentPart() == null ? null
                        : wordMLPackage.getMainDocumentPart().getContent();
                int previousDocumentSize = content == null ? 0 : content.size();
                P previousParagraph = lastParagraph(content);
                int previousParagraphSize = previousParagraph == null ? 0 : previousParagraph.getContent().size();
                Node block = InlineCss.closestBlock(node);
                boolean newBlock = lastInlineBlock != null && block != lastInlineBlock
                        && (block == null || !block.nodeName().equals("p"));
                if (paragraphEnded || (newBlock && previousParagraphSize > 0)) {
                    RunUtils.createParagraph(wordMLPackage, null);
                }
                // Keep validation/embedding exceptions unchanged. Only commit paragraph
                // state and alignment if the handler actually produced image or alt content.
                handler.handleTag(node, wordMLPackage);
                content = wordMLPackage.getMainDocumentPart().getContent();
                P paragraph = lastParagraph(content);
                boolean added = paragraph != null && (paragraph == previousParagraph
                        ? paragraph.getContent().size() > previousParagraphSize : !paragraph.getContent().isEmpty());
                if (added) {
                    indentListContinuation(node, paragraph);
                    InlineCss.alignImplicitParagraph(node, paragraph);
                    lastInlineBlock = block;
                    paragraphEnded = false;
                } else if (content.size() > previousDocumentSize && paragraph != null && paragraph.getContent().isEmpty()) {
                    // Discard the speculative block boundary for a source without an alt fallback.
                    content.remove(content.size() - 1);
                }
            }

            @Override
            public FilterResult head(Node node, int depth) {
                TagHandler tagHandler = tagHandlerMap.get(node.nodeName());
                if (isBuiltInListItem(node)) {
                    var properties = RunUtils.getObjectFactory().createPPr();
                    listNumbering.numberItem(node, properties, wordMLPackage);
                    RunUtils.createParagraph(wordMLPackage, properties);
                    pendingListParagraph = RunUtils.getCurrentParagraph(wordMLPackage);
                    pendingListItem = node;
                    paragraphEnded = false;
                    lastInlineBlock = node;
                }
                if (tagHandler != null) {
                    if (node instanceof TextNode && ((TextNode) node).isBlank()
                            && (ListNumbering.isList(node.parentNode())
                                || (pendingListItem != null && node.parentNode() == pendingListItem && pendingListParagraph.getContent().isEmpty()))) {
                        return FilterResult.CONTINUE;
                    }
                    if (pendingListItem != null && "p".equals(node.nodeName()) && node.parentNode() == pendingListItem
                            && pendingListParagraph.getContent().isEmpty()
                            && lastParagraph(wordMLPackage.getMainDocumentPart().getContent()) == pendingListParagraph
                            && tagHandler.getClass() == ParagraphHandler.class) {
                        var content = wordMLPackage.getMainDocumentPart().getContent();
                        tagHandler.handleTag(node, wordMLPackage);
                        var paragraph = lastParagraph(content);
                        if (paragraph.getPPr() == null) paragraph.setPPr(RunUtils.getObjectFactory().createPPr());
                        paragraph.getPPr().setNumPr(pendingListParagraph.getPPr().getNumPr());
                        content.remove(pendingListParagraph);
                        pendingListItem = null;
                        pendingListParagraph = null;
                        paragraphEnded = false;
                        lastInlineBlock = node;
                        return FilterResult.CONTINUE;
                    }
                    if (paragraphEnded && node instanceof TextNode && ((TextNode) node).isBlank()) {
                        return FilterResult.CONTINUE;
                    }
                    if (tagHandler instanceof ImageHandler && !tagHandler.isRepeatable()) {
                        handleImage(node, tagHandler);
                        return FilterResult.CONTINUE;
                    }
                    if (tagHandler instanceof HeadingHandler) {
                        tagHandler.handleTag(node, wordMLPackage);
                        paragraphEnded = false;
                        lastInlineBlock = node;
                        return FilterResult.CONTINUE;
                    }
                    if (paragraphEnded && !(tagHandler instanceof TableHandler) && !node.nodeName().equals("p")
                            && !tagHandler.isRepeatable()) {
                        RunUtils.createParagraph(wordMLPackage, null);
                        paragraphEnded = false;
                    }
                    if (node.nodeName().equals("p")) paragraphEnded = false;
                    if (tagHandler instanceof PlainTextHandler || tagHandler instanceof BreakHandler) {
                        Node block = InlineCss.closestBlock(node);
                        var paragraph = RunUtils.getCurrentParagraph(wordMLPackage);
                        if (block != lastInlineBlock && lastInlineBlock != null && !paragraph.getContent().isEmpty()
                                && (block == null || !block.nodeName().equals("p"))) {
                            if (node instanceof TextNode && ((TextNode) node).isBlank()) return FilterResult.CONTINUE;
                            RunUtils.createParagraph(wordMLPackage, null);
                            paragraph = RunUtils.getCurrentParagraph(wordMLPackage);
                        }
                        indentListContinuation(node, paragraph);
                        InlineCss.alignImplicitParagraph(node, paragraph);
                        lastInlineBlock = block;
                    }
                    if (tagHandler instanceof PlainTextHandler) {
                        RunUtils.getCurrentParagraph(wordMLPackage).getContent().add(RunUtils.getObjectFactory().createR());
                        Map<TagHandler, List<Node>> contexts = new LinkedHashMap<>();
                        activeTags.forEach(source -> contexts.computeIfAbsent(tagHandlerMap.get(source.nodeName()),
                                handler -> new ArrayList<>()).add(source));
                        // Apply semantic tags and CSS in DOM order so inner elements can override
                        // inherited formatting. Legacy custom handlers still run once per text node.
                        List<Node> ancestors = new ArrayList<>();
                        for (Node source = node.parentNode(); source != null; source = source.parentNode()) {
                            ancestors.add(source);
                        }
                        Collections.reverse(ancestors);
                        Set<TagHandler> invokedCustomHandlers = new HashSet<>();
                        for (Node source : ancestors) {
                            TagHandler handler = tagHandlerMap.get(source.nodeName());
                            if (handler != null && contexts.containsKey(handler)) {
                                if (BUILT_IN_FORMATTING.contains(handler.getClass())) {
                                    handler.handleTag(node, List.of(source), wordMLPackage);
                                } else if (invokedCustomHandlers.add(handler)) {
                                    handler.handleTag(node, contexts.get(handler), wordMLPackage);
                                }
                            }
                            InlineCss.apply(source, wordMLPackage);
                        }
                        tagHandler.handleTag(node, wordMLPackage);
                    } else if (tagHandler instanceof TableHandler) {
                        ((TableHandler) tagHandler).handleTag(node, wordMLPackage, HtmlToOpenXMLConverter.this);
                        return FilterResult.SKIP_CHILDREN;
                    } else if (tagHandler.isRepeatable()) {
                        activeTags.add(node);
                    } else {
                        var content = wordMLPackage.getMainDocumentPart().getContent();
                        P previous = lastParagraph(content);
                        tagHandler.handleTag(node, wordMLPackage);
                        P paragraph = lastParagraph(content);
                        if (paragraph != null && paragraph != previous) {
                            indentListContinuation(node, paragraph);
                        }
                    }
                }
                return FilterResult.CONTINUE;
            }

            @Override
            public FilterResult tail(Node node, int depth) {
                if (tagHandlerMap.get(node.nodeName()) instanceof HeadingHandler) paragraphEnded = true;
                activeTags.remove(node);
                if (isBuiltInListItem(node)) {
                    paragraphEnded = true;
                    if (node == pendingListItem) {
                        pendingListItem = null;
                        pendingListParagraph = null;
                    }
                }
                if (node.nodeName().equals("p") || node == lastInlineBlock) paragraphEnded = true;
                return FilterResult.CONTINUE;
            }
        }, document);
    }
}
