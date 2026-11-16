package com.denisfesenko.converter.handler;

import com.denisfesenko.converter.HtmlToOpenXMLConverter;
import com.denisfesenko.core.TagHandler;
import com.denisfesenko.handler.ImageHandler;
import org.docx4j.openpackaging.exceptions.InvalidFormatException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.PartName;
import org.docx4j.openpackaging.parts.WordprocessingML.ImagePngPart;
import org.jsoup.nodes.Node;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ImageBudgetTest {
    private byte[] png() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(4, 2, BufferedImage.TYPE_INT_RGB), "png", bytes);
        return bytes.toByteArray();
    }

    private String img(byte[] bytes) {
        return "<img src='data:image/png;base64," + Base64.getEncoder().encodeToString(bytes) + "'>";
    }

    private CountingImagePart existing(WordprocessingMLPackage document, byte[] bytes) throws Exception {
        CountingImagePart part = new CountingImagePart();
        part.setBinaryData(bytes);
        document.getMainDocumentPart().addTargetPart(part);
        part.reads = 0;
        return part;
    }

    @Test
    void scansExistingImagesOnceAcrossManyImagesAndNestedTableConversions() throws Exception {
        byte[] bytes = png();
        var document = WordprocessingMLPackage.createPackage();
        CountingImagePart part = existing(document, bytes);
        var converter = new HtmlToOpenXMLConverter();
        String image = img(bytes);
        converter.convert(image.repeat(20) + "<table><tr><td>" + image
                + "<table><tr><td>" + image + "</td></tr></table></td></tr></table>", document);
        assertEquals(1, part.reads, "The budget must not rescan existing image buffers per new image");
        converter.convert(image, document);
        assertEquals(2, part.reads, "Each separate conversion must refresh the package count");
    }

    @Test
    void refreshesBytesChangedBetweenConversionsAndReleasesScopeAfterFailure() throws Exception {
        byte[] bytes = png();
        var document = WordprocessingMLPackage.createPackage();
        CountingImagePart part = existing(document, bytes);
        var converter = new HtmlToOpenXMLConverter(Map.of("img", new ImageHandler(bytes.length, 3L * bytes.length)));
        converter.convert(img(bytes), document);
        part.setBinaryData(Arrays.copyOf(bytes, 3 * bytes.length));
        assertThrows(IllegalArgumentException.class, () -> converter.convert(img(bytes), document));
        part.setBinaryData(bytes);
        assertDoesNotThrow(() -> converter.convert(img(bytes), document));
        assertEquals(3, part.reads);
    }

    @Test
    void customHandlersCannotBypassBudgetByChangingExistingBytesWithoutAddingParts() throws Exception {
        byte[] bytes = png();
        for (boolean repeatable : new boolean[]{false, true}) {
            var document = WordprocessingMLPackage.createPackage();
            CountingImagePart part = existing(document, bytes);
            TagHandler mutate = new TagHandler() {
                public void handleTag(Node node, WordprocessingMLPackage target) {
                    part.setBinaryData(Arrays.copyOf(bytes, 3 * bytes.length));
                }
                public boolean isRepeatable() { return repeatable; }
            };
            var converter = new HtmlToOpenXMLConverter(Map.of(
                    "img", new ImageHandler(bytes.length, 3L * bytes.length), "mutate", mutate));
            var error = assertThrows(IllegalArgumentException.class,
                    () -> converter.convert(img(bytes) + "<mutate>X</mutate>" + img(bytes), document));
            assertTrue(error.getMessage().contains("document limit"));
            assertEquals(2, part.reads, "Changing bytes in a custom handler must invalidate the budget");
        }
    }

    @Test
    void nestedConverterSharesTheOuterDocumentBudget() throws Exception {
        byte[] bytes = png();
        var document = WordprocessingMLPackage.createPackage();
        TagHandler nested = new TagHandler() {
            public void handleTag(Node node, WordprocessingMLPackage target) {
                // A nested converter adds an image while sharing the outer conversion's scope.
                new HtmlToOpenXMLConverter(Map.of("img", new ImageHandler(bytes.length, bytes.length)))
                        .convert(img(bytes), target);
            }
            public boolean isRepeatable() { return false; }
        };
        var converter = new HtmlToOpenXMLConverter(Map.of(
                "img", new ImageHandler(bytes.length, bytes.length), "nested", nested));
        assertThrows(IllegalArgumentException.class,
                () -> converter.convert("<nested></nested>" + img(bytes), document));
    }

    private static final class CountingImagePart extends ImagePngPart {
        private int reads;
        private CountingImagePart() throws InvalidFormatException {
            super(new PartName("/word/media/existing.png"));
        }
        @Override
        public ByteBuffer getBuffer() {
            reads++;
            return super.getBuffer();
        }
    }
}
