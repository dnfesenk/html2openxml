package com.denisfesenko.converter;

import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConversionRegressionTest {
    private final HtmlToOpenXMLConverter converter = new HtmlToOpenXMLConverter();

    @Test
    void nestedBoldKeepsOuterFormatting() throws Exception {
        P paragraph = (P) converter.convert("<p><b>A<b>B</b>C</b>D</p>").getMainDocumentPart().getContent().get(0);
        for (int i = 0; i < 3; i++) {
            assertTrue(((R) paragraph.getContent().get(i)).getRPr().getB().isVal());
        }
        assertNull(((R) paragraph.getContent().get(3)).getRPr());
    }

    @Test
    void colspanPreservesFollowingCell() throws Exception {
        Tbl table = (Tbl) converter.convert("<table><tr><td colspan='2'>A</td><td>B</td></tr></table>").getMainDocumentPart().getContent().get(0);
        assertEquals(3, table.getTblGrid().getGridCol().size());
        Tr row = (Tr) table.getContent().get(0);
        assertEquals(2, row.getContent().size());
        assertEquals(2, ((Tc) row.getContent().get(0)).getTcPr().getGridSpan().getVal().intValue());
        assertEquals("B", text(((Tc) row.getContent().get(1))));
    }

    private String text(Tc cell) {
        P paragraph = (P) cell.getContent().get(0);
        R run = (R) paragraph.getContent().get(0);
        return ((Text) run.getContent().get(0)).getValue();
    }

    @Test
    void emptyTablesAreIgnored() throws Exception {
        assertTrue(converter.convert("<table></table><table><tr></tr></table>").getMainDocumentPart().getContent().isEmpty());
    }

    @Test
    void nestedTableStaysInsideItsCell() throws Exception {
        Tbl outer = (Tbl) converter.convert("<table><tr><td><table><tr><td>inner</td></tr></table></td><td>outer</td></tr></table>").getMainDocumentPart().getContent().get(0);
        assertEquals(1, outer.getContent().size());
        Tr row = (Tr) outer.getContent().get(0);
        assertEquals(2, row.getContent().size());
        Tc cell = (Tc) row.getContent().get(0);
        assertTrue(cell.getContent().get(0) instanceof Tbl);
        assertTrue(cell.getContent().get(cell.getContent().size() - 1) instanceof P);
        assertEquals("outer", text((Tc) row.getContent().get(1)));
    }

    @Test
    void failedCellConversionRestoresDocumentAndFormattingState() throws Exception {
        WordprocessingMLPackage document = converter.convert("<p>original</p>");
        Object original = document.getMainDocumentPart().getContent().get(0);
        HtmlToOpenXMLConverter failingConverter = new HtmlToOpenXMLConverter(java.util.Map.of("fail",
                new com.denisfesenko.core.TagHandler() {
                    public void handleTag(org.jsoup.nodes.Node node, WordprocessingMLPackage target) {
                        throw new IllegalStateException("test failure");
                    }
                    public boolean isRepeatable() { return false; }
                }));
        assertThrows(IllegalStateException.class, () -> failingConverter.convert(
                "<table><tr><td><b><fail>bad</fail></b></td></tr></table>", document));
        assertEquals(1, document.getMainDocumentPart().getContent().size());
        assertSame(original, document.getMainDocumentPart().getContent().get(0));
        P plain = (P) converter.convert("plain").getMainDocumentPart().getContent().get(0);
        assertNull(((R) plain.getContent().get(0)).getRPr());
    }

    @Test
    void backgroundUsesRgbShading() throws Exception {
        P paragraph = (P) converter.convert("<span style='background-color: rgb(255, 0, 0);'>red</span>").getMainDocumentPart().getContent().get(0);
        RPr properties = ((R) paragraph.getContent().get(0)).getRPr();
        assertEquals("ff0000", properties.getShd().getFill());
        assertNull(properties.getHighlight());
    }
    @Test
    void nestedFontInheritsAndOverridesIndividualProperties() throws Exception {
        P paragraph = (P) converter.convert("<font size='16px' color='#0000ff'><font color='#ff0000'>X</font>Y</font>")
                .getMainDocumentPart().getContent().get(0);
        RPr inner = ((R) paragraph.getContent().get(0)).getRPr();
        assertEquals(java.math.BigInteger.valueOf(24), inner.getSz().getVal());
        assertEquals("ff0000", inner.getColor().getVal());
        assertEquals("0000ff", ((R) paragraph.getContent().get(1)).getRPr().getColor().getVal());
    }

    @Test
    void nestedSpanInheritsBackgroundAndRestoresAfterOverride() throws Exception {
        P paragraph = (P) converter.convert("<span style='background-color: #ff0000;'><span>A</span><span style='background-color: #0000ff;'>B</span>C</span>")
                .getMainDocumentPart().getContent().get(0);
        assertEquals("ff0000", ((R) paragraph.getContent().get(0)).getRPr().getShd().getFill());
        assertEquals("0000ff", ((R) paragraph.getContent().get(1)).getRPr().getShd().getFill());
        assertEquals("ff0000", ((R) paragraph.getContent().get(2)).getRPr().getShd().getFill());
    }

    @Test
    void borderlessTableRetainsWidthAndRemovesInternalBorders() throws Exception {
        Tbl table = (Tbl) converter.convert("<table style='width: 50%; border: none;'><tr><td>A</td><td>B</td></tr></table>")
                .getMainDocumentPart().getContent().get(0);
        assertEquals(java.math.BigInteger.valueOf(2500), table.getTblPr().getTblW().getW());
        assertNotNull(table.getTblPr().getTblLook());
        assertEquals(STBorder.NONE, table.getTblPr().getTblBorders().getInsideH().getVal());
        assertEquals(STBorder.NONE, table.getTblPr().getTblBorders().getInsideV().getVal());
    }

    @Test
    void fractionalMeasurementsAreConvertedToIntegerUnits() throws Exception {
        var content = converter.convert("<p style='text-indent: 1.5px;'><font size='12.5px'>X</font></p><table style='width: 33.3%;'><tr><td style='width: 33.3%;'>A</td><td>B</td></tr></table>")
                .getMainDocumentPart().getContent();
        P paragraph = (P) content.get(0);
        assertEquals(java.math.BigInteger.valueOf(23), paragraph.getPPr().getInd().getFirstLine());
        assertEquals(java.math.BigInteger.valueOf(19), ((R) paragraph.getContent().get(0)).getRPr().getSz().getVal());
        Tbl table = (Tbl) content.get(1);
        assertEquals(java.math.BigInteger.valueOf(1665), table.getTblPr().getTblW().getW());
        Tc cell = (Tc) ((Tr) table.getContent().get(0)).getContent().get(0);
        assertEquals(java.math.BigInteger.valueOf(1665), cell.getTcPr().getTcW().getW());
        assertTrue(table.getTblGrid().getGridCol().get(0).getW().compareTo(table.getTblGrid().getGridCol().get(1).getW()) < 0);
    }

    @Test
    void textAfterParagraphStartsNewParagraphWithoutTrailingEmptyParagraph() throws Exception {
        var content = converter.convert("<p>A</p> \n<b>B</b><p>C</p> \n").getMainDocumentPart().getContent();
        assertEquals(3, content.size());
        assertEquals("A", ((Text) ((R) ((P) content.get(0)).getContent().get(0)).getContent().get(0)).getValue());
        R bold = (R) ((P) content.get(1)).getContent().get(0);
        assertEquals("B", ((Text) bold.getContent().get(0)).getValue());
        assertTrue(bold.getRPr().getB().isVal());
        assertEquals("C", ((Text) ((R) ((P) content.get(2)).getContent().get(0)).getContent().get(0)).getValue());
    }

    @Test
    void invalidColspanFallsBackToOneWithoutLosingCellText() throws Exception {
        for (String span : new String[]{"0", "-1", "abc", "1.5", "", "1"}) {
            Tbl table = (Tbl) converter.convert("<table><tr><td colspan='" + span + "'>A</td><td>B</td></tr></table>")
                    .getMainDocumentPart().getContent().get(0);
            assertEquals(2, table.getTblGrid().getGridCol().size(), span);
            Tr row = (Tr) table.getContent().get(0);
            assertEquals(2, row.getContent().size(), span);
            assertEquals("A", text((Tc) row.getContent().get(0)));
            assertEquals("B", text((Tc) row.getContent().get(1)));
        }
    }

    @Test
    void largeColspanIsClampedWithoutIntegerOverflow() throws Exception {
        for (String span : new String[]{"9999", "200000", "2000000000", "999999999999999999999999", "9".repeat(10000)}) {
            Tbl table = (Tbl) converter.convert("<table><tr><td colspan='" + span + "'>A</td><td>B</td></tr></table>")
                    .getMainDocumentPart().getContent().get(0);
            assertEquals(1001, table.getTblGrid().getGridCol().size());
            Tr row = (Tr) table.getContent().get(0);
            assertEquals(2, row.getContent().size());
            Tc first = (Tc) row.getContent().get(0);
            assertEquals(java.math.BigInteger.valueOf(1000), first.getTcPr().getGridSpan().getVal());
            assertEquals("A", text(first));
            assertEquals("B", text((Tc) row.getContent().get(1)));
        }
    }

    @Test
    void legacyCustomHandlerRunsOncePerTextNodeInsideNestedTags() throws Exception {
        java.util.List<String> calls = new java.util.ArrayList<>();
        var custom = new com.denisfesenko.core.TagHandler() {
            public void handleTag(org.jsoup.nodes.Node node, WordprocessingMLPackage document) {
                calls.add(((org.jsoup.nodes.TextNode) node).text());
            }
            public boolean isRepeatable() { return true; }
        };
        var customConverter = new HtmlToOpenXMLConverter(java.util.Map.of("x", custom, "y", custom));
        customConverter.convert("<x>A<x>B</x>C</x>D<x>E</x><x><y>F</y></x>");
        assertEquals(java.util.List.of("A", "B", "C", "E", "F"), calls);
    }

    @Test
    void negativeTextIndentUsesHangingIndent() throws Exception {
        P paragraph = (P) converter.convert("<p style='text-indent: -10.5px;'>A</p>")
                .getMainDocumentPart().getContent().get(0);
        assertEquals(java.math.BigInteger.valueOf(158), paragraph.getPPr().getInd().getHanging());
        assertNull(paragraph.getPPr().getInd().getFirstLine());
    }

}
