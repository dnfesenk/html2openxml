package com.denisfesenko.converter.handler;

import com.denisfesenko.handler.HrHandler;
import com.denisfesenko.util.RunUtils;
import org.docx4j.openpackaging.exceptions.InvalidFormatException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.R;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class HrHandlerTest {

    @Test
    void handleTag() throws InvalidFormatException {
        Element hrElement = new Element(Tag.valueOf("hr"), "");
        WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.createPackage();
        HrHandler hrHandler = new HrHandler();
        hrHandler.handleTag(hrElement, wordMLPackage);
        R currentRun = RunUtils.getCurrentRun(wordMLPackage);
        assertFalse(currentRun.getContent().isEmpty(), "The run should contain a horizontal rule");
    }
}
