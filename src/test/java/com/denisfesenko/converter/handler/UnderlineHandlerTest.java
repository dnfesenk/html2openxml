package com.denisfesenko.converter.handler;

import com.denisfesenko.handler.UnderlineHandler;
import com.denisfesenko.util.RunUtils;
import org.docx4j.openpackaging.exceptions.InvalidFormatException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.RPr;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Node;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class UnderlineHandlerTest {

    @Test
    void handleTag_appliesUnderlineFormatting() throws InvalidFormatException {
        // Arrange
        UnderlineHandler underlineHandler = new UnderlineHandler();
        Node underlineNode = Jsoup.parse("<u>Underlined text</u>").body().child(0);
        WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.createPackage();

        // Act
        underlineHandler.handleTag(underlineNode, wordMLPackage);

        // Assert
        RPr rPr = RunUtils.getCurrentRPr(wordMLPackage);
        assertNotNull(rPr.getU());
    }
}
