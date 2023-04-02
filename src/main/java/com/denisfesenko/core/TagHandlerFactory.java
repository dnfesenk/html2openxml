package com.denisfesenko.core;

import com.denisfesenko.handler.BoldHandler;
import com.denisfesenko.handler.BreakHandler;
import com.denisfesenko.handler.FontHandler;
import com.denisfesenko.handler.HrHandler;
import com.denisfesenko.handler.ItalicHandler;
import com.denisfesenko.handler.PageBreakHandler;
import com.denisfesenko.handler.ParagraphHandler;
import com.denisfesenko.handler.PlainTextHandler;
import com.denisfesenko.handler.SpanHandler;
import com.denisfesenko.handler.SubHandler;
import com.denisfesenko.handler.SupHandler;
import com.denisfesenko.handler.TableHandler;
import com.denisfesenko.handler.UnderlineHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * The TagHandlerFactory class is responsible for creating a map
 * of supported HTML tags and their corresponding handlers.
 * Each handler is responsible for processing a specific HTML tag.
 */
public class TagHandlerFactory {

    /**
     * Creates and returns a map of supported HTML tags and their corresponding handlers.
     * The keys in the map are tag names, and the values are instances of the appropriate
     * TagHandler subclasses for processing the tags.
     *
     * @return A map of supported HTML tags and their corresponding handlers.
     */
    public Map<String, TagHandler> createTagHandlerMap() {
        Map<String, TagHandler> tagHandlerMap = new HashMap<>();
        tagHandlerMap.put("font", new FontHandler());
        tagHandlerMap.put("table", new TableHandler());
        tagHandlerMap.put("sub", new SubHandler());
        tagHandlerMap.put("sup", new SupHandler());
        tagHandlerMap.put("hr", new HrHandler());
        tagHandlerMap.put("u", new UnderlineHandler());
        tagHandlerMap.put("span", new SpanHandler());
        tagHandlerMap.put("br", new BreakHandler());
        tagHandlerMap.put("pb", new PageBreakHandler());
        tagHandlerMap.put("p", new ParagraphHandler());
        tagHandlerMap.put("b", new BoldHandler());
        tagHandlerMap.put("strong", new BoldHandler());
        tagHandlerMap.put("i", new ItalicHandler());
        tagHandlerMap.put("em", new ItalicHandler());
        tagHandlerMap.put("#text", new PlainTextHandler());
        return tagHandlerMap;
    }
}
