package com.denisfesenko.handler;

import com.denisfesenko.converter.HtmlToOpenXMLConverter;
import com.denisfesenko.core.TagHandler;
import com.denisfesenko.util.CellWrapper;
import com.denisfesenko.util.Constants;
import com.denisfesenko.util.ConverterUtils;
import com.denisfesenko.util.RunUtils;
import org.docx4j.model.table.TblFactory;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.Tbl;
import org.docx4j.wml.TblWidth;
import org.docx4j.wml.Tc;
import org.docx4j.wml.Tr;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class handles the conversion of HTML tables to WordprocessingMLPackage format.
 */
public class TableHandler implements TagHandler {

    /**
     * Reserved implementation tag for inherited cell styles; not a custom-handler extension point.
     */
    public static final String INHERITED_STYLE_TAG = "html2openxml-inherited-style";

    private final boolean strictValidation;

    /**
     * Creates a standalone handler with the historical strict validation behavior.
     */
    public TableHandler() {
        this(true);
    }

    /**
     * Creates a handler with optional strict attribute validation.
     *
     * @param strictValidation whether unknown merge values cause errors
     */
    public TableHandler(boolean strictValidation) {
        this.strictValidation = strictValidation;
    }

    private HtmlToOpenXMLConverter converter;

    /**
     * This method is called to process the input node and convert it to the corresponding
     * WordprocessingMLPackage format.
     *
     * @param node          The input HTML node to be processed.
     * @param wordMLPackage The WordprocessingMLPackage instance where the converted content
     *                      will be added.
     */
    @Override
    public void handleTag(Node node, WordprocessingMLPackage wordMLPackage) {
        Element rootNode = (Element) node;
        Element docTable = rootNode.selectFirst("table");
        if (docTable != null) {
            CellWrapper[][] cellMatrix = parseTable(new Elements(docTable.select("tr").stream()
                    .filter(tr -> tr.closest("table") == docTable).collect(java.util.stream.Collectors.toList())));
            int maxCols = getMaxColumns(cellMatrix);
            if (maxCols == 0) return;
            int cellWidthTwips = wordMLPackage.getDocumentModel().getSections().get(0).getPageDimensions().getWritableWidthTwips() / maxCols;
            Tbl table = createTableWithSettings(cellMatrix, maxCols, cellWidthTwips, docTable, wordMLPackage);
            if ("none".equalsIgnoreCase(ConverterUtils.getStyleProperty(docTable.attr(Constants.STYLE), "border"))) {
                ConverterUtils.setBorderlessStyle(table);
            }
            wordMLPackage.getMainDocumentPart().getContent().add(table);
        }
    }

    /**
     * This method returns whether the tag handler can be applied to multiple tags.
     *
     * @return false, as the TableHandler should not be applied to multiple tags.
     */
    @Override
    public boolean isRepeatable() {
        return false;
    }

    /**
     * Adds the HtmlToOpenXMLConverter instance to this TableHandler.
     *
     * @param converter The HtmlToOpenXMLConverter instance.
     * @return The TableHandler instance.
     */
    public TableHandler addConverter(HtmlToOpenXMLConverter converter) {
        this.converter = converter;
        return this;
    }

    private Tbl createTableWithSettings(CellWrapper[][] cellMatrix, int maxCols, int cellWidthTwips, Element docTable,
                                        WordprocessingMLPackage wordMLPackage) {
        Tbl table = TblFactory.createTable(cellMatrix.length, maxCols, cellWidthTwips);
        TblWidth tableWidth = RunUtils.getObjectFactory().createTblWidth();
        tableWidth.setW(getTableWidthFromStyle(docTable.attr(Constants.STYLE)));
        tableWidth.setType("pct");
        table.getTblPr().setTblW(tableWidth);
        ConverterUtils.fillTblLook(table.getTblPr().getTblLook());
        ConverterUtils.fillTblGrid(table, cellMatrix[0]);
        int i = 0;
        for (CellWrapper[] tblRow : cellMatrix) {
            Tr row = (Tr) table.getContent().get(i);
            i++;
            int d = 0;
            for (CellWrapper tblCol : tblRow) {
                processTableCell(tblCol, row, d, wordMLPackage);
                d++;
            }
        }
        return table;
    }

    private int getMaxColumns(CellWrapper[][] cellMatrix) {
        return Arrays.stream(cellMatrix).mapToInt(cells -> Arrays.stream(cells).mapToInt(cell -> cell.getColspan() == null ? 1 : cell.getColspan().intValueExact()).sum()).max().orElse(0);
    }

