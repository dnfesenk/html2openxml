package com.denisfesenko.converter;

import com.denisfesenko.util.RunUtils;
import org.docx4j.XmlUtils;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.relationships.Relationship;
import org.docx4j.wml.P;
import org.jsoup.nodes.Node;

import java.net.URI;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;

final class Hyperlinks {
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
        Link link = links.get(anchor);
        if (link == null) {
            String target = allowedTarget(anchor.attr("href").strip());
            if (target == null) return;
            link = new Link(target);
            links.put(anchor, link);
        }
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
        link.lastLink.getContent().addAll(added);
    }

    private static String allowedTarget(String href) {
        try {
            URI uri = URI.create(href);
            if (uri.getScheme() == null) return null;
            switch (uri.getScheme().toLowerCase(Locale.ROOT)) {
                case "http":
                case "https":
                    return uri.getHost() == null ? null : uri.toASCIIString();
                case "mailto":
                    return uri.isOpaque() ? uri.toASCIIString() : null;
                default:
                    return null;
            }
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
