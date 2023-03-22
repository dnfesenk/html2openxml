package com.denisfesenko.converter;

import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HtmlToOpenXMLConverterTest {

    @Test
    void testHtmlToOpenXMLConversion() throws Exception {
        String html = "<p>Hello, <b>World</b>! This is an <i>example</i> of <b><i>mixed</i> formatting</b>.</p>";

        HtmlToOpenXMLConverter converter = new HtmlToOpenXMLConverter();
        WordprocessingMLPackage wordMLPackage = converter.convert(html);
        assertNotNull(wordMLPackage, "Conversion should return a non-null WordprocessingMLPackage");

        Path outputFile = Files.createTempFile("output", ".docx");
        wordMLPackage.save(outputFile.toFile());
        assertTrue(Files.exists(outputFile), "Output file should exist after saving");
    }
}