    private CellWrapper[][] parseTable(Elements trs) {
        CellWrapper[][] cellMatrix = new CellWrapper[trs.size()][];
        for (int i = 0; i < trs.size(); i++) {
            Elements tds = trs.get(i).select("> td, > th");
            cellMatrix[i] = new CellWrapper[tds.size()];
            for (int j = 0; j < tds.size(); j++) {
                cellMatrix[i][j] = new CellWrapper().setContent(cellContentWithInheritedStyles(tds.get(j))).setStyle(tds.get(j).attr(Constants.STYLE))
                        .setMerge(tds.get(j).attr("merge")).setColspan(parseColspan(tds.get(j).attr("colspan")));
                String merge = cellMatrix[i][j].getMerge();
                if (!strictValidation && !"restart".equals(merge) && !"continue".equals(merge)) {
                    cellMatrix[i][j].setMerge("");
                }
            }
        }
        return cellMatrix;
    }

    private static String cellContentWithInheritedStyles(Element cell) {
        List<String> inheritedStyles = new ArrayList<>();
        // Cells are converted as separate fragments. Keep inherited text declarations in
        // transparent wrappers without re-running outer table or custom tag handlers.
        for (Node source = cell; source != null; source = source.parentNode()) {
            StringBuilder style = new StringBuilder();
            for (String property : List.of("color", "font-size", "font-weight", "font-style")) {
                String value = ConverterUtils.getStyleProperty(source.attr("style"), property);
                if (value != null) style.append(property).append(':').append(value).append(';');
            }
            if (source instanceof Element && (((Element) source).isBlock() || INHERITED_STYLE_TAG.equals(source.nodeName()))) {
                String alignment = ConverterUtils.getStyleProperty(source.attr("style"), "text-align");
                if (alignment != null) style.append("text-align:").append(alignment).append(';');
            }
            if (style.length() > 0) {
                inheritedStyles.add(style.toString());
            }
        }
        if (inheritedStyles.isEmpty()) return cell.html();
        Document fragment = Document.createShell("");
        fragment.outputSettings().prettyPrint(false);
        Element content = fragment.body().appendElement(INHERITED_STYLE_TAG).html(cell.html());
        for (String style : inheritedStyles) {
            Element wrapper = fragment.body().appendElement(INHERITED_STYLE_TAG).attr("style", style);
            wrapper.appendChild(content);
            content = wrapper;
        }
        return content.outerHtml();
    }

    private static BigInteger parseColspan(String value) {
        String digits = value.trim();
        if (digits.isEmpty()) return null;
        int span = 0;
        for (int i = 0; i < digits.length(); i++) {
            char digit = digits.charAt(i);
            if (digit < '0' || digit > '9') return null;
            span = Math.min(1000, span * 10 + digit - '0');
        }
        return span > 1 ? BigInteger.valueOf(span) : null;
    }

    private void processTableCell(CellWrapper tblCol, Tr row, int cellIndex, WordprocessingMLPackage wordMLPackage) {
        Tc column = (Tc) row.getContent().get(cellIndex);
        BigInteger colspan = tblCol.getColspan();
        if (!tblCol.getStyle().isBlank() || !tblCol.getMerge().isBlank() || colspan != null) {
            tblCol.setCellParams(column);
            if (colspan != null) {
                int elementsToRemove = colspan.intValue() - 1;
                int startIndex = row.getContent().size() - elementsToRemove;
                row.getContent().subList(startIndex, row.getContent().size()).clear();
            }
        }
        List<Object> tempContent = new ArrayList<>(wordMLPackage.getMainDocumentPart().getContent());
        wordMLPackage.getMainDocumentPart().getContent().clear();
        try {
            converter.convert(tblCol.getContent(), wordMLPackage);
            ConverterUtils.replaceListContent(column.getContent(), wordMLPackage.getMainDocumentPart().getContent());
            if (column.getContent().isEmpty() || !(column.getContent().get(column.getContent().size() - 1) instanceof org.docx4j.wml.P)) {
                column.getContent().add(RunUtils.getObjectFactory().createP());
            }
        } finally {
            ConverterUtils.replaceListContent(wordMLPackage.getMainDocumentPart().getContent(), tempContent);
        }
    }

    private BigInteger getTableWidthFromStyle(String style) {
        BigInteger width = ConverterUtils.scaleDecimal(ConverterUtils.getPercentWidthFromStyle(style), "50");
        return width != null && width.signum() >= 0 ? width : BigInteger.valueOf(5000);
    }
}
