package com.denisfesenko.util;

import org.docx4j.sharedtypes.STOnOff;
import org.docx4j.wml.CTBorder;
import org.docx4j.wml.CTTblLook;
import org.docx4j.wml.ObjectFactory;
import org.docx4j.wml.STBorder;
import org.docx4j.wml.Tbl;
import org.docx4j.wml.TblBorders;
import org.docx4j.wml.TblPr;
import org.jsoup.nodes.Node;

import java.math.BigInteger;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConverterUtils {

    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^#([a-fA-F0-9]{6})$");
    private static final Pattern RGB_COLOR_PATTERN = Pattern.compile("rgb *\\( *(\\d+), *(\\d+), *(\\d+) *\\)");

    private ConverterUtils() {
    }

    public static boolean isHexColor(String value) {
        return HEX_COLOR_PATTERN.matcher(value).matches();
    }

    public static String rgbToHex(String rgb) {
        Matcher matcher = RGB_COLOR_PATTERN.matcher(rgb);
        if (matcher.matches()) {
            return String.format("#%02x%02x%02x", Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3)));
        }
        return "#ffffff";
    }

    public static Node findParentNode(Node node, String nodeName) {
        if (node.parentNode() != null) {
            if (node.parentNode().nodeName().equalsIgnoreCase(nodeName)) {
                return node.parentNode();
            } else {
                return findParentNode(node.parentNode(), nodeName);
            }
        }
        return null;
    }

    public static void fillTblGrid(Tbl tbl, CellWrapper[] firstRow) {
        for (int i = 0; i < firstRow.length; i++) {
            var w = firstRow[i].getWidth();
            var tblGridCol = RunUtils.getObjectFactory().createTblGridCol();
            if (!w.isBlank()) {
                tblGridCol.setW(BigInteger.valueOf(Integer.parseInt(w) * 100L));
            } else {
                tblGridCol.setW(tbl.getTblPr().getTblW().getW()
                        .divide(BigInteger.valueOf(firstRow.length))
                        .multiply(BigInteger.TWO));
            }
            tbl.getTblGrid().getGridCol().set(i, tblGridCol);
        }
    }

    public static void fillTblLook(CTTblLook ctTblLook) {
        ctTblLook.setFirstRow(STOnOff.ONE);
        ctTblLook.setLastRow(STOnOff.ZERO);
        ctTblLook.setFirstColumn(STOnOff.ONE);
        ctTblLook.setLastColumn(STOnOff.ZERO);
        ctTblLook.setNoHBand(STOnOff.ZERO);
        ctTblLook.setNoVBand(STOnOff.ONE);
    }

    public static void setBorderlessStyle(Tbl table) {
        ObjectFactory objectFactory = RunUtils.getObjectFactory();
        TblPr tblPr = objectFactory.createTblPr();
        TblBorders tblBorders = objectFactory.createTblBorders();
        CTBorder ctBorder = objectFactory.createCTBorder();
        ctBorder.setVal(STBorder.NONE);
        tblBorders.setBottom(ctBorder);
        tblBorders.setTop(ctBorder);
        tblBorders.setLeft(ctBorder);
        tblBorders.setRight(ctBorder);
        tblPr.setTblBorders(tblBorders);
        table.setTblPr(tblPr);
    }

    public static <T> void replaceListContent(List<T> targetList, List<T> sourceList) {
        targetList.clear();
        targetList.addAll(sourceList);
    }

    public static int pxToDxa(int px) {
        double inches = px / 96.0;
        return (int) Math.round(inches * 1440);
    }
}
