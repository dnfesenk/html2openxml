package com.denisfesenko.converter;

import com.denisfesenko.util.RunUtils;
import org.docx4j.openpackaging.exceptions.InvalidFormatException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.NumberingDefinitionsPart;
import org.docx4j.wml.NumberFormat;
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
        int depth = 0;
        for (Node ancestor = list.parentNode(); ancestor != null; ancestor = ancestor.parentNode()) {
            if (isList(ancestor)) depth++;
        }
        // Word numbering supports nine levels per definition.
        int level = Math.min(depth, 8);
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
        var definition = factory.createNumberingAbstractNum();
        var type = factory.createNumberingAbstractNumMultiLevelType();
        type.setVal("multilevel");
        definition.setMultiLevelType(type);
        for (int index = 0; index <= level; index++) {
            var entry = factory.createLvl();
            entry.setIlvl(BigInteger.valueOf(index));
            var start = factory.createLvlStart();
            start.setVal(ordered ? orderedStart(list) : BigInteger.ONE);
            entry.setStart(start);
            var format = factory.createNumFmt();
            format.setVal(ordered ? NumberFormat.DECIMAL : NumberFormat.BULLET);
            entry.setNumFmt(format);
            var text = factory.createLvlLvlText();
            text.setVal(ordered ? "%" + (index + 1) + "." : "•");
            entry.setLvlText(text);
            var properties = factory.createPPr();
            var indent = factory.createPPrBaseInd();
            indent.setLeft(BigInteger.valueOf(720L * (index + 1)));
            indent.setHanging(BigInteger.valueOf(360));
            properties.setInd(indent);
            entry.setPPr(properties);
            definition.getLvl().add(entry);
        }
        return part.addAbstractListNumberingDefinition(definition).getNumId();
    }

    private static BigInteger orderedStart(Node list) {
        try {
            return BigInteger.valueOf(Integer.parseInt(list.attr("start").trim()));
        } catch (NumberFormatException e) {
            return BigInteger.ONE;
        }
    }
}
