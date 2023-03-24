package com.denisfesenko.converter;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.docx4j.openpackaging.exceptions.InvalidFormatException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.BooleanDefaultTrue;
import org.docx4j.wml.P;
import org.docx4j.wml.R;
import org.docx4j.wml.RPr;
import org.docx4j.wml.Text;
import org.docx4j.wml.U;
import org.docx4j.wml.UnderlineEnumeration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class HtmlToOpenXMLConverterTest {

    private final HtmlToOpenXMLConverter converter = new HtmlToOpenXMLConverter();

    @Test
    void convertHtmlToDocx_createsValidDocxFileWithCorrectFormatting() {
        // Arrange
        String html = "<p>Hello, <b>World</b>! This is an <span style=\"background-color: rgb(255, 0, 0);\"><i>example</i> of <b><i>mixed</i></span> formatting</b>.</p>";
        HtmlToOpenXMLConverter converter = new HtmlToOpenXMLConverter();
        File outputFile = new File("output.docx");

        // Act
        WordprocessingMLPackage wordMLPackage = null;
        try {
            wordMLPackage = converter.convert(html);
            wordMLPackage.save(outputFile);
        } catch (Exception e) {
            fail("Conversion failed with exception: " + e.getMessage());
        }

        // Assert
        assertTrue(outputFile.exists(), "Output file does not exist");
        assertTrue(outputFile.length() > 0, "Output file is empty");

        // Validate content and formatting
        try (FileInputStream fis = new FileInputStream(outputFile)) {
            XWPFDocument doc = new XWPFDocument(fis);
            List<XWPFParagraph> paragraphs = doc.getParagraphs();
            assertEquals(1, paragraphs.size(), "There should be only one paragraph");

            XWPFParagraph paragraph = paragraphs.get(0);
            List<XWPFRun> runs = paragraph.getRuns();
            assertEquals(8, runs.size(), "There should be seven runs");

            assertRun(runs.get(0), "Hello, ", false, false);
            assertRun(runs.get(1), "World", true, false);
            assertRun(runs.get(2), "! This is an ", false, false);
            assertRun(runs.get(3), "example", false, true);
            assertRun(runs.get(4), " of ", false, false);
            assertRun(runs.get(5), "mixed", true, true);
            assertRun(runs.get(6), " formatting", true, false);
            assertRun(runs.get(7), ".", false, false);
        } catch (IOException e) {
            fail("Failed to read the output file with exception: " + e.getMessage());
        }
    }

    @Test
    void testNestedTags() throws InvalidFormatException {
        String html = "<p><b>Hello</b>, <i>World</i>!</p>";
        WordprocessingMLPackage result = converter.convert(html);

        List<Object> content = result.getMainDocumentPart().getContent();
        assertEquals(1, content.size());

        P paragraph = (P) content.get(0);
        List<Object> paragraphContent = paragraph.getContent();
        assertEquals(4, paragraphContent.size());

        R boldRun = (R) paragraphContent.get(0);
        assertBold(boldRun);
        assertRunContent(boldRun, "Hello");

        R plainRun = (R) paragraphContent.get(1);
        assertPlainText(plainRun, ", ");

        R italicRun = (R) paragraphContent.get(2);
        assertItalic(italicRun);
        assertRunContent(italicRun, "World");

        R plainRun2 = (R) paragraphContent.get(3);
        assertPlainText(plainRun2, "!");
    }

    @Test
    void testMultipleTagsInParagraph() throws InvalidFormatException {
        String html = "<p><b>Bold</b> <i>Italic</i> <u>Underlined</u></p>";
        WordprocessingMLPackage result = converter.convert(html);

        List<Object> content = result.getMainDocumentPart().getContent();
        assertEquals(1, content.size());

        P paragraph = (P) content.get(0);
        List<Object> paragraphContent = paragraph.getContent();
        assertEquals(5, paragraphContent.size());

        R boldRun = (R) paragraphContent.get(0);
        assertBold(boldRun);
        assertRunContent(boldRun, "Bold");

        R plainRun = (R) paragraphContent.get(1);
        assertPlainText(plainRun, " ");

        R italicRun = (R) paragraphContent.get(2);
        assertItalic(italicRun);
        assertRunContent(italicRun, "Italic");

        R plainRun2 = (R) paragraphContent.get(3);
        assertPlainText(plainRun2, " ");

        R underlinedRun = (R) paragraphContent.get(4);
        assertUnderlined(underlinedRun);
        assertRunContent(underlinedRun, "Underlined");
    }

    @Test
    void testEmptyTags() throws InvalidFormatException {
        String html = "<p><b></b><i></i><u></u></p>";
        WordprocessingMLPackage result = converter.convert(html);

        List<Object> content = result.getMainDocumentPart().getContent();
        assertEquals(1, content.size());

        P paragraph = (P) content.get(0);
        List<Object> paragraphContent = paragraph.getContent();
        assertEquals(0, paragraphContent.size());
    }

    private void assertBold(R run) {
        RPr runProperties = run.getRPr();
        assertNotNull(runProperties);
        BooleanDefaultTrue bold = runProperties.getB();
        assertNotNull(bold);
        assertTrue(bold.isVal());
    }

    private void assertItalic(R run) {
        RPr runProperties = run.getRPr();
        assertNotNull(runProperties);
        BooleanDefaultTrue italic = runProperties.getI();
        assertNotNull(italic);
        assertTrue(italic.isVal());
    }

    private void assertUnderlined(R run) {
        RPr runProperties = run.getRPr();
        assertNotNull(runProperties);
        U underline = runProperties.getU();
        assertNotNull(underline);
        assertEquals(UnderlineEnumeration.SINGLE, underline.getVal());
    }

    private void assertPlainText(R run, String expectedText) {
        RPr runProperties = run.getRPr();
        if (runProperties != null) {
            assertNull(runProperties.getB());
            assertNull(runProperties.getI());
            assertNull(runProperties.getU());
        }
        assertRunContent(run, expectedText);
    }

    private void assertRunContent(R run, String expectedText) {
        List<Object> runContent = run.getContent();
        assertEquals(1, runContent.size());

        Object textElement = runContent.get(0);
        assertTrue(textElement instanceof Text);

        Text text = (Text) textElement;
        assertEquals(expectedText, text.getValue());
    }

    private void assertRun(XWPFRun run, String text, boolean bold, boolean italic) {
        assertEquals(text, run.getText(0), "Incorrect run text");
        assertEquals(bold, run.isBold(), "Bold formatting is incorrect");
        assertEquals(italic, run.isItalic(), "Italic formatting is incorrect");
    }
}
