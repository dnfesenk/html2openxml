package com.denisfesenko.converter.handler;

import com.denisfesenko.handler.UnderlineTagHandler;
import com.denisfesenko.util.RunUtils;
import org.docx4j.openpackaging.exceptions.InvalidFormatException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.RPr;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Node;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class UnderlineTagHandlerTest {

    @Test
    void handleTag_appliesUnderlineFormatting() throws InvalidFormatException {
        // Arrange
        UnderlineTagHandler underlineTagHandler = new UnderlineTagHandler();
        Node underlineNode = Jsoup.parse("<u>Underlined text</u>").body().child(0);
        WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.createPackage();

        // Act
        underlineTagHandler.handleTag(underlineNode, wordMLPackage);

        // Assert
        RPr rPr = RunUtils.getCurrentRPr(wordMLPackage);
        assertNotNull(rPr.getU());
    }
}
