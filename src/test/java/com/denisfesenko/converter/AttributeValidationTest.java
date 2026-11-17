package com.denisfesenko.converter;

import com.denisfesenko.core.TagHandler;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.P;
import org.docx4j.wml.R;
import org.docx4j.wml.Tbl;
import org.docx4j.wml.Tc;
import org.docx4j.wml.Tr;
import org.docx4j.wml.JcEnumeration;
import org.jsoup.nodes.Node;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AttributeValidationTest {
    @Test
    void defaultConstructorsIgnoreUnknownAlignmentAndPreserveOtherFormatting() throws Exception {
        for (var converter : new HtmlToOpenXMLConverter[]{new HtmlToOpenXMLConverter(),
                new HtmlToOpenXMLConverter(false), new HtmlToOpenXMLConverter(Map.of()),
                new HtmlToOpenXMLConverter(Map.of(), false)}) {
            var document = converter.convert("<p align='middle' style='text-indent:10px'><b>Kept</b></p><p>After</p>");
            P paragraph = (P) document.getMainDocumentPart().getContent().get(0);
            assertNull(paragraph.getPPr().getJc());
            assertEquals(BigInteger.valueOf(150), paragraph.getPPr().getInd().getFirstLine());
            assertTrue(((R) paragraph.getContent().get(0)).getRPr().getB().isVal());
            assertEquals("KeptAfter", text(document));
        }
    }

    @Test
    void ignoresUnknownMergeWithoutLosingCellContentColspanOrBackground() throws Exception {
        var document = new HtmlToOpenXMLConverter().convert("<table><tr><td merge='x' colspan='2' "
                + "style='background-color:red'>Cell</td><td>Next</td></tr></table><p>After</p>");
        Tbl table = (Tbl) document.getMainDocumentPart().getContent().get(0);
        Tr row = (Tr) table.getContent().get(0);
        assertEquals(2, row.getContent().size());
        Tc cell = (Tc) row.getContent().get(0);
        assertNull(cell.getTcPr().getVMerge());
        assertEquals(BigInteger.TWO, cell.getTcPr().getGridSpan().getVal());
        assertEquals("FF0000", cell.getTcPr().getShd().getFill().toString().toUpperCase());
        assertEquals("CellNextAfter", text(document));
    }

    @Test
    void strictConstructorsRetainErrorsForUnknownValues() {
        for (var converter : new HtmlToOpenXMLConverter[]{new HtmlToOpenXMLConverter(true),
                new HtmlToOpenXMLConverter(Map.of(), true)}) {
            assertThrows(IllegalArgumentException.class, () -> converter.convert("<p align='middle'>Text</p>"));
            assertThrows(IllegalArgumentException.class,
                    () -> converter.convert("<table><tr><td merge='x'>Cell</td></tr></table>"));
        }
    }

    @Test
    void selectedModeAppliesInsideNestedTablesAndDoesNotLeakAcrossConverters() throws Exception {
        var lenient = new HtmlToOpenXMLConverter();
        var strict = new HtmlToOpenXMLConverter(true);
        String[] documents = {
                "<table><tr><td><p align='middle'>Nested</p></td></tr></table><p>After</p>",
                "<table><tr><td><table><tr><td merge='x'>Nested</td></tr></table></td></tr></table><p>After</p>"
        };
        for (String html : documents) {
            assertEquals("NestedAfter", text(lenient.convert(html)));
            assertThrows(IllegalArgumentException.class, () -> strict.convert(html));
            assertEquals("NestedAfter", text(lenient.convert(html)));
            assertEquals("Recovered", text(strict.convert("<p>Recovered</p>")));
        }
    }

    @Test
    void bothModesPreserveNormalizedValidValuesAndAllowMissingValues() throws Exception {
        for (boolean strict : new boolean[]{false, true}) {
            var converter = new HtmlToOpenXMLConverter(strict);
            var document = converter.convert("<p align=' Center '>Centered</p><table><tr><td merge=' Restart '>A</td>"
                    + "</tr><tr><td merge=' Continue '>B</td></tr></table><p align=' '>Empty</p>");
            P paragraph = (P) document.getMainDocumentPart().getContent().get(0);
            assertEquals(JcEnumeration.CENTER, paragraph.getPPr().getJc().getVal());
            Tbl table = (Tbl) document.getMainDocumentPart().getContent().get(1);
            assertEquals("restart", ((Tc) ((Tr) table.getContent().get(0)).getContent().get(0)).getTcPr().getVMerge().getVal());
            assertEquals("continue", ((Tc) ((Tr) table.getContent().get(1)).getContent().get(0)).getTcPr().getVMerge().getVal());
            assertEquals("CenteredABEmpty", text(document));
        }
    }

    @Test
    void customHandlerFailuresAreNotSwallowedInEitherMode() {
        var failure = new IllegalArgumentException("custom failure");
        TagHandler custom = new TagHandler() {
            public void handleTag(Node node, WordprocessingMLPackage document) { throw failure; }
            public boolean isRepeatable() { return false; }
        };
        for (boolean strict : new boolean[]{false, true}) {
            var converter = new HtmlToOpenXMLConverter(Map.of("custom", custom), strict);
            assertSame(failure, assertThrows(IllegalArgumentException.class,
                    () -> converter.convert("<table><tr><td><custom>Text</custom></td></tr></table>")));
        }
    }

    @Test
    void invalidImagesStillFailInEitherMode() {
        for (boolean strict : new boolean[]{false, true}) {
            var converter = new HtmlToOpenXMLConverter(strict);
            assertThrows(IllegalArgumentException.class,
                    () -> converter.convert("<p><img src='data:image/png;base64,invalid!'></p>"));
        }
    }

    private static String text(WordprocessingMLPackage document) {
        return org.docx4j.TextUtils.getText(document.getMainDocumentPart().getJaxbElement());
    }
}
