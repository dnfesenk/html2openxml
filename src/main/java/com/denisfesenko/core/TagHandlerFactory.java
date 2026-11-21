package com.denisfesenko.core;

import com.denisfesenko.handler.BoldHandler;
import com.denisfesenko.handler.ImageHandler;
import com.denisfesenko.handler.BreakHandler;
import com.denisfesenko.handler.FontHandler;
import com.denisfesenko.handler.HrHandler;
import com.denisfesenko.handler.HeadingHandler;
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
     * TagHandler subclasses for processing the tags. Unknown alignment and merge values
     * are ignored; use {@link #createTagHandlerMap(boolean)} with true for strict validation.
     *
     * @return A map of supported HTML tags and their corresponding handlers.
     */
    public Map<String, TagHandler> createTagHandlerMap() {
        return createTagHandlerMap(false);
    }

    /**
     * Creates built-in handlers with the chosen attribute validation policy.
     *
     * @param strictValidation whether unknown paragraph align and cell merge values cause errors
     * @return default tag handlers with the requested validation policy
     */
    public Map<String, TagHandler> createTagHandlerMap(boolean strictValidation) {
        Map<String, TagHandler> tagHandlerMap = new HashMap<>();
        tagHandlerMap.put("img", new ImageHandler());
        tagHandlerMap.put("font", new FontHandler());
        tagHandlerMap.put("table", new TableHandler(strictValidation));
        tagHandlerMap.put("sub", new SubHandler());
        tagHandlerMap.put("sup", new SupHandler());
        tagHandlerMap.put("hr", new HrHandler());
        tagHandlerMap.put("u", new UnderlineHandler());
        tagHandlerMap.put("span", new SpanHandler());
        tagHandlerMap.put("br", new BreakHandler());
        tagHandlerMap.put("pb", new PageBreakHandler());
        HeadingHandler headingHandler = new HeadingHandler(strictValidation);
        for (int level = 1; level <= 6; level++) {
            tagHandlerMap.put("h" + level, headingHandler);
        }
        tagHandlerMap.put("p", new ParagraphHandler(strictValidation));
        tagHandlerMap.put("b", new BoldHandler());
        tagHandlerMap.put("strong", new BoldHandler());
        tagHandlerMap.put("i", new ItalicHandler());
        tagHandlerMap.put("em", new ItalicHandler());
        tagHandlerMap.put("#text", new PlainTextHandler());
        return tagHandlerMap;
    }
}
