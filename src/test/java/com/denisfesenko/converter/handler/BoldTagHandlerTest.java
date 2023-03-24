package com.denisfesenko.converter.handler;

import com.denisfesenko.handler.BoldTagHandler;
import com.denisfesenko.util.RunUtils;
import org.docx4j.openpackaging.exceptions.InvalidFormatException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.RPr;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Node;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class BoldTagHandlerTest {

    @Test
    void handleTag_appliesBoldFormatting() throws InvalidFormatException {
        // Arrange
        BoldTagHandler boldTagHandler = new BoldTagHandler();
        Node boldNode = Jsoup.parse("<b>Bold text</b>").body().child(0);
        WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.createPackage();

        // Act
        boldTagHandler.handleTag(boldNode, wordMLPackage);

        // Assert
        RPr rPr = RunUtils.getCurrentRPr(wordMLPackage);
        assertNotNull(rPr.getB());
        assertNotNull(rPr.getBCs());
    }
}
