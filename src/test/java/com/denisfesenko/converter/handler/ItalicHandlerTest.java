package com.denisfesenko.converter.handler;

import com.denisfesenko.handler.ItalicHandler;
import com.denisfesenko.util.RunUtils;
import org.docx4j.openpackaging.exceptions.InvalidFormatException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.RPr;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Node;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ItalicHandlerTest {

    @Test
    void handleTag_appliesItalicFormatting() throws InvalidFormatException {
        // Arrange
        ItalicHandler italicHandler = new ItalicHandler();
        Node italicNode = Jsoup.parse("<i>Italic text</i>").body().child(0);
        WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.createPackage();

        // Act
        italicHandler.handleTag(italicNode, wordMLPackage);

        // Assert
        RPr rPr = RunUtils.getCurrentRPr(wordMLPackage);
        assertNotNull(rPr.getI());
        assertNotNull(rPr.getICs());
    }
}