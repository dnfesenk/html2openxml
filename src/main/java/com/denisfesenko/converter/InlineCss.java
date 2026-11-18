package com.denisfesenko.converter;

import com.denisfesenko.util.ConverterUtils;
import com.denisfesenko.util.RunUtils;
import com.denisfesenko.util.JcEnumMapper;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.BooleanDefaultTrue;
import org.docx4j.wml.Color;
import org.docx4j.wml.HpsMeasure;
import org.docx4j.wml.RPr;
import org.docx4j.wml.P;
import org.docx4j.wml.Jc;
import org.docx4j.wml.JcEnumeration;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import java.math.BigInteger;
import java.util.Locale;

/** Applies supported inline text declarations after the semantic formatting of one element. */
final class InlineCss {
    static void apply(Node source, WordprocessingMLPackage document) {
        String style = source.attr("style");
        if (style.isBlank()) return;
        String color = ConverterUtils.parseColor(ConverterUtils.getStyleProperty(style, "color"));
        BigInteger size = ConverterUtils.scaleDecimal(ConverterUtils.getStyleMeasurement(style, "font-size", "px"), "1.5");
        if (size == null) {
            size = ConverterUtils.scaleDecimal(ConverterUtils.getStyleMeasurement(style, "font-size", "pt"), "2");
        }
        Boolean bold = weight(ConverterUtils.getStyleProperty(style, "font-weight"));
        Boolean italic = italic(ConverterUtils.getStyleProperty(style, "font-style"));
        if (color == null && (size == null || size.signum() <= 0) && bold == null && italic == null) return;
        RPr properties = RunUtils.getCurrentRPr(document);
        if (color != null) {
            Color value = RunUtils.getObjectFactory().createColor();
            value.setVal(color);
            properties.setColor(value);
        }
        if (size != null && size.signum() > 0) {
            HpsMeasure value = RunUtils.getObjectFactory().createHpsMeasure();
            value.setVal(size.min(BigInteger.valueOf(3276)));
            properties.setSz(value);
            properties.setSzCs(value);
        }
        if (bold != null) {
            properties.setB(onOff(bold));
            properties.setBCs(onOff(bold));
        }
        if (italic != null) {
            properties.setI(onOff(italic));
            properties.setICs(onOff(italic));
        }
    }

    static Node closestBlock(Node node) {
        for (Node ancestor = node.parentNode(); ancestor != null; ancestor = ancestor.parentNode()) {
            if (ancestor instanceof Element && ((Element) ancestor).isBlock()) return ancestor;
        }
        return null;
    }

    static void alignImplicitParagraph(Node node, P paragraph) {
        // Explicit paragraphs have already been configured by ParagraphHandler (or a
        // custom replacement). Only initialize a new implicit paragraph here.
        if (!paragraph.getContent().isEmpty()) return;
        JcEnumeration alignment = null;
        for (Node ancestor = node.parentNode(); ancestor != null; ancestor = ancestor.parentNode()) {
            if (ancestor.nodeName().equals("p")) return;
            if (alignment == null && ancestor instanceof Element && ((Element) ancestor).isBlock()) {
                alignment = JcEnumMapper.map(ConverterUtils.getStyleProperty(ancestor.attr("style"), "text-align"), false);
            }
        }
        if (alignment != null) {
            if (paragraph.getPPr() == null) paragraph.setPPr(RunUtils.getObjectFactory().createPPr());
            Jc justification = RunUtils.getObjectFactory().createJc();
            justification.setVal(alignment);
            paragraph.getPPr().setJc(justification);
        }
    }

    private static Boolean weight(String value) {
        if (value == null) return null;
        if (value.equalsIgnoreCase("normal")) return false;
        if (value.equalsIgnoreCase("bold")) return true;
        if (value.matches("[1-9]00")) return Integer.parseInt(value) >= 600;
        return null;
    }

    private static Boolean italic(String value) {
        if (value == null) return null;
        switch (value.toLowerCase(Locale.ROOT)) {
            case "normal": return false;
            case "italic":
            case "oblique": return true;
            default: return null;
        }
    }

    private static BooleanDefaultTrue onOff(boolean value) {
        BooleanDefaultTrue result = RunUtils.getObjectFactory().createBooleanDefaultTrue();
        result.setVal(value);
        return result;
    }

    private InlineCss() { }
}
