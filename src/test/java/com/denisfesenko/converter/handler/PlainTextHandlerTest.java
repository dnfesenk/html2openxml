package com.denisfesenko.converter.handler;

import com.denisfesenko.handler.PlainTextHandler;
import com.denisfesenko.util.RunUtils;
import org.docx4j.openpackaging.exceptions.InvalidFormatException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.RPr;
import org.jsoup.nodes.TextNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

class PlainTextHandlerTest {

    @Test
    void handleTag_appliesPlainText() throws InvalidFormatException {
        // Arrange
        PlainTextHandler plainTextHandler = new PlainTextHandler();
        TextNode plainTextNode = new TextNode("Plain text");
        WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.createPackage();

        // Act
        plainTextHandler.handleTag(plainTextNode, wordMLPackage);

        // Assert
        RPr rPr = RunUtils.getCurrentRPr(wordMLPackage);
        assertNull(rPr.getB());
        assertNull(rPr.getI());
        assertNull(rPr.getU());
    }
}
