package com.denisfesenko.converter.handler;

import com.denisfesenko.handler.PageBreakHandler;
import com.denisfesenko.util.RunUtils;
import org.docx4j.openpackaging.exceptions.InvalidFormatException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.Br;
import org.docx4j.wml.R;
import org.docx4j.wml.STBrType;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Node;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageBreakHandlerTest {

    @Test
    void handleTag_appliesPageBreakFormatting() throws InvalidFormatException {
        // Arrange
        PageBreakHandler pageBreakHandler = new PageBreakHandler();
        Node breakNode = Jsoup.parse("<pb/>").body().child(0);
        WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.createPackage();

        // Act
        pageBreakHandler.handleTag(breakNode, wordMLPackage);

        // Assert
        R run = RunUtils.getCurrentRun(wordMLPackage);
        assertTrue(run.getContent().get(0) instanceof Br);
        assertEquals(STBrType.PAGE, ((Br) run.getContent().get(0)).getType());
    }
}
