package com.denisfesenko.converter.handler;

import com.denisfesenko.handler.SupHandler;
import com.denisfesenko.util.RunUtils;
import org.docx4j.openpackaging.exceptions.InvalidFormatException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.R;
import org.docx4j.wml.STVerticalAlignRun;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SupHandlerTest {

    @Test
    void handleTag() throws InvalidFormatException {
        Element supElement = new Element(Tag.valueOf("sup"), "");
        WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.createPackage();
        SupHandler supHandler = new SupHandler();
        supHandler.handleTag(supElement, wordMLPackage);
        R currentRun = RunUtils.getCurrentRun(wordMLPackage);
        STVerticalAlignRun vertAlignVal = currentRun.getRPr().getVertAlign().getVal();
        assertEquals(STVerticalAlignRun.SUPERSCRIPT, vertAlignVal);
    }
}
