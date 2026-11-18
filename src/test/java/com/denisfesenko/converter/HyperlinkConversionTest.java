package com.denisfesenko.converter;

import org.docx4j.XmlUtils;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.P;
import org.docx4j.wml.R;
import org.docx4j.wml.Text;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HyperlinkConversionTest {
    @Test
    void serializesExternalLinksAndPreservesInlineFormatting() throws Exception {
        var doc = new HtmlToOpenXMLConverter().convert("<p>Before <a href='https://example.org/a?x=1&amp;y=2'>"
                + "<b>Bold</b> plain</a> <a href='http://example.org'>HTTP</a> "
                + "<a href='mailto:test@example.org'>Mail</a> <a href='JaVaScRiPt&#58;alert(1)'>Blocked</a>"
                + " <a href='//example.org'>Relative</a></p>");
        var output = new ByteArrayOutputStream();
        doc.save(output);
        var main = WordprocessingMLPackage.load(new ByteArrayInputStream(output.toByteArray())).getMainDocumentPart();
        var paragraph = (P) main.getContent().get(0);
        var links = paragraph.getContent().stream().map(XmlUtils::unwrap)
                .filter(P.Hyperlink.class::isInstance).map(P.Hyperlink.class::cast).collect(Collectors.toList());
        var relationships = links.stream().map(link -> main.getRelationshipsPart().getRelationshipByID(link.getId()))
                .collect(Collectors.toList());
        assertEquals(List.of("https://example.org/a?x=1&y=2", "http://example.org", "mailto:test@example.org"),
                relationships.stream().map(rel -> rel.getTarget()).collect(Collectors.toList()));
        relationships.forEach(rel -> assertEquals("External", rel.getTargetMode()));
        assertEquals(2, links.get(0).getContent().size());
        assertNotNull(((R) XmlUtils.unwrap(links.get(0).getContent().get(0))).getRPr().getB());
        String unlinkedText = paragraph.getContent().stream().map(XmlUtils::unwrap)
                .filter(R.class::isInstance).map(R.class::cast)
                .flatMap(run -> run.getContent().stream()).map(XmlUtils::unwrap)
                .filter(Text.class::isInstance).map(Text.class::cast).map(Text::getValue)
                .collect(Collectors.joining());
        assertTrue(unlinkedText.contains("Blocked Relative"));
    }
}
