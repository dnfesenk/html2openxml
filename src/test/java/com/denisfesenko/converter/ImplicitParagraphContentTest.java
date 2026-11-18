package com.denisfesenko.converter;

import com.denisfesenko.core.TagHandler;
import com.denisfesenko.handler.TableHandler;
import com.denisfesenko.util.RunUtils;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.*;
import org.jsoup.nodes.Node;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ImplicitParagraphContentTest {
    @Test
    void imageOnlyCellGetsInheritedAlignment() throws Exception {
        var document = new HtmlToOpenXMLConverter().convert(cell(image()));
        P p = cellParagraph(document);
        assertEquals(JcEnumeration.CENTER, p.getPPr().getJc().getVal());
        assertEquals(1, p.getContent().size());
        assertTrue(((R) p.getContent().get(0)).getContent().get(0) instanceof Drawing);
        var bytes = new ByteArrayOutputStream();
        document.save(bytes);
        try (var reopened = new org.apache.poi.xwpf.usermodel.XWPFDocument(
                new java.io.ByteArrayInputStream(bytes.toByteArray()))) {
            var paragraph = reopened.getTables().get(0).getRow(0).getCell(0).getParagraphs().get(0);
            assertEquals(org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER, paragraph.getAlignment());
            assertEquals(1, paragraph.getRuns().get(0).getEmbeddedPictures().size());
        }
    }

    @Test
    void imageAndFollowingTextStayInOneAlignedParagraph() throws Exception {
        var document = new HtmlToOpenXMLConverter().convert(cell(image() + "caption"));
        P p = cellParagraph(document);
        assertEquals(JcEnumeration.CENTER, p.getPPr().getJc().getVal());
        assertEquals(2, p.getContent().size());
        assertTrue(((R) p.getContent().get(0)).getContent().get(0) instanceof Drawing);
        assertEquals("caption", org.docx4j.TextUtils.getText(p));
    }

    @Test
    void leadingBreakAndFollowingTextStayInOneAlignedParagraph() throws Exception {
        var document = new HtmlToOpenXMLConverter().convert(cell("<br>after"));
        P p = cellParagraph(document);
        assertEquals(JcEnumeration.CENTER, p.getPPr().getJc().getVal());
        assertTrue(((R) p.getContent().get(0)).getContent().get(0) instanceof Br);
        assertEquals("after", ((Text) ((R) p.getContent().get(1)).getContent().get(0)).getValue().trim());
    }

    @Test
    void nonemptyImplicitParagraphCanAcquireAlignmentWithoutReplacingExistingAlignment() throws Exception {
        for (boolean explicitAlignment : new boolean[]{false, true}) {
            TagHandler prefix = new TagHandler() {
                public void handleTag(Node node, WordprocessingMLPackage document) {
                    P p = RunUtils.getCurrentParagraph(document);
                    if (explicitAlignment) {
                        p.setPPr(RunUtils.getObjectFactory().createPPr());
                        Jc jc = RunUtils.getObjectFactory().createJc();
                        jc.setVal(JcEnumeration.RIGHT);
                        p.getPPr().setJc(jc);
                    }
                    R r = RunUtils.getObjectFactory().createR();
                    Text text = RunUtils.getObjectFactory().createText();
                    text.setValue("prefix");
                    r.getContent().add(text);
                    p.getContent().add(r);
                }
                public boolean isRepeatable() { return false; }
            };
            P p = (P) new HtmlToOpenXMLConverter(Map.of("prefix", prefix))
                    .convert("<div style='text-align:center'><prefix></prefix>after</div>")
                    .getMainDocumentPart().getContent().get(0);
            assertEquals(explicitAlignment ? JcEnumeration.RIGHT : JcEnumeration.CENTER, p.getPPr().getJc().getVal());
            assertEquals("prefixafter", org.docx4j.TextUtils.getText(p));
        }
    }

    @Test
    void inheritedCellStylesDoNotInvokeCustomDivHandlerButActualDivDoes() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        TagHandler div = new TagHandler() {
            public void handleTag(Node node, WordprocessingMLPackage document) {
                calls.incrementAndGet();
                RunUtils.getCurrentRPr(document).setCaps(RunUtils.createBooleanDefaultTrue());
            }
            public boolean isRepeatable() { return true; }
        };
        var converter = new HtmlToOpenXMLConverter(Map.of("div", div));
        for (String style : new String[]{"", " style='color:red;text-align:center'"}) {
            calls.set(0);
            var document = converter.convert("<table" + style + "><tr><td>plain</td></tr></table>");
            assertEquals(0, calls.get());
            RPr properties = ((R) cellParagraph(document).getContent().get(0)).getRPr();
            if (properties != null) assertNull(properties.getCaps());
            if (!style.isEmpty()) assertEquals("ff0000", properties.getColor().getVal());
        }
        calls.set(0);
        var document = converter.convert("<table style='color:red'><tr><td><div>real</div></td></tr></table>");
        assertEquals(1, calls.get());
        assertTrue(((R) cellParagraph(document).getContent().get(0)).getRPr().getCaps().isVal());
    }

    @Test
    void unsupportedImageWithoutAltDoesNotCreateAnEmptyParagraph() throws Exception {
        assertTrue(new HtmlToOpenXMLConverter().convert("<img src='unsupported'>")
                .getMainDocumentPart().getContent().isEmpty());
    }

    @Test
    void internalStyleTagCannotBeRegisteredAsACustomHandler() {
        TagHandler handler = new com.denisfesenko.handler.BoldHandler();
        assertThrows(IllegalArgumentException.class,
                () -> new HtmlToOpenXMLConverter(Map.of(TableHandler.INHERITED_STYLE_TAG, handler)));
        assertThrows(IllegalArgumentException.class,
                () -> new HtmlToOpenXMLConverter(Map.of(TableHandler.INHERITED_STYLE_TAG.toUpperCase(java.util.Locale.ROOT), handler)));
    }

    private static P cellParagraph(WordprocessingMLPackage document) {
        Tbl table = (Tbl) document.getMainDocumentPart().getContent().get(0);
        Tc cell = (Tc) ((Tr) table.getContent().get(0)).getContent().get(0);
        assertEquals(1, cell.getContent().size());
        return (P) cell.getContent().get(0);
    }

    private static String cell(String content) {
        return "<table style='text-align:center'><tr><td>" + content + "</td></tr></table>";
    }

    private static String image() throws Exception {
        var bytes = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2, 2,
                java.awt.image.BufferedImage.TYPE_INT_RGB), "png", bytes);
        return "<img src='data:image/png;base64," + Base64.getEncoder().encodeToString(bytes.toByteArray()) + "'>";
    }
}
