package com.denisfesenko.core;

import com.denisfesenko.handler.BoldTagHandler;
import com.denisfesenko.handler.BreakHandler;
import com.denisfesenko.handler.ItalicTagHandler;
import com.denisfesenko.handler.PageBreakHandler;
import com.denisfesenko.handler.ParagraphTagHandler;
import com.denisfesenko.handler.PlainTextTagHandler;
import com.denisfesenko.handler.SpanHandler;
import com.denisfesenko.handler.UnderlineTagHandler;

import java.util.HashMap;
import java.util.Map;

public class TagHandlerFactory {
    public Map<String, TagHandler> createTagHandlerMap() {
        var defaultTagHandlerMap = new HashMap<String, TagHandler>();
        defaultTagHandlerMap.put("u", new UnderlineTagHandler());
        defaultTagHandlerMap.put("span", new SpanHandler());
        defaultTagHandlerMap.put("br", new BreakHandler());
        defaultTagHandlerMap.put("pb", new PageBreakHandler());
        defaultTagHandlerMap.put("p", new ParagraphTagHandler());
        defaultTagHandlerMap.put("b", new BoldTagHandler());
        defaultTagHandlerMap.put("strong", new BoldTagHandler());
        defaultTagHandlerMap.put("i", new ItalicTagHandler());
        defaultTagHandlerMap.put("em", new ItalicTagHandler());
        defaultTagHandlerMap.put("#text", new PlainTextTagHandler());
        return defaultTagHandlerMap;
    }
}
