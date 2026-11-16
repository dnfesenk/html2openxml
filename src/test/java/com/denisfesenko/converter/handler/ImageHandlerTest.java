package com.denisfesenko.converter.handler;

import com.denisfesenko.converter.HtmlToOpenXMLConverter;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.docx4j.dml.wordprocessingDrawing.Inline;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.BinaryPartAbstractImage;
import org.docx4j.wml.*;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class ImageHandlerTest {
    private final HtmlToOpenXMLConverter converter = new HtmlToOpenXMLConverter();

    private byte[] image(String format) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(new BufferedImage(40, 20, BufferedImage.TYPE_INT_RGB), format, bytes));
        return bytes.toByteArray();
    }

    private String uri(String format) throws Exception {
        return "data:image/" + format + ";base64," + Base64.getEncoder().encodeToString(image(format));
    }

    private Inline inline(P paragraph, int index) {
        R run = (R) paragraph.getContent().get(index);
        Drawing drawing = (Drawing) run.getContent().get(0);
        return (Inline) drawing.getAnchorOrInline().get(0);
    }

    @Test
    void embedsSupportedFormatsAndPreservesBytes() throws Exception {
        for (String format : new String[]{"png", "jpeg", "gif"}) {
            WordprocessingMLPackage document = converter.convert("<img src='" + uri(format) + "'>");
            Inline drawing = inline((P) document.getMainDocumentPart().getContent().get(0), 0);
            assertArrayEquals(image(format), BinaryPartAbstractImage.getImage(document, drawing.getGraphic()));
            assertEquals(40 * 9525L, drawing.getExtent().getCx());
            assertEquals(20 * 9525L, drawing.getExtent().getCy());
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            document.save(bytes);
            try (XWPFDocument loaded = new XWPFDocument(new ByteArrayInputStream(bytes.toByteArray()))) {
                assertEquals(1, loaded.getAllPictures().size());
                assertArrayEquals(image(format), loaded.getAllPictures().get(0).getData());
                assertEquals(1, loaded.getParagraphs().get(0).getRuns().get(0).getEmbeddedPictures().size());
            }
        }
    }

    @Test
    void preservesTextOrderAndEscapesAltDescription() throws Exception {
        var document = converter.convert("<p>A<img src='" + uri("png") + "' alt='A &amp; &quot;B&quot; &lt;C&gt;'>B</p>");
        P paragraph = (P) document.getMainDocumentPart().getContent().get(0);
        assertEquals(3, paragraph.getContent().size());
        assertEquals("A", ((Text) ((R) paragraph.getContent().get(0)).getContent().get(0)).getValue());
        assertEquals("A & \"B\" <C>", inline(paragraph, 1).getDocPr().getDescr());
        assertEquals("B", ((Text) ((R) paragraph.getContent().get(2)).getContent().get(0)).getValue());
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        document.save(bytes);
        try (XWPFDocument loaded = new XWPFDocument(new ByteArrayInputStream(bytes.toByteArray()))) {
            assertEquals("AB", loaded.getParagraphs().get(0).getText());
            assertEquals(1, loaded.getAllPictures().size());
        }
    }

    @Test
    void respectsDimensionsAndPreservesAspectRatioForSingleDimension() throws Exception {
        String[] attributes = {"width='100.5px'", "height='50.25'", "width='100' height='30'"};
        long[][] sizes = {{957263, 478632}, {957262, 478631}, {952500, 285750}};
        for (int i = 0; i < attributes.length; i++) {
            P paragraph = (P) converter.convert("<img src='" + uri("png") + "' " + attributes[i] + ">")
                    .getMainDocumentPart().getContent().get(0);
            assertEquals(sizes[i][0], inline(paragraph, 0).getExtent().getCx());
            assertEquals(sizes[i][1], inline(paragraph, 0).getExtent().getCy());
        }
    }

    @Test
    void scalesWideImagesToPage() throws Exception {
        var document = converter.convert("<img src='" + uri("png") + "' width='5000'>");
        Inline drawing = inline((P) document.getMainDocumentPart().getContent().get(0), 0);
        long pageWidth = document.getDocumentModel().getSections().get(0).getPageDimensions().getWritableWidthTwips() * 635L;
        assertEquals(pageWidth, drawing.getExtent().getCx());
        assertEquals(Math.round(pageWidth / 2.0), drawing.getExtent().getCy());
    }

    @Test
    void allocatesUniqueIdsAcrossTablesAndRepeatedConversions() throws Exception {
        String img = "<img src='" + uri("png") + "'>";
        var document = converter.convert(img + "<table><tr><td>" + img + "</td></tr></table>");
        converter.convert("<p>" + img + "</p>", document);
        var content = document.getMainDocumentPart().getContent();
        long first = inline((P) content.get(0), 0).getDocPr().getId();
        Tbl table = (Tbl) content.get(1);
        Tc cell = (Tc) ((Tr) table.getContent().get(0)).getContent().get(0);
        long second = inline((P) cell.getContent().get(0), 0).getDocPr().getId();
        long third = inline((P) content.get(2), 0).getDocPr().getId();
        assertNotEquals(first, second);
        assertNotEquals(first, third);
        assertNotEquals(second, third);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        document.save(bytes);
        try (XWPFDocument loaded = new XWPFDocument(new ByteArrayInputStream(bytes.toByteArray()))) {
            assertEquals(3, loaded.getAllPictures().size());
            assertEquals(1, loaded.getTables().get(0).getRow(0).getCell(0).getParagraphs().get(0)
                    .getRuns().get(0).getEmbeddedPictures().size());
        }
    }

    @Test
    void acceptsWhitespaceInBase64AndJpgAlias() throws Exception {
        String source = uri("jpeg").replace("image/jpeg", "image/jpg").replace(",", ",\n ");
        assertDoesNotThrow(() -> converter.convert("<img src='" + source + "'>"));
    }

    @Test
    void unsupportedSourcesUseAltTextAndDoNotLoadFilesOrUrls() throws Exception {
        for (String source : new String[]{"https://example.invalid/a.png", "file:///missing.png", "data:image/svg+xml;base64,AAAA", ""}) {
            P paragraph = (P) converter.convert("<img src='" + source + "' alt='fallback'>")
                    .getMainDocumentPart().getContent().get(0);
            assertEquals("fallback", ((Text) ((R) paragraph.getContent().get(0)).getContent().get(0)).getValue());
        }
    }

    @Test
    void rejectsMalformedDataAndMismatchedMimeType() throws Exception {
        for (String source : new String[]{"data:image/png;base64,!!!", "data:image/png;base64,", "data:image/png;base64,AAAA", uri("png").replace("image/png", "image/jpeg")}) {
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> converter.convert("<img src='" + source + "'>"));
            assertTrue(error.getMessage().startsWith("Cannot embed image:"));
        }
    }
}
