package com.denisfesenko.core;

import com.denisfesenko.handler.BoldTagHandler;
import com.denisfesenko.handler.ItalicTagHandler;
import com.denisfesenko.handler.ParagraphTagHandler;
import com.denisfesenko.handler.TextTagHandler;

import java.util.HashMap;
import java.util.Map;

public class TagHandlerFactory {
    public Map<String, TagHandler> createTagHandlerMap() {
        var defaultTagHandlerMap = new HashMap<String, TagHandler>();
        defaultTagHandlerMap.put("p", new ParagraphTagHandler());
        defaultTagHandlerMap.put("b", new BoldTagHandler());
        defaultTagHandlerMap.put("strong", new BoldTagHandler());
        defaultTagHandlerMap.put("i", new ItalicTagHandler());
        defaultTagHandlerMap.put("em", new ItalicTagHandler());
        defaultTagHandlerMap.put("#text", new TextTagHandler());
        return defaultTagHandlerMap;
    }
}
