package com.denisfesenko.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.docx4j.sharedtypes.STOnOff;
import org.docx4j.wml.CTBorder;
import org.docx4j.wml.CTTblLook;
import org.docx4j.wml.ObjectFactory;
import org.docx4j.wml.STBorder;
import org.docx4j.wml.Tbl;
import org.docx4j.wml.TblBorders;
import org.docx4j.wml.TblGridCol;
import org.docx4j.wml.TblPr;
import org.jsoup.nodes.Node;

import java.math.BigInteger;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The ConverterUtils class provides utility methods for converting and manipulating HTML and docx4j objects.
 * This class is not meant to be instantiated.
 */
public class ConverterUtils {

    /**
     * The regular expression pattern to match a hexadecimal color value.
     */
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^#([a-fA-F0-9]{6})$");

    /**
     * The regular expression pattern to match an RGB color value.
     */
    private static final Pattern RGB_COLOR_PATTERN = Pattern.compile("rgb *\\( *(\\d+), *(\\d+), *(\\d+) *\\)");

    /**
     * Checks if the given value is a valid hexadecimal color code.
     *
     * @param value the color value to check
     * @return true if the value is a valid hexadecimal color code, false otherwise
     */
    public static boolean isHexColor(String value) {
        return HEX_COLOR_PATTERN.matcher(value).matches();
    }

    /**
     * Converts an RGB color string to its corresponding hexadecimal color string.
     *
     * @param rgb          The input RGB color string in the format "rgb(r, g, b)".
     * @param defaultColor The default hexadecimal color string to return if the input RGB string
     *                     does not match the expected format.
     * @return The hexadecimal color string corresponding to the input RGB color string, or
     * the default color if the input RGB string is not in the expected format.
     */
    public static String rgbToHex(String rgb, String defaultColor) {
        Matcher matcher = RGB_COLOR_PATTERN.matcher(rgb);
        if (matcher.matches()) {
            return String.format("#%02x%02x%02x", Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3)));
        }
        return defaultColor;
    }

    /**
     * Finds the nearest ancestor node with a specified node name for a given node.
     * This method searches recursively up the node hierarchy until it finds an ancestor
     * node with the specified node name or throws an exception if the parent node is null.
     *
     * @param node     The starting node from which to search for the parent node.
     * @param nodeName The name of the desired parent node.
     * @return The nearest ancestor node with the specified node name.
     * @throws IllegalStateException If the parent node is null.
     */
    public static Node findParentNode(Node node, String nodeName) {
        Node parentNode = node.parentNode();
        if (parentNode == null) {
            throw new IllegalStateException("Parent node cannot be null");
        }
        if (parentNode.nodeName().equalsIgnoreCase(nodeName)) {
            return parentNode;
        } else {
            return findParentNode(parentNode, nodeName);
        }
    }

    /**
     * Fills the table grid with the given cell widths in the first row of the table.
     *
     * @param tbl      the table to fill the grid with
     * @param firstRow the first row of the table containing the cell widths
     */
    public static void fillTblGrid(Tbl tbl, CellWrapper[] firstRow) {
        for (int i = 0; i < firstRow.length; i++) {
            String width = firstRow[i].getWidth();
            TblGridCol tblGridCol = RunUtils.getObjectFactory().createTblGridCol();
            if (NumberUtils.isCreatable(width)) {
                tblGridCol.setW(BigInteger.valueOf(Integer.parseInt(width) * 100L));
            } else {
                tblGridCol.setW(tbl.getTblPr().getTblW().getW()
                        .divide(BigInteger.valueOf(firstRow.length))
                        .multiply(BigInteger.TWO));
            }
            tbl.getTblGrid().getGridCol().set(i, tblGridCol);
        }
    }

    /**
     * Fills the table look with the default borderless style.
     *
     * @param ctTblLook the table look to fill
     */
    public static void fillTblLook(CTTblLook ctTblLook) {
        ctTblLook.setFirstRow(STOnOff.ONE);
        ctTblLook.setLastRow(STOnOff.ZERO);
        ctTblLook.setFirstColumn(STOnOff.ONE);
        ctTblLook.setLastColumn(STOnOff.ZERO);
        ctTblLook.setNoHBand(STOnOff.ZERO);
        ctTblLook.setNoVBand(STOnOff.ONE);
    }

    /**
     * Sets the borderless style for the given table.
     *
     * @param table the table to set the borderless style for
     */
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

    /**
     * Replaces the content of the target list with the content of the source list.
     *
     * @param targetList the list to replace the content of
     * @param sourceList the list to get the content from
     * @param <T>        the type of the list elements
     */
    public static <T> void replaceListContent(List<T> targetList, List<T> sourceList) {
        targetList.clear();
        targetList.addAll(sourceList);
    }

    /**
     * Converts a pixel (px) value to its corresponding OpenXML DXA (twentieths of a point) value.
     *
     * @param px The pixel value to be converted.
     * @return The equivalent OpenXML DXA value as a BigInteger.
     */
    public static BigInteger pxToDxa(int px) {
        double inches = px / 96.0;
        return BigInteger.valueOf(Math.round(inches * 1440));
    }

    /**
     * Converts a pixel (px) value to its corresponding OpenXML half-points value.
     *
     * @param px The pixel value to be converted.
     * @return The equivalent OpenXML half-points value as a BigInteger.
     */
    public static BigInteger pxToHalfPoints(int px) {
        int dpi = 96; // Standard display DPI
        int pointsPerInch = 72;
        return BigInteger.valueOf(Math.round(((double) px / dpi) * pointsPerInch * 2));
    }

    /**
     * Returns the percentage width from a given CSS style string.
     *
     * @param style the CSS style string to extract the percentage width from
     * @return the percentage width as a string, or null if not found
     */
    public static String getPercentWidthFromStyle(String style) {
        return StringUtils.substringBetween(style, Constants.WIDTH + ": ", "%;");
    }

    /**
     * Private constructor to prevent instantiation.
     */
    private ConverterUtils() {
    }
}
