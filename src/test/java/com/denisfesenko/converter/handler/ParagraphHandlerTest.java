package com.denisfesenko.converter.handler;

import com.denisfesenko.handler.ParagraphHandler;
import com.denisfesenko.util.RunUtils;
import org.docx4j.openpackaging.exceptions.InvalidFormatException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.JcEnumeration;
import org.docx4j.wml.P;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Node;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ParagraphHandlerTest {

    @Test
    void handleTag_appliesParagraphFormatting() throws InvalidFormatException {
        // Arrange
        ParagraphHandler paragraphHandler = new ParagraphHandler();
        Node pNode = Jsoup.parse("<p align=\"left\" style=\"text-indent: 50px;\">Paragraph</p>").body().child(0);
        WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.createPackage();

        // Act
        paragraphHandler.handleTag(pNode, wordMLPackage);

        // Assert
        P p = RunUtils.getCurrentParagraph(wordMLPackage);
        assertNotNull(p.getPPr());
        assertNotNull(p.getPPr().getInd());
        assertNotNull(p.getPPr().getInd().getFirstLine());
        assertEquals(750, p.getPPr().getInd().getFirstLine().intValue());

        assertNotNull(p.getPPr().getJc());
        assertEquals(JcEnumeration.LEFT, p.getPPr().getJc().getVal());
    }
}
