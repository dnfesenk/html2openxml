package com.denisfesenko.converter;

import com.denisfesenko.handler.ParagraphHandler;
import org.docx4j.XmlUtils;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.NumberFormat;
import org.docx4j.wml.P;
import org.docx4j.wml.PPrBase;
import org.docx4j.wml.Tbl;
import org.docx4j.wml.Tc;
import org.docx4j.wml.Tr;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListConversionTest {
    @Test
    void writesNumberingForNestedAndIndependentLists() throws Exception {
        var converter = new HtmlToOpenXMLConverter();
        var doc = converter.convert("<ol start='3'>\n<li><p>First</p><ul><li>Nested</li></ul></li>"
                + "<li>Second</li></ol><ol><li>Restart</li></ol><p>After</p>");
        var output = new ByteArrayOutputStream();
        doc.save(output);
        var reloaded = WordprocessingMLPackage.load(new ByteArrayInputStream(output.toByteArray()));
        converter.convert("<ul><li>Appended</li></ul>", reloaded);
        var main = reloaded.getMainDocumentPart();

        var paragraphs = main.getContent();
        assertEquals(6, paragraphs.size());
        var first = numbering((P) paragraphs.get(0));
        var nested = numbering((P) paragraphs.get(1));
        var second = numbering((P) paragraphs.get(2));
        var restarted = numbering((P) paragraphs.get(3));
        var appended = numbering((P) paragraphs.get(5));
        assertEquals(BigInteger.ZERO, first.getIlvl().getVal());
        assertEquals(BigInteger.ONE, nested.getIlvl().getVal());
        assertEquals(first.getNumId().getVal(), second.getNumId().getVal());
        assertNotEquals(first.getNumId().getVal(), restarted.getNumId().getVal());
        assertNotEquals(nested.getNumId().getVal(), appended.getNumId().getVal());
        assertNull(((P) paragraphs.get(4)).getPPr());

        var definitions = main.getNumberingDefinitionsPart().getJaxbElement().getAbstractNum();
        assertEquals(NumberFormat.DECIMAL, definitions.get(0).getLvl().get(0).getNumFmt().getVal());
        assertEquals(BigInteger.valueOf(3), definitions.get(0).getLvl().get(0).getStart().getVal());
        assertEquals(NumberFormat.BULLET, definitions.get(1).getLvl().get(1).getNumFmt().getVal());
        assertEquals(4, definitions.stream().map(definition -> definition.getAbstractNumId()).distinct().count());
    }

    @Test
    void alignsContinuationParagraphsWithoutAddingMarkers() throws Exception {
        var main = new HtmlToOpenXMLConverter().convert(
                "<ul><li>A<ul><li>B<p>Nested continuation</p></li></ul>C</li></ul>"
                + "<ol><li><p>a</p><p style='text-indent:10px'>b</p></li></ol><p>After</p>")
                .getMainDocumentPart();
        var paragraphs = main.getContent();
        assertEquals(7, paragraphs.size());
        assertContinuation((P) paragraphs.get(2), 1440);
        assertContinuation((P) paragraphs.get(3), 720);
        var continued = (P) paragraphs.get(5);
        assertContinuation(continued, 720);
        assertEquals(BigInteger.valueOf(150), continued.getPPr().getInd().getFirstLine());
        assertNull(((P) paragraphs.get(6)).getPPr());
    }

    @Test
    void clampsDepthAndKeepsAnEmptyItemNumbered() throws Exception {
        var main = new HtmlToOpenXMLConverter().convert(
                "<ul><li>".repeat(10) + "</li></ul>".repeat(10)).getMainDocumentPart();
        assertEquals(10, main.getContent().size());
        var deepest = (P) main.getContent().get(9);
        assertTrue(deepest.getContent().isEmpty());
        assertEquals(BigInteger.valueOf(8), numbering(deepest).getIlvl().getVal());
    }

    @Test
    void validatesStartAndSelectsNumberFormat() throws Exception {
        var definitions = new HtmlToOpenXMLConverter().convert(
                "<ol start='-2'><li>a</li></ol><ol start='99999999999'><li>b</li></ol>"
                + "<ol start='bad'><li>c</li></ol><ol type='i' start='4'><li>d</li></ol>")
                .getMainDocumentPart().getNumberingDefinitionsPart().getJaxbElement().getAbstractNum();
        assertEquals(BigInteger.ZERO, definitions.get(0).getLvl().get(0).getStart().getVal());
        assertEquals(BigInteger.ONE, definitions.get(1).getLvl().get(0).getStart().getVal());
        assertEquals(BigInteger.ONE, definitions.get(2).getLvl().get(0).getStart().getVal());
        var roman = definitions.get(3).getLvl().get(0);
        assertEquals(BigInteger.valueOf(4), roman.getStart().getVal());
        assertEquals(NumberFormat.LOWER_ROMAN, roman.getNumFmt().getVal());
    }

    @Test
    void leavesNumberingToCustomListHandlers() throws Exception {
        for (String tag : new String[]{"li", "ul", "ol"}) {
            String list = "ol".equals(tag) ? "ol" : "ul";
            var main = new HtmlToOpenXMLConverter(Map.of(tag, new ParagraphHandler())).convert(
                    "<" + list + "><li><p>Item</p><p>Continuation</p></li></" + list + ">")
                    .getMainDocumentPart();
            assertNull(main.getNumberingDefinitionsPart(), tag);
            assertTrue(main.getContent().stream().map(P.class::cast).allMatch(p -> p.getPPr() == null), tag);
        }
    }

    @Test
    void sharesNumberingBetweenTableCellsAndTheMainDocument() throws Exception {
        var main = new HtmlToOpenXMLConverter().convert(
                "<ul><li>Outside</li></ul><table><tr><td><ul><li>Inside</li></ul></td></tr></table>")
                .getMainDocumentPart();
        var table = (Tbl) main.getContent().get(1);
        var row = (Tr) table.getContent().get(0);
        var cell = (Tc) XmlUtils.unwrap(row.getContent().get(0));
        var outside = numbering((P) main.getContent().get(0)).getNumId().getVal();
        var inside = numbering((P) cell.getContent().get(0)).getNumId().getVal();
        assertNotEquals(outside, inside);
        var instances = main.getNumberingDefinitionsPart().getJaxbElement().getNum();
        assertTrue(instances.stream().anyMatch(num -> inside.equals(num.getNumId())));
    }

    private static void assertContinuation(P paragraph, long leftIndent) {
        assertNull(numbering(paragraph));
        assertEquals(BigInteger.valueOf(leftIndent), paragraph.getPPr().getInd().getLeft());
    }

    private static PPrBase.NumPr numbering(P paragraph) {
        return paragraph.getPPr().getNumPr();
    }
}
