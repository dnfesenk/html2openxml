package com.denisfesenko.converter;

import org.docx4j.wml.*;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

class InlineCssTest {
    private final HtmlToOpenXMLConverter converter = new HtmlToOpenXMLConverter();

    @Test
    void editorParagraphConvertsAllFiveProperties() throws Exception {
        P paragraph = paragraph("<p style='color:rebeccapurple;font-size:12pt;text-align:center;font-weight:700;font-style:italic'>Editor text</p>");
        assertEquals(JcEnumeration.CENTER, paragraph.getPPr().getJc().getVal());
        RPr style = run(paragraph, 0);
        assertEquals("663399", style.getColor().getVal());
        assertEquals(BigInteger.valueOf(24), style.getSz().getVal());
        assertEquals(style.getSz().getVal(), style.getSzCs().getVal());
        assertTrue(style.getB().isVal());
        assertTrue(style.getBCs().isVal());
        assertTrue(style.getI().isVal());
        assertTrue(style.getICs().isVal());
    }

    @Test
    void nestedCssOverridesAndRestoresInheritedValues() throws Exception {
        P p = paragraph("<p style='color:red;font-size:16px;font-weight:bold;font-style:italic'>A"
                + "<span style='color:blue;font-size:10pt;font-weight:normal;font-style:normal'>B</span>C</p>");
        for (int i : new int[]{0, 2}) {
            assertEquals("ff0000", run(p, i).getColor().getVal());
            assertEquals(BigInteger.valueOf(24), run(p, i).getSz().getVal());
            assertTrue(run(p, i).getB().isVal());
            assertTrue(run(p, i).getI().isVal());
        }
        assertEquals("0000ff", run(p, 1).getColor().getVal());
        assertEquals(BigInteger.valueOf(20), run(p, 1).getSz().getVal());
        assertFalse(run(p, 1).getB().isVal());
        assertFalse(run(p, 1).getI().isVal());
    }

    @Test
    void innerSemanticTagsOverrideInheritedCssAndOwnCssOverridesSemanticTag() throws Exception {
        P p = paragraph("<span style='font-weight:normal;font-style:normal'><b><i>A</i></b></span>"
                + "<b style='font-weight:normal'><i style='font-style:normal'>B</i></b>");
        assertTrue(run(p, 0).getB().isVal());
        assertTrue(run(p, 0).getI().isVal());
        assertFalse(run(p, 1).getB().isVal());
        assertFalse(run(p, 1).getI().isVal());
    }

    @Test
    void cssAndLegacyFontAttributesRespectElementOrder() throws Exception {
        P p = paragraph("<span style='color:red;font-size:16px'><font color='blue' size='20px'>A</font>B</span>"
                + "<font color='red' size='30px' style='color:green;font-size:10pt'>C</font>");
        assertEquals("0000ff", run(p, 0).getColor().getVal());
        assertEquals(BigInteger.valueOf(30), run(p, 0).getSz().getVal());
        assertEquals("ff0000", run(p, 1).getColor().getVal());
        assertEquals(BigInteger.valueOf(24), run(p, 1).getSz().getVal());
        assertEquals("008000", run(p, 2).getColor().getVal());
        assertEquals(BigInteger.valueOf(20), run(p, 2).getSz().getVal());
    }

    @Test
    void cssDeclarationsAcceptCaseSpacingDecimalsAndLastDeclaration() throws Exception {
        P p = paragraph("<p align='right' style=' TEXT-ALIGN : CENTER ;color:blue; COLOR : #f00; FONT-SIZE:12.5PX;"
                + "font-weight:400;FONT-WEIGHT:BOLD; FONT-STYLE:OBLIQUE'>A</p>");
        assertEquals(JcEnumeration.CENTER, p.getPPr().getJc().getVal());
        assertEquals("ff0000", run(p, 0).getColor().getVal());
        assertEquals(BigInteger.valueOf(19), run(p, 0).getSz().getVal());
        assertTrue(run(p, 0).getB().isVal());
        assertTrue(run(p, 0).getI().isVal());
    }

