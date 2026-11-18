package com.denisfesenko.converter;

import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.NumberFormat;
import org.docx4j.wml.P;
import org.docx4j.wml.PPrBase;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ListConversionTest {
    @Test
    void writesNumberingForNestedAndIndependentLists() throws Exception {
        var converter = new HtmlToOpenXMLConverter();
        var doc = converter.convert("<ol start='3'>\n<li><p>First</p><ul><li>Nested</li></ul></li>"
                + "<li>Second</li></ol><ol><li>Restart</li></ol><p>After</p>");
        converter.convert("<ul><li>Appended</li></ul>", doc);
        var output = new ByteArrayOutputStream();
        doc.save(output);
        var main = WordprocessingMLPackage.load(new ByteArrayInputStream(output.toByteArray())).getMainDocumentPart();

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
    }

    private static PPrBase.NumPr numbering(P paragraph) {
        return paragraph.getPPr().getNumPr();
    }
}
