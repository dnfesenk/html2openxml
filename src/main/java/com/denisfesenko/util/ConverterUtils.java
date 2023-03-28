package com.denisfesenko.util;

import org.docx4j.sharedtypes.STOnOff;
import org.docx4j.wml.CTTblLook;
import org.docx4j.wml.Tbl;
import org.jsoup.nodes.Node;

import java.math.BigInteger;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConverterUtils {

    private ConverterUtils() {
    }

    public static String rgbToHex(String rgb) {
        Pattern c = Pattern.compile("rgb *\\( *([0-9]+), *([0-9]+), *([0-9]+) *\\)");
        Matcher m = c.matcher(rgb);
        if (m.matches()) {
            return String.format("#%02x%02x%02x", Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
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

    public static <T> void replaceListContent(List<T> targetList, List<T> sourceList) {
        targetList.clear();
        targetList.addAll(sourceList);
    }
}
