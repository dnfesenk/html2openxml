package com.denisfesenko.converter;

import com.denisfesenko.util.RunUtils;
import org.docx4j.openpackaging.exceptions.InvalidFormatException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.NumberingDefinitionsPart;
import org.docx4j.wml.NumberFormat;
import org.docx4j.wml.P;
import org.docx4j.wml.PPr;
import org.jsoup.nodes.Node;

import java.math.BigInteger;
import java.util.IdentityHashMap;
import java.util.Map;

final class ListNumbering {
    private final Map<Node, BigInteger> listIds = new IdentityHashMap<>();

    static boolean isList(Node node) {
        return node != null && ("ul".equals(node.nodeName()) || "ol".equals(node.nodeName()));
    }

    void numberItem(Node item, PPr properties, WordprocessingMLPackage wordPackage) {
        Node list = item.parentNode();
        int level = level(item);
        BigInteger id = listIds.computeIfAbsent(list, key -> createDefinition(key, level, wordPackage));
        var factory = RunUtils.getObjectFactory();
        var numbering = factory.createPPrBaseNumPr();
        var indentLevel = factory.createPPrBaseNumPrIlvl();
        indentLevel.setVal(BigInteger.valueOf(level));
        numbering.setIlvl(indentLevel);
        var numberId = factory.createPPrBaseNumPrNumId();
        numberId.setVal(id);
        numbering.setNumId(numberId);
        properties.setNumPr(numbering);
    }

    static void indentContinuation(Node item, P paragraph) {
        var factory = RunUtils.getObjectFactory();
        var properties = paragraph.getPPr();
        if (properties == null) {
            properties = factory.createPPr();
            paragraph.setPPr(properties);
        }
        if (properties.getNumPr() != null) return;
        var indent = properties.getInd();
        if (indent == null) {
            indent = factory.createPPrBaseInd();
            properties.setInd(indent);
        }
        if (indent.getLeft() == null) {
            indent.setLeft(leftIndent(level(item)));
        }
    }

    private static int level(Node item) {
        int depth = 0;
        for (Node ancestor = item.parentNode().parentNode(); ancestor != null; ancestor = ancestor.parentNode()) {
            if (isList(ancestor)) depth++;
        }
        // Word numbering supports nine levels per definition.
        return Math.min(depth, 8);
    }

    private static BigInteger leftIndent(int level) {
        return BigInteger.valueOf(720L * (level + 1));
    }

    private static NumberFormat numberFormat(Node list) {
        if ("ul".equals(list.nodeName())) return NumberFormat.BULLET;
        switch (list.attr("type")) {
            case "a": return NumberFormat.LOWER_LETTER;
            case "A": return NumberFormat.UPPER_LETTER;
            case "i": return NumberFormat.LOWER_ROMAN;
            case "I": return NumberFormat.UPPER_ROMAN;
            default: return NumberFormat.DECIMAL;
        }
    }

    private static BigInteger createDefinition(Node list, int level, WordprocessingMLPackage wordPackage) {
        var main = wordPackage.getMainDocumentPart();
        var part = main.getNumberingDefinitionsPart();
        var factory = RunUtils.getObjectFactory();
        if (part == null) {
            try {
                part = new NumberingDefinitionsPart();
                part.setJaxbElement(factory.createNumbering());
                main.addTargetPart(part);
            } catch (InvalidFormatException e) {
                throw new IllegalStateException("Unable to create Word numbering part", e);
            }
        }

        boolean ordered = "ol".equals(list.nodeName());
        NumberFormat numberFormat = numberFormat(list);
        BigInteger startValue = ordered ? orderedStart(list, numberFormat) : BigInteger.ONE;
        var definition = factory.createNumberingAbstractNum();
        var type = factory.createNumberingAbstractNumMultiLevelType();
        type.setVal("multilevel");
        definition.setMultiLevelType(type);
        for (int index = 0; index <= level; index++) {
            var entry = factory.createLvl();
            entry.setIlvl(BigInteger.valueOf(index));
            var start = factory.createLvlStart();
            start.setVal(startValue);
            entry.setStart(start);
            var format = factory.createNumFmt();
            format.setVal(numberFormat);
            entry.setNumFmt(format);
            var text = factory.createLvlLvlText();
            text.setVal(ordered ? "%" + (index + 1) + "." : "•");
            entry.setLvlText(text);
            var properties = factory.createPPr();
            var indent = factory.createPPrBaseInd();
            indent.setLeft(leftIndent(index));
            indent.setHanging(BigInteger.valueOf(360));
            properties.setInd(indent);
            entry.setPPr(properties);
            definition.getLvl().add(entry);
        }
        return part.addAbstractListNumberingDefinition(definition).getNumId();
    }

    private static BigInteger orderedStart(Node list, NumberFormat format) {
        int minimum = format == NumberFormat.DECIMAL ? 0 : 1;
        try {
            return BigInteger.valueOf(Math.max(minimum, Integer.parseInt(list.attr("start").trim())));
        } catch (NumberFormatException e) {
            return BigInteger.ONE;
        }
    }
}
