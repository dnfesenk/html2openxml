package com.denisfesenko.handler;

import com.denisfesenko.core.TagHandler;
import com.denisfesenko.util.ConverterUtils;
import com.denisfesenko.util.RunUtils;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.BinaryPartAbstractImage;
import org.docx4j.wml.Drawing;
import org.docx4j.wml.R;
import org.docx4j.wml.Text;
import org.jsoup.nodes.Node;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Base64;
import java.util.Iterator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Embeds PNG, JPEG and GIF images supplied as Base64 data URIs. */
public class ImageHandler implements TagHandler {
    private static final Pattern DATA_URI = Pattern.compile(
            "^data:image/(png|jpeg|jpg|gif);base64,(.*)$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final long EMU_PER_PIXEL = 9525;

    /** Default maximum decoded image size: 10 MiB. */
    public static final int DEFAULT_MAX_IMAGE_BYTES = 10 * 1024 * 1024;
    /** Default maximum embedded image bytes per document: 50 MiB. */
    public static final long DEFAULT_MAX_DOCUMENT_IMAGE_BYTES = 50L * 1024 * 1024;
    private final long maxDocumentImageBytes;
    private final int maxImageBytes;
    private final long maxDataUriChars;

    /** Uses the default 10 MiB image limit. */
    public ImageHandler() {
        this(DEFAULT_MAX_IMAGE_BYTES);
    }

    /**
     * @param maxImageBytes maximum decoded image bytes; must be positive
     */
    public ImageHandler(int maxImageBytes) {
        this(maxImageBytes, DEFAULT_MAX_DOCUMENT_IMAGE_BYTES);
    }

    /**
     * @param maxImageBytes maximum decoded bytes per image; must be positive
     * @param maxDocumentImageBytes maximum embedded image bytes in the document; must be positive
     */
    public ImageHandler(int maxImageBytes, long maxDocumentImageBytes) {
        if (maxDocumentImageBytes <= 0) throw new IllegalArgumentException("Document image byte limit must be positive");
        this.maxDocumentImageBytes = maxDocumentImageBytes;
        if (maxImageBytes <= 0) throw new IllegalArgumentException("Image byte limit must be positive");
        this.maxImageBytes = maxImageBytes;
        this.maxDataUriChars = 2 * (4 * ((maxImageBytes + 2L) / 3)) + 128;
    }

    @Override
    public void handleTag(Node node, WordprocessingMLPackage wordMLPackage) {
        String source = node.attr("src");
        // Classify the URI from its short header before limiting or copying the payload.
        int start = 0;
        while (start < source.length() && source.charAt(start) <= ' ') start++;
        int comma = source.indexOf(',', start);
        if (comma < start || comma - start > 64
                || !DATA_URI.matcher(source.substring(start, comma + 1)).matches()) {
            addAltText(node, wordMLPackage);
            return;
        }
        if (source.length() > maxDataUriChars) {
            throw new IllegalArgumentException("Image source exceeds the data URI character limit");
        }
        Matcher matcher = DATA_URI.matcher(source.trim());
        if (!matcher.matches()) {
            addAltText(node, wordMLPackage);
            return;
        }
        String encoded = matcher.group(2).replaceAll("\\s", "");
        long decodedSize = encoded.length() * 6L / 8;
        if (encoded.endsWith("==")) decodedSize -= 2;
        else if (encoded.endsWith("=")) decodedSize--;
        if (decodedSize > maxImageBytes) {
            throw new IllegalArgumentException("Decoded image exceeds the configured limit of " + maxImageBytes + " bytes");
        }
        checkDocumentImageBudget(wordMLPackage, Math.max(0, decodedSize));
        byte[] bytes;
        int[] size;
        try {
            bytes = Base64.getDecoder().decode(encoded);
            size = imageSize(bytes, matcher.group(1));
        } catch (IllegalArgumentException | IOException e) {
            throw new IllegalArgumentException("Cannot embed image: invalid or unsupported Base64 image data", e);
        }
        try {
            long width = dimension(node.attr("width"));
            long height = dimension(node.attr("height"));
            if (width == 0 && height == 0) {
                width = size[0] * EMU_PER_PIXEL;
                height = size[1] * EMU_PER_PIXEL;
            } else if (width == 0) {
                width = Math.max(1, Math.round((double) height * size[0] / size[1]));
            } else if (height == 0) {
                height = Math.max(1, Math.round((double) width * size[1] / size[0]));
            }
            long pageWidth = wordMLPackage.getDocumentModel().getSections().get(0)
                    .getPageDimensions().getWritableWidthTwips() * 635L;
            if (width > pageWidth) {
                height = Math.max(1, Math.round((double) height * pageWidth / width));
                width = pageWidth;
            }
            BinaryPartAbstractImage image = BinaryPartAbstractImage.createImagePart(wordMLPackage, bytes);
            long id = wordMLPackage.getDrawingPropsIdTracker().generateId();
            var inline = image.createImageInline("image", "", id, Math.toIntExact(id), width, height, false);
            inline.getDocPr().setDescr(node.attr("alt"));
            Drawing drawing = RunUtils.getObjectFactory().createDrawing();
            drawing.getAnchorOrInline().add(inline);
            R run = RunUtils.getObjectFactory().createR();
            run.getContent().add(drawing);
            RunUtils.getCurrentParagraph(wordMLPackage).getContent().add(run);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to embed image in the Word document", e);
        }
    }

    private void checkDocumentImageBudget(WordprocessingMLPackage document, long incomingBytes) {
        long remaining = maxDocumentImageBytes;
        if (incomingBytes > remaining) throw documentLimitExceeded();
        remaining -= incomingBytes;
        for (var part : document.getParts().getParts().values()) {
            if (part instanceof BinaryPartAbstractImage) {
                BinaryPartAbstractImage image = (BinaryPartAbstractImage) part;
                if (image.getExternalTarget() != null) continue;
                var buffer = image.getBuffer();
                if (buffer == null) throw new IllegalStateException("Cannot read an existing image to check the document budget");
                if (buffer.remaining() > remaining) throw documentLimitExceeded();
                remaining -= buffer.remaining();
            }
        }
    }

    private IllegalArgumentException documentLimitExceeded() {
        return new IllegalArgumentException("Embedded images exceed the document limit of " + maxDocumentImageBytes + " bytes");
    }

    private static long dimension(String value) {
        BigInteger dimension = ConverterUtils.scaleDecimal(value.trim().replaceFirst("(?i)px$", "").trim(),
                Long.toString(EMU_PER_PIXEL));
        return dimension != null && dimension.signum() > 0 ? dimension.longValue() : 0;
    }

    private static int[] imageSize(byte[] bytes, String mimeSubtype) throws IOException {
        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw new IllegalArgumentException("Unrecognized image data");
            ImageReader reader = readers.next();
            try {
                reader.setInput(stream);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                String expected = mimeSubtype.equalsIgnoreCase("jpg") ? "jpeg" : mimeSubtype.toLowerCase(Locale.ROOT);
                if (!format.equals(expected)) throw new IllegalArgumentException("Image MIME type does not match its data");
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0) throw new IllegalArgumentException("Invalid image dimensions");
                return new int[]{width, height};
            } finally {
                reader.dispose();
            }
        }
    }

    private static void addAltText(Node node, WordprocessingMLPackage wordMLPackage) {
        if (!node.attr("alt").isEmpty()) {
            Text text = RunUtils.getObjectFactory().createText();
            text.setValue(node.attr("alt"));
            text.setSpace("preserve");
            R run = RunUtils.getObjectFactory().createR();
            run.getContent().add(text);
            RunUtils.getCurrentParagraph(wordMLPackage).getContent().add(run);
        }
    }

    @Override
    public boolean isRepeatable() {
        return false;
    }
}
