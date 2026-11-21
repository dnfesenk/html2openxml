package com.denisfesenko.converter;

import com.denisfesenko.util.RunUtils;
import org.docx4j.XmlUtils;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.StyleDefinitionsPart;
import org.docx4j.relationships.Relationship;
import org.docx4j.wml.P;
import org.docx4j.wml.R;
import org.jsoup.nodes.Node;

import java.net.IDN;
import java.net.URI;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

final class Hyperlinks {
    private static final String STYLE_ID = "Hyperlink";
    private static final Pattern HOST_AUTHORITY = Pattern.compile("([^@]*@)?([^:@]+)(:[0-9]+)?");
    private static final Pattern UNDERSCORE_HOST = Pattern.compile("[a-zA-Z0-9_-]+(?:\\.[a-zA-Z0-9_-]+)*\\.?");
    private static final String HEX_DIGITS = "0123456789ABCDEFabcdef";
    private static final String URI_PUNCTUATION_TO_ESCAPE = " \"<>^`{|}\\";
    private final Map<Node, Link> links = new IdentityHashMap<>();

    private static final class Link {
        private final String target;
        private String relationshipId;
        private P.Hyperlink lastLink;

        private Link(String target) {
            this.target = target;
        }
    }

    static Node enclosingAnchor(Node node) {
        for (Node ancestor = node.parentNode(); ancestor != null; ancestor = ancestor.parentNode()) {
            if ("a".equals(ancestor.nodeName())) return ancestor;
        }
        return null;
    }

    void wrapContent(Node anchor, P paragraph, int start, WordprocessingMLPackage wordPackage) {
        var content = paragraph.getContent();
        if (start >= content.size()) return;
        Link link = linkFor(anchor);
        if (link.target == null) return;
        if (link.relationshipId == null) {
            var relationship = new Relationship();
            relationship.setType("http://schemas.openxmlformats.org/officeDocument/2006/relationships/hyperlink");
            relationship.setTargetMode("External");
            relationship.setTarget(link.target);
            wordPackage.getMainDocumentPart().getRelationshipsPart().addRelationship(relationship);
            link.relationshipId = relationship.getId();
        }
        var added = new ArrayList<>(content.subList(start, content.size()));
        content.subList(start, content.size()).clear();
        if (start == 0 || XmlUtils.unwrap(content.get(start - 1)) != link.lastLink) {
            link.lastLink = RunUtils.getObjectFactory().createPHyperlink();
            link.lastLink.setId(link.relationshipId);
            content.add(RunUtils.getObjectFactory().createPHyperlink(link.lastLink));
        }
        for (Object item : added) {
            Object element = XmlUtils.unwrap(item);
            if (element instanceof R) {
                applyLinkStyle((R) element, wordPackage);
            }
        }
        link.lastLink.getContent().addAll(added);
    }

    boolean hasTarget(Node anchor) {
        return linkFor(anchor).target != null;
    }

    private Link linkFor(Node anchor) {
        return links.computeIfAbsent(anchor, key -> new Link(allowedTarget(key.attr("href").strip())));
    }

    private static void applyLinkStyle(R run, WordprocessingMLPackage wordPackage) {
        var factory = RunUtils.getObjectFactory();
        if (run.getRPr() == null) run.setRPr(factory.createRPr());
        if (run.getRPr().getRStyle() != null) return;

        var styles = RunUtils.getOrCreateStyles(wordPackage);
        String styleId = RunUtils.findStyleId(styles, "character", STYLE_ID, STYLE_ID);
        if (styles.getStyleById(styleId) == null) {
            var style = XmlUtils.deepCopy(StyleDefinitionsPart.getKnownStyles().get(STYLE_ID));
            // The built-in Hyperlink style can reference a character style absent from a template.
            if (style.getBasedOn() != null && styles.getStyleById(style.getBasedOn().getVal()) == null) {
                style.setBasedOn(null);
            }
            styles.getJaxbElement().getStyle().add(style);
        }
        var style = factory.createRStyle();
        style.setVal(styleId);
        run.getRPr().setRStyle(style);
    }

    private static String allowedTarget(String href) {
        href = href.replace("\t", "").replace("\n", "").replace("\r", "");
        if (href.codePoints().anyMatch(Character::isISOControl)) return null;
        try {
            URI uri = URI.create(escapePunctuation(href));
            if (uri.getScheme() == null) return null;
            switch (uri.getScheme().toLowerCase(Locale.ROOT)) {
                case "http":
                case "https":
                    return webTarget(uri);
                case "mailto":
                    return uri.isOpaque() ? uri.toASCIIString() : null;
                default:
                    return null;
            }
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String webTarget(URI uri) {
        if (uri.getHost() != null) return uri.toASCIIString();
        String authority = uri.getRawAuthority();
        if (authority == null) return null;

        var parts = HOST_AUTHORITY.matcher(authority);
        if (!parts.matches()) return null;
        String host = parts.group(2);
        // URI treats an ASCII host containing an underscore as a registry authority.
        if (host.indexOf('_') >= 0 && UNDERSCORE_HOST.matcher(host).matches()) {
            return uri.toASCIIString();
        }
        String asciiHost = IDN.toASCII(host, IDN.USE_STD3_ASCII_RULES);
        String raw = uri.toString();
        int hostStart = uri.getScheme().length() + 3 + parts.start(2);
        URI normalized = URI.create(raw.substring(0, hostStart) + asciiHost + raw.substring(hostStart + host.length()));
        return normalized.getHost() == null ? null : normalized.toASCIIString();
    }

    private static String escapePunctuation(String href) {
        StringBuilder escaped = new StringBuilder(href.length());
        boolean inFragment = false;
        for (int index = 0; index < href.length(); index++) {
            char character = href.charAt(index);
            boolean invalidEscape = character == '%' && (index + 2 >= href.length()
                    || HEX_DIGITS.indexOf(href.charAt(index + 1)) < 0
                    || HEX_DIGITS.indexOf(href.charAt(index + 2)) < 0);
            if (invalidEscape || (character == '#' && inFragment) || URI_PUNCTUATION_TO_ESCAPE.indexOf(character) >= 0) {
                escaped.append('%').append(Integer.toHexString(character).toUpperCase(Locale.ROOT));
            } else {
                escaped.append(character);
                if (character == '#') inFragment = true;
            }
        }
        return escaped.toString();
    }
}
