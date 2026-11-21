package com.denisfesenko.converter;

import com.denisfesenko.handler.ParagraphHandler;
import com.denisfesenko.util.RunUtils;
import org.docx4j.TextUtils;
import org.docx4j.XmlUtils;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.docx4j.wml.Br;
import org.docx4j.wml.P;
import org.docx4j.wml.Tbl;
import org.docx4j.wml.Tc;
import org.docx4j.wml.Tr;
import org.docx4j.wml.R;
import org.docx4j.wml.Text;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HyperlinkConversionTest {
    @Test
    void serializesExternalLinksAndPreservesInlineFormatting() throws Exception {
        var doc = new HtmlToOpenXMLConverter().convert("<p>Before <a href='https://example.org/a?x=1&amp;y=2'>"
                + "<b style='color:#ff0000'>Bold</b> plain</a> <a href='http://example.org'>HTTP</a> "
                + "<a href='mailto:test@example.org'>Mail</a> <a href='JaVaScRiPt&#58;alert(1)'>Blocked</a>"
                + " <a href='//example.org'>Relative</a></p>");
        var output = new ByteArrayOutputStream();
        doc.save(output);
        var main = WordprocessingMLPackage.load(new ByteArrayInputStream(output.toByteArray())).getMainDocumentPart();
        var paragraph = (P) main.getContent().get(0);
        var links = linksIn(paragraph);
        var relationships = links.stream().map(link -> main.getRelationshipsPart().getRelationshipByID(link.getId()))
                .collect(Collectors.toList());
        assertEquals(List.of("https://example.org/a?x=1&y=2", "http://example.org", "mailto:test@example.org"),
                relationships.stream().map(rel -> rel.getTarget()).collect(Collectors.toList()));
        relationships.forEach(rel -> assertEquals("External", rel.getTargetMode()));
        assertEquals(2, links.get(0).getContent().size());
        var styledRun = (R) XmlUtils.unwrap(links.get(0).getContent().get(0));
        assertNotNull(styledRun.getRPr().getB());
        assertEquals("FF0000", styledRun.getRPr().getColor().getVal());
        assertEquals("Hyperlink", styledRun.getRPr().getRStyle().getVal());
        String unlinkedText = paragraph.getContent().stream().map(XmlUtils::unwrap)
                .filter(R.class::isInstance).map(R.class::cast)
                .flatMap(run -> run.getContent().stream()).map(XmlUtils::unwrap)
                .filter(Text.class::isInstance).map(Text.class::cast).map(Text::getValue)
                .collect(Collectors.joining());
        assertTrue(unlinkedText.contains("Blocked Relative"));
    }

    @Test
    void normalizesBrowserAddressesAndPreservesExistingEscapes() throws Exception {
        var main = new HtmlToOpenXMLConverter().convert("<p>"
                + "<a href='https://a.example/a b?q=1|2'>One</a>"
                + "<a href='https://a.example/?q={x}%20'>Two</a>"
                + "<a href='https://пример.рф:8443/Москва#часть'>Three</a>"
                + "<a href='mailto:a@b.c?subject=Hello World'>Four</a>"
                + "<a href='https://bad host.example/'>Invalid host</a></p>").getMainDocumentPart();

        var links = linksIn((P) main.getContent().get(0));
        assertEquals(List.of("https://a.example/a%20b?q=1%7C2", "https://a.example/?q=%7Bx%7D%20",
                "https://xn--e1afmkfd.xn--p1ai:8443/%D0%9C%D0%BE%D1%81%D0%BA%D0%B2%D0%B0"
                        + "#%D1%87%D0%B0%D1%81%D1%82%D1%8C",
                "mailto:a@b.c?subject=Hello%20World"),
                links.stream().map(link -> main.getRelationshipsPart().getRelationshipByID(link.getId()).getTarget())
                        .collect(Collectors.toList()));
    }

    @Test
    void putsLeadingBreakAndImageFallbackInsideTheirLink() throws Exception {
        var paragraph = (P) new HtmlToOpenXMLConverter().convert(
                "x<a href='https://example.org'><br>y<img alt='Caption'></a>").getMainDocumentPart().getContent().get(0);
        var outside = (R) paragraph.getContent().get(0);
        assertEquals(1, outside.getContent().size());
        var link = linksIn(paragraph).get(0);
        var firstLinkedRun = (R) XmlUtils.unwrap(link.getContent().get(0));
        assertTrue(XmlUtils.unwrap(firstLinkedRun.getContent().get(0)) instanceof Br);
        assertEquals("yCaption", TextUtils.getText(link));
    }

    @Test
    void keepsRelationshipsAcrossParagraphsAndAppendedConversions() throws Exception {
        var converter = new HtmlToOpenXMLConverter();
        var doc = converter.convert("<a href='https://example.org'><p>First</p><p>Second</p></a>");
        var content = doc.getMainDocumentPart().getContent();
        var first = linksIn((P) content.get(0)).get(0);
        var second = linksIn((P) content.get(1)).get(0);
        assertEquals(first.getId(), second.getId());

        converter.convert("<p><a href='https://example.net'>Appended</a></p>", doc);
        var appended = linksIn((P) content.get(2)).get(0);
        assertNotEquals(first.getId(), appended.getId());
    }

    @Test
    void linksInListItemsHeadingsAndTableCellsUseTheDocumentRelationships() throws Exception {
        var main = new HtmlToOpenXMLConverter().convert(
                "<ul><li><h2><a href='https://example.org'>Heading</a></h2>"
                + "<a href='https://example.net'>List text</a></li></ul>"
                + "<table><tr><td><a href='https://example.com'>Cell</a></td></tr></table>").getMainDocumentPart();
        var heading = (P) main.getContent().get(0);
        assertNotNull(heading.getPPr().getNumPr());
        assertEquals("Heading2", heading.getPPr().getPStyle().getVal());
        assertEquals(1, linksIn(heading).size());
        assertEquals(1, linksIn((P) main.getContent().get(1)).size());
        var table = (Tbl) main.getContent().get(2);
        var row = (Tr) table.getContent().get(0);
        var cell = (Tc) XmlUtils.unwrap(row.getContent().get(0));
        var link = linksIn((P) cell.getContent().get(0)).get(0);
        assertEquals("https://example.com", main.getRelationshipsPart().getRelationshipByID(link.getId()).getTarget());
    }

    @Test
    void letsACustomAnchorHandlerOverrideBuiltInHyperlinks() throws Exception {
        var doc = new HtmlToOpenXMLConverter(Map.of("a", new ParagraphHandler())).convert(
                "<a href='https://example.org'>Custom</a>");
        var paragraph = (P) doc.getMainDocumentPart().getContent().get(0);
        assertEquals("Custom", TextUtils.getText(paragraph));
        assertTrue(linksIn(paragraph).isEmpty());
    }

    @Test
    void addsTheHyperlinkStyleToADocumentWithoutStyles() throws Exception {
        var doc = new WordprocessingMLPackage();
        var main = new MainDocumentPart();
        var factory = RunUtils.getObjectFactory();
        var document = factory.createDocument();
        document.setBody(factory.createBody());
        main.setJaxbElement(document);
        doc.addTargetPart(main);

        new HtmlToOpenXMLConverter().convert("<a href='https://example.org'>Link</a>", doc);

        var styles = main.getStyleDefinitionsPart();
        var style = styles.getStyleById("Hyperlink");
        assertNotNull(style.getRPr().getColor());
        assertNotNull(style.getRPr().getU());
        var link = linksIn((P) main.getContent().get(0)).get(0);
        var run = (R) XmlUtils.unwrap(link.getContent().get(0));
        assertEquals("Hyperlink", run.getRPr().getRStyle().getVal());
    }

    private static List<P.Hyperlink> linksIn(P paragraph) {
        return paragraph.getContent().stream().map(XmlUtils::unwrap)
                .filter(P.Hyperlink.class::isInstance).map(P.Hyperlink.class::cast).collect(Collectors.toList());
    }
}
