package com.denisfesenko.util;

import com.denisfesenko.converter.HtmlToOpenXMLConverter;
import org.docx4j.wml.*;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import static org.junit.jupiter.api.Assertions.*;

class ColorTest {
    @Test
    void normalizesShortHexNamesAndRgb() {
        assertEquals("ffffff", ConverterUtils.parseColor("#fff"));
        assertEquals("AA00ff", ConverterUtils.parseColor(" #A0f "));
        assertEquals("ff0000", ConverterUtils.parseColor(" RED "));
        assertEquals("663399", ConverterUtils.parseColor("RebeccaPurple"));
        assertEquals("808080", ConverterUtils.parseColor("grey"));
        assertEquals("808080", ConverterUtils.parseColor("gray"));
        assertEquals("0102ff", ConverterUtils.parseColor("RGB(1, 2, 255)"));
    }

    @Test
    void invalidColorsDoNotProduceInvalidOpenXml() {
        for (String color : new String[]{"unknown", "#ff", "#gggggg", "rgb(256, 0, 0)", "rgb(99999999999999, 0, 0)", "rgb(-1, 0, 0)", "transparent", ""}) {
            assertNull(ConverterUtils.parseColor(color), color);
        }
        assertNull(ConverterUtils.parseColor(null));
    }

    @Test
    void usesNormalizedColorsInTextBackgroundAndCellsAfterSaving() throws Exception {
        var document = new HtmlToOpenXMLConverter().convert("<p><font color='#F0a'>font</font><span style='background-color:ReBeccAPurple'>span</span></p>"
                + "<table><tr><td style='background-color:red'>cell</td></tr></table>");
        P paragraph = (P) document.getMainDocumentPart().getContent().get(0);
        assertEquals("FF00aa", ((R) paragraph.getContent().get(0)).getRPr().getColor().getVal());
        assertEquals("663399", ((R) paragraph.getContent().get(1)).getRPr().getShd().getFill());
        Tbl table = (Tbl) document.getMainDocumentPart().getContent().get(1);
        assertEquals("ff0000", ((Tc) ((Tr) table.getContent().get(0)).getContent().get(0)).getTcPr().getShd().getFill());
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        document.save(bytes);
        try (XWPFDocument loaded = new XWPFDocument(new ByteArrayInputStream(bytes.toByteArray()))) {
            assertEquals("FF00AA", loaded.getParagraphs().get(0).getRuns().get(0).getColor());
        }
    }

    @Test
    void invalidInnerColorPreservesInheritedColor() throws Exception {
        P paragraph = (P) new HtmlToOpenXMLConverter().convert("<font color='red'><font color='unknown'>A</font></font>")
                .getMainDocumentPart().getContent().get(0);
        assertEquals("ff0000", ((R) paragraph.getContent().get(0)).getRPr().getColor().getVal());
    }
}
