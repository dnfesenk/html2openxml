package com.denisfesenko.converter.handler;

import com.denisfesenko.handler.BreakHandler;
import com.denisfesenko.util.RunUtils;
import org.docx4j.openpackaging.exceptions.InvalidFormatException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.Br;
import org.docx4j.wml.R;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Node;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BreakHandlerTest {

    @Test
    void handleTag_appliesBreakFormatting() throws InvalidFormatException {
        // Arrange
        BreakHandler breakHandler = new BreakHandler();
        Node breakNode = Jsoup.parse("<br/>").body().child(0);
        WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.createPackage();

        // Act
        breakHandler.handleTag(breakNode, wordMLPackage);

        // Assert
        R run = RunUtils.getCurrentRun(wordMLPackage);
        assertTrue(run.getContent().get(0) instanceof Br);
    }
}