    @Test
    void invalidCssPreservesInheritanceAndDoesNotTriggerStrictAttributeValidation() throws Exception {
        String html = "<p align='right' style='color:red;font-size:16px;text-align:middle;font-weight:bold;font-style:italic'>"
                + "<span style='color:not-a-color;font-size:2em;font-weight:invalid;font-style:invalid'>A</span></p>";
        for (boolean strict : new boolean[]{false, true}) {
            P p = (P) new HtmlToOpenXMLConverter(strict).convert(html).getMainDocumentPart().getContent().get(0);
            assertEquals(JcEnumeration.RIGHT, p.getPPr().getJc().getVal());
            assertEquals("ff0000", run(p, 0).getColor().getVal());
            assertEquals(BigInteger.valueOf(24), run(p, 0).getSz().getVal());
            assertTrue(run(p, 0).getB().isVal());
            assertTrue(run(p, 0).getI().isVal());
        }
        P p = paragraph("<p style='font-size:0px;color:transparent;font-weight:1000;font-style:unset'>A</p>");
        assertNull(((R) p.getContent().get(0)).getRPr());
    }

    @Test
    void inheritedTableStylesReachNestedCellsAndDoNotLeakAfterTable() throws Exception {
        var content = converter.convert("<div style='color:red;font-size:16px'><table><tr style='font-weight:700'>"
                + "<td style='font-style:italic;text-align:right'><p>A</p><table><tr><td><p>B</p></td></tr></table></td>"
                + "<td style='color:blue'>C</td></tr></table></div><p>D</p>").getMainDocumentPart().getContent();
        Tbl outer = (Tbl) content.get(0);
        Tr row = (Tr) outer.getContent().get(0);
        Tc first = (Tc) row.getContent().get(0);
        P a = (P) first.getContent().get(0);
        assertEquals("ff0000", run(a, 0).getColor().getVal());
        assertEquals(BigInteger.valueOf(24), run(a, 0).getSz().getVal());
        assertTrue(run(a, 0).getB().isVal());
        assertTrue(run(a, 0).getI().isVal());
        assertEquals(JcEnumeration.RIGHT, a.getPPr().getJc().getVal());
        Tbl nested = (Tbl) first.getContent().get(1);
        P b = (P) ((Tc) ((Tr) nested.getContent().get(0)).getContent().get(0)).getContent().get(0);
        assertEquals("ff0000", run(b, 0).getColor().getVal());
        assertTrue(run(b, 0).getB().isVal());
        P c = (P) ((Tc) row.getContent().get(1)).getContent().get(0);
        assertEquals("0000ff", run(c, 0).getColor().getVal());
        assertNull(((R) ((P) content.get(1)).getContent().get(0)).getRPr());
    }

    @Test
    void blockAlignmentIsInheritedButParagraphAttributeAndCssTakePrecedence() throws Exception {
        var content = converter.convert("<div style='text-align:right'><p>A</p><p align='center'>B</p>"
                + "<p align='right' style='text-align:justify'>C</p></div>").getMainDocumentPart().getContent();
        assertEquals(JcEnumeration.RIGHT, ((P) content.get(0)).getPPr().getJc().getVal());
        assertEquals(JcEnumeration.CENTER, ((P) content.get(1)).getPPr().getJc().getVal());
        assertEquals(JcEnumeration.BOTH, ((P) content.get(2)).getPPr().getJc().getVal());
    }

    @Test
    void numericFontWeightsMapToBoldAtSixHundred() throws Exception {
        for (int weight = 100; weight <= 900; weight += 100) {
            P p = paragraph("<span style='font-weight:" + weight + "'>A</span>");
            assertEquals(weight >= 600, run(p, 0).getB().isVal());
        }
    }

    @Test
    void editorStyleMarkupSurvivesDocxSerialization() throws Exception {
        var document = converter.convert("<p style='text-align:center'><b style='font-weight:normal'>"
                + "<span style='color:#123456;font-size:11pt;font-weight:400;font-style:italic'>Editor export</span>"
                + "</b></p>");
        var bytes = new java.io.ByteArrayOutputStream();
        document.save(bytes);
        try (var reopened = new org.apache.poi.xwpf.usermodel.XWPFDocument(
                new java.io.ByteArrayInputStream(bytes.toByteArray()))) {
            var paragraph = reopened.getParagraphs().get(0);
            var run = paragraph.getRuns().get(0);
            assertEquals("Editor export", paragraph.getText());
            assertEquals(org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER, paragraph.getAlignment());
            assertEquals("123456", run.getColor());
            assertEquals(Double.valueOf(11), run.getFontSizeAsDouble());
            assertFalse(run.isBold());
            assertTrue(run.isItalic());
        }
    }

    private P paragraph(String html) throws Exception {
        return (P) converter.convert(html).getMainDocumentPart().getContent().get(0);
    }

    private RPr run(P paragraph, int index) {
        return ((R) paragraph.getContent().get(index)).getRPr();
    }
}
