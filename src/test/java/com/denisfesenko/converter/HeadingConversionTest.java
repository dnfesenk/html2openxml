package com.denisfesenko.converter;

import com.denisfesenko.util.RunUtils;
import org.docx4j.TextUtils;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.docx4j.wml.P;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

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

    @Test
    void putsTheListMarkerOnTheFirstHeadingInAnItem() throws Exception {
        var doc = new HtmlToOpenXMLConverter().convert("<ul><li>\n<h2>Head</h2>Body</li></ul>");
        var content = doc.getMainDocumentPart().getContent();
        assertEquals(2, content.size());
        var heading = (P) content.get(0);
        assertEquals("Head", TextUtils.getText(heading));
        assertEquals("Heading2", heading.getPPr().getPStyle().getVal());
        assertNotNull(heading.getPPr().getNumPr());

        var continuation = (P) content.get(1);
        assertEquals("Body", TextUtils.getText(continuation));
        assertNull(continuation.getPPr().getNumPr());
        assertEquals(BigInteger.valueOf(720), continuation.getPPr().getInd().getLeft());
    }

    @Test
    void skipsEmptyHeadingsWithoutConsumingAListMarker() throws Exception {
        var doc = new HtmlToOpenXMLConverter().convert(
                "<h6><span>&nbsp;</span><!-- comment --></h6><h5><br></h5>"
                + "<ul><li><h2>&nbsp;<br></h2>Body</li></ul><h3><img alt='Caption'></h3>");
        var main = doc.getMainDocumentPart();
        var content = main.getContent();
        assertEquals(2, content.size());
        var item = (P) content.get(0);
        assertEquals("Body", TextUtils.getText(item));
        assertNotNull(item.getPPr().getNumPr());
        assertNull(item.getPPr().getPStyle());
        var heading = (P) content.get(1);
        assertEquals("Caption", TextUtils.getText(heading));
        assertEquals("Heading3", heading.getPPr().getPStyle().getVal());
        assertNull(main.getStyleDefinitionsPart().getStyleById("Heading5"));
        assertNull(main.getStyleDefinitionsPart().getStyleById("Heading6"));
    }

    @Test
    void reusesALocalizedTemplateHeadingStyle() throws Exception {
        var doc = WordprocessingMLPackage.createPackage();
        var main = doc.getMainDocumentPart();
        var styles = main.getStyleDefinitionsPart();
        var headingStyle = styles.getStyleById("Heading1");
        headingStyle.setStyleId("1");
        var properties = headingStyle.getPPr();
        properties.getOutlineLvl().setVal(BigInteger.valueOf(2));
        int styleCount = styles.getJaxbElement().getStyle().size();

        new HtmlToOpenXMLConverter().convert("<h1>Template heading</h1>", doc);

        var heading = (P) main.getContent().get(0);
        assertEquals("1", heading.getPPr().getPStyle().getVal());
        assertEquals(styleCount, styles.getJaxbElement().getStyle().size());
        assertSame(headingStyle, styles.getStyleById("1"));
        assertEquals(BigInteger.valueOf(2), headingStyle.getPPr().getOutlineLvl().getVal());
    }

    @Test
    void createsAStylesPartWithTheHeadingAndItsBaseStyle() throws Exception {
        var doc = new WordprocessingMLPackage();
        var main = new MainDocumentPart();
        var factory = RunUtils.getObjectFactory();
        var document = factory.createDocument();
        document.setBody(factory.createBody());
        main.setJaxbElement(document);
        doc.addTargetPart(main);
        assertNull(main.getStyleDefinitionsPart());

        new HtmlToOpenXMLConverter().convert("<h6>Heading</h6>", doc);

        var styles = main.getStyleDefinitionsPart();
        var headingStyle = styles.getStyleById("Heading6");
        assertNotNull(headingStyle);
        assertNotNull(styles.getStyleById(headingStyle.getBasedOn().getVal()));
        assertEquals("Heading6", ((P) main.getContent().get(0)).getPPr().getPStyle().getVal());
    }
}
