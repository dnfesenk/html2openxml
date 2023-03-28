package com.denisfesenko.core;

import com.denisfesenko.handler.BoldHandler;
import com.denisfesenko.handler.BreakHandler;
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

public class TagHandlerFactory {
    public Map<String, TagHandler> createTagHandlerMap() {
        Map<String, TagHandler> tagHandlerMap = new HashMap<>();
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
