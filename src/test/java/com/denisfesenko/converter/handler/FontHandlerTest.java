package com.denisfesenko.converter.handler;

import com.denisfesenko.converter.HtmlToOpenXMLConverter;
import com.denisfesenko.util.RunUtils;
import org.docx4j.openpackaging.exceptions.InvalidFormatException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.Color;
import org.docx4j.wml.HpsMeasure;
import org.docx4j.wml.RPr;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


class FontHandlerTest {
    private final HtmlToOpenXMLConverter converter = new HtmlToOpenXMLConverter();

    @Test
    void testHandleTag_fontWithSizeAndColor() throws InvalidFormatException {
        String html = "<font size=\"16px\" color=\"#FF5733\">Sample Text</font>";
        WordprocessingMLPackage wordMLPackage = converter.convert(html);
        RPr rPr = RunUtils.getCurrentRPr(wordMLPackage);

        Color color = rPr.getColor();
        HpsMeasure size = rPr.getSz();
        HpsMeasure sizeCs = rPr.getSzCs();

        assertEquals("#FF5733", color.getVal());
        assertEquals(BigInteger.valueOf(24), size.getVal());
        assertEquals(BigInteger.valueOf(24), sizeCs.getVal());
    }

    @Test
    void testHandleTag_fontWithSizeOnly() throws InvalidFormatException {
        String html = "<font size=\"16px\">Sample Text</font>";
        WordprocessingMLPackage wordMLPackage = converter.convert(html);
        RPr rPr = RunUtils.getCurrentRPr(wordMLPackage);

        Color color = rPr.getColor();
        HpsMeasure size = rPr.getSz();
        HpsMeasure sizeCs = rPr.getSzCs();

        assertNull(color);
        assertEquals(BigInteger.valueOf(24), size.getVal());
        assertEquals(BigInteger.valueOf(24), sizeCs.getVal());
    }

    @Test
    void testHandleTag_fontWithColorOnly() throws InvalidFormatException {
        String html = "<font color=\"#FF5733\">Sample Text</font>";
        WordprocessingMLPackage wordMLPackage = converter.convert(html);
        RPr rPr = RunUtils.getCurrentRPr(wordMLPackage);

        Color color = rPr.getColor();
        HpsMeasure size = rPr.getSz();
        HpsMeasure sizeCs = rPr.getSzCs();

        assertEquals("#FF5733", color.getVal());
        assertNull(size);
        assertNull(sizeCs);
    }
}

