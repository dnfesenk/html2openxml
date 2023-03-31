package com.denisfesenko.converter.handler;

import com.denisfesenko.converter.HtmlToOpenXMLConverter;
import com.denisfesenko.handler.TableHandler;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.STBorder;
import org.docx4j.wml.Tbl;
import org.docx4j.wml.Tc;
import org.docx4j.wml.Tr;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TableHandlerTest {

    private TableHandler tableHandler;
    private WordprocessingMLPackage wordMLPackage;

    @BeforeEach
    void setUp() throws Exception {
        tableHandler = new TableHandler();
        HtmlToOpenXMLConverter converter = new HtmlToOpenXMLConverter();
        wordMLPackage = WordprocessingMLPackage.createPackage();
        tableHandler.addConverter(converter);
    }

    @Test
    void testHandleTag() throws Exception {
        String html = "<html><body><table><tr><td>Cell 1</td><td>Cell 2</td></tr><tr><td>Cell 3</td><td>Cell 4</td></tr>" +
                "</table></body></html>";
        Document document = Jsoup.parse(html);
        tableHandler.handleTag(document.body().child(0), wordMLPackage);

        // Check that the table was added to the WordprocessingMLPackage
        assertEquals(1, wordMLPackage.getMainDocumentPart().getContent().size());
        assertTrue(wordMLPackage.getMainDocumentPart().getContent().get(0) instanceof org.docx4j.wml.Tbl);
    }

    @Test
    void testTableWithColspan() throws Exception {
        String html = "<html><body><table><tr><td colspan=\"2\">Cell 1</td></tr><tr><td>Cell 2</td><td>Cell 3</td></tr>" +
                "</table></body></html>";
        Document document = Jsoup.parse(html);
        tableHandler.handleTag(document.body().child(0), wordMLPackage);

        // Check that the table was added to the WordprocessingMLPackage
        Tbl table = (Tbl) wordMLPackage.getMainDocumentPart().getContent().get(0);
        Tr firstRow = (Tr) table.getContent().get(0);
        Tc firstCell = (Tc) firstRow.getContent().get(0);
        int colspan = firstCell.getTcPr().getGridSpan().getVal().intValue();
        assertEquals(2, colspan);
    }

    @Test
    void testTableWithStyle() throws Exception {
        String html = "<html><body><table><tr><td style=\"background-color: #FF0000;\">Cell 1</td></tr></table></body></html>";
        Document document = Jsoup.parse(html);
        tableHandler.handleTag(document.body().child(0), wordMLPackage);

        // Check that the table was added to the WordprocessingMLPackage
        Tbl table = (Tbl) wordMLPackage.getMainDocumentPart().getContent().get(0);
        Tr firstRow = (Tr) table.getContent().get(0);
        Tc firstCell = (Tc) firstRow.getContent().get(0);
        String fillColor = firstCell.getTcPr().getShd().getFill();
        assertEquals("#FF0000", fillColor);
    }

    @Test
    void testTableWithTdWidth() throws Exception {
        String html = "<html><body><table><tr><td style=\"width: 100%;\">Cell 1</td></tr></table></body></html>";
        Document document = Jsoup.parse(html);
        tableHandler.handleTag(document.body().child(0), wordMLPackage);

        // Check that the table was added to the WordprocessingMLPackage
        Tbl table = (Tbl) wordMLPackage.getMainDocumentPart().getContent().get(0);
        Tr firstRow = (Tr) table.getContent().get(0);
        Tc firstCell = (Tc) firstRow.getContent().get(0);
        BigInteger cellWidth = firstCell.getTcPr().getTcW().getW();
        assertEquals(BigInteger.valueOf(100).multiply(BigInteger.valueOf(50L)), cellWidth);
    }

    @Test
    void testTableWithWidth() throws Exception {
        String html = "<html><body><table style=\"width: 100%;\"><tr><td>Cell 1</td></tr></table></body></html>";
        Document document = Jsoup.parse(html);
        tableHandler.handleTag(document.body().child(0), wordMLPackage);

        Tbl table = (Tbl) wordMLPackage.getMainDocumentPart().getContent().get(0);
        assertNotNull(table.getTblPr());
        assertNotNull(table.getTblPr().getTblW());
        assertEquals(BigInteger.valueOf(100).multiply(BigInteger.valueOf(50L)), table.getTblPr().getTblW().getW());
    }

    @Test
    void testHandleTagWithMerge() throws Exception {
        String html = "<html><body><table><tr><td merge=\"restart\">Cell 1</td><td>Cell 2</td></tr><tr>" +
                "<td merge=\"continue\">Cell 3</td><td>Cell 4</td></tr></table></body></html>";
        Document document = Jsoup.parse(html);
        tableHandler.handleTag(document.body().child(0), wordMLPackage);

        // Check if the table was added to the WordprocessingMLPackage
        assertEquals(1, wordMLPackage.getMainDocumentPart().getContent().size());
        Tbl tbl = (Tbl) wordMLPackage.getMainDocumentPart().getContent().get(0);

        // Check if the first cell has a restart merge
        Tr firstRow = (Tr) tbl.getContent().get(0);
        Tc firstCell = (Tc) firstRow.getContent().get(0);
        assertNotNull(firstCell.getTcPr().getVMerge());
        assertEquals("restart", firstCell.getTcPr().getVMerge().getVal());

        // Check if the second cell has a continued merge
        Tr secondRow = (Tr) tbl.getContent().get(1);
        Tc secondCell = (Tc) secondRow.getContent().get(0);
        assertNotNull(secondCell.getTcPr().getVMerge());
        assertEquals("continue", secondCell.getTcPr().getVMerge().getVal());
    }

    @Test
    void testHandleTagWithStyles() throws Exception {
        String html = "<html><body><table><tr><td style=\"background-color: #ff0000;\">Cell 1</td>" +
                "<td style=\"background-color: #00ff00;\">Cell 2</td></tr><tr><td style=\"background-color: #0000ff;\">Cell 3</td>" +
                "<td>Cell 4</td></tr></table></body></html>";
        Document document = Jsoup.parse(html);
        tableHandler.handleTag(document.body().child(0), wordMLPackage);

        // Check if the table was added to the WordprocessingMLPackage
        assertEquals(1, wordMLPackage.getMainDocumentPart().getContent().size());
        Tbl tbl = (Tbl) wordMLPackage.getMainDocumentPart().getContent().get(0);

        // Check if the first cell has the correct background color
        Tr firstRow = (Tr) tbl.getContent().get(0);
        Tc firstCell = (Tc) firstRow.getContent().get(0);
        assertNotNull(firstCell.getTcPr().getShd());
        assertEquals("#ff0000", firstCell.getTcPr().getShd().getFill());

        // Check if the third cell has the correct background color
        Tr secondRow = (Tr) tbl.getContent().get(1);
        Tc thirdCell = (Tc) secondRow.getContent().get(0);
        assertNotNull(thirdCell.getTcPr().getShd());
        assertEquals("#0000ff", thirdCell.getTcPr().getShd().getFill());

        // Check if the fourth cell has no background color
        Tc fourthCell = (Tc) secondRow.getContent().get(1);
        assertNull(fourthCell.getTcPr().getShd());
    }

    @Test
    void testHandleTagWithMixedStylesAndMerge() throws Exception {
        String html = "<html><body><table><tr><td style=\"background-color: #ff0000;\" merge=\"restart\">Cell 1</td>" +
                "<td style=\"background-color: #00ff00;\">Cell 2</td></tr><tr>" +
                "<td style=\"background-color: #0000ff;\" merge=\"continue\">Cell 3</td><td>Cell 4</td></tr></table></body></html>";
        Document document = Jsoup.parse(html);
        tableHandler.handleTag(document.body().child(0), wordMLPackage);

        // Check if the table was added to the WordprocessingMLPackage
        assertEquals(1, wordMLPackage.getMainDocumentPart().getContent().size());
        Tbl tbl = (Tbl) wordMLPackage.getMainDocumentPart().getContent().get(0);

        // Check if the first cell has the correct background color and merge type
        Tr firstRow = (Tr) tbl.getContent().get(0);
        Tc firstCell = (Tc) firstRow.getContent().get(0);
        assertNotNull(firstCell.getTcPr().getShd());
        assertEquals("#ff0000", firstCell.getTcPr().getShd().getFill());
        assertNotNull(firstCell.getTcPr().getVMerge());
        assertEquals("restart", firstCell.getTcPr().getVMerge().getVal());

        // Check if the second cell has the correct background color
        Tc secondCell = (Tc) firstRow.getContent().get(1);
        assertNotNull(secondCell.getTcPr().getShd());
        assertEquals("#00ff00", secondCell.getTcPr().getShd().getFill());

        // Check if the third cell has the correct background color and merge type
        Tr secondRow = (Tr) tbl.getContent().get(1);
        Tc thirdCell = (Tc) secondRow.getContent().get(0);
        assertNotNull(thirdCell.getTcPr().getShd());
        assertEquals("#0000ff", thirdCell.getTcPr().getShd().getFill());
        assertNotNull(thirdCell.getTcPr().getVMerge());
        assertEquals("continue", thirdCell.getTcPr().getVMerge().getVal());

        // Check if the fourth cell has no background color
        Tc fourthCell = (Tc) secondRow.getContent().get(1);
        assertNull(fourthCell.getTcPr().getShd());
    }

    @Test
    void testHandleTagWithBorderlessStyle() throws Exception {
        String html = "<html><body><table style=\"border: none;\"><tr><td>Cell 1</td><td>Cell 2</td></tr><tr>" +
                "<td>Cell 3</td><td>Cell 4</td></tr></table></body></html>";
        Document document = Jsoup.parse(html);
        tableHandler.handleTag(document.body().child(0), wordMLPackage);

        // Check if the table was added to the WordprocessingMLPackage
        assertEquals(1, wordMLPackage.getMainDocumentPart().getContent().size());
        Tbl tbl = (Tbl) wordMLPackage.getMainDocumentPart().getContent().get(0);

        // Check if the table borders have been set to STBorder.NONE
        assertNotNull(tbl.getTblPr().getTblBorders());
        assertEquals(STBorder.NONE, tbl.getTblPr().getTblBorders().getBottom().getVal());
        assertEquals(STBorder.NONE, tbl.getTblPr().getTblBorders().getTop().getVal());
        assertEquals(STBorder.NONE, tbl.getTblPr().getTblBorders().getLeft().getVal());
        assertEquals(STBorder.NONE, tbl.getTblPr().getTblBorders().getRight().getVal());
    }
}
