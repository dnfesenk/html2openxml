package com.denisfesenko.converter;

import org.docx4j.wml.P;

import java.math.BigInteger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class HeadingConversionTest {
    @Test
    void appliesWordHeadingStylesAndKeepsBlockBoundaries() throws Exception {
        var doc = new HtmlToOpenXMLConverter().convert("<p>Intro</p><h1>One</h1>\n<h2><em>Two</em></h2>"
                + "<h3>Three</h3><h4>Four</h4><h5>Five</h5><h6>Six</h6>After");
        var main = doc.getMainDocumentPart();
        assertEquals(8, main.getContent().size());
        for (int level = 1; level <= 6; level++) {
            var paragraph = (P) main.getContent().get(level);
            String styleId = "Heading" + level;
            assertEquals(styleId, paragraph.getPPr().getPStyle().getVal());
            var style = main.getStyleDefinitionsPart().getStyleById(styleId);
            assertNotNull(style);
            assertEquals(BigInteger.valueOf(level - 1), style.getPPr().getOutlineLvl().getVal());
        }
        assertNull(((P) main.getContent().get(7)).getPPr());
    }
}
