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

    private HtmlToOpenXMLConverter converter;

    @Override
    public void handleTag(Node node, WordprocessingMLPackage wordMLPackage) {
        Element rootNode = (Element) node;
        Element docTable = rootNode.selectFirst("table");
        if (docTable != null) {
            CellWrapper[][] cellMatrix = parseTable(docTable.select("tr"));
            int maxCols = getMaxColumns(cellMatrix);
            int cellWidthTwips = wordMLPackage.getDocumentModel().getSections().get(0).getPageDimensions().getWritableWidthTwips() / maxCols;
            Tbl table = createTableWithSettings(cellMatrix, maxCols, cellWidthTwips, docTable, wordMLPackage);
            if (docTable.attributes().get(Constants.STYLE).toLowerCase().contains(Constants.TABLE_BORDERLESS_STYLE)) {
                ConverterUtils.setBorderlessStyle(table);
            }
            wordMLPackage.getMainDocumentPart().getContent().add(table);
        }
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
        tableWidth.setW(BigInteger.valueOf(docTable.attr(Constants.WIDTH).isBlank()
                ? 5000L : Long.parseLong(docTable.attr(Constants.WIDTH)) * 50L));
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
        return Arrays.stream(cellMatrix).mapToInt(cellWrappers -> cellWrappers.length).max().orElse(cellMatrix[0].length);
    }

    private CellWrapper[][] parseTable(Elements trs) {
        CellWrapper[][] cellMatrix = new CellWrapper[trs.size()][];
        for (int i = 0; i < trs.size(); i++) {
            Elements tds = trs.get(i).select("td");
            cellMatrix[i] = new CellWrapper[tds.size()];
            for (int j = 0; j < tds.size(); j++) {
                cellMatrix[i][j] = new CellWrapper().setContent(tds.get(j).html()).setWidth(tds.get(j).attr(Constants.WIDTH))
                        .setStyle(tds.get(j).attr(Constants.STYLE)).setMerge(tds.get(j).attr("merge"))
                        .setColspan(!tds.get(j).attr("colspan").isBlank()
                                ? new BigInteger(tds.get(j).attr("colspan")) : null);
            }
        }
        return cellMatrix;
    }

    private void processTableCell(CellWrapper tblCol, Tr row, int cellIndex, WordprocessingMLPackage wordMLPackage) {
        Tc column = (Tc) row.getContent().get(cellIndex);
        BigInteger colspan = tblCol.getColspan();
        if (!tblCol.getWidth().isBlank() || !tblCol.getStyle().isBlank() || !tblCol.getMerge().isBlank() || colspan != null) {
            tblCol.setCellParams(column);
            if (colspan != null) {
                int elementsToRemove = colspan.intValue() - 1;
                int startIndex = row.getContent().size() - elementsToRemove;
                row.getContent().subList(startIndex, row.getContent().size()).clear();
            }
        }
        List<Object> tempContent = new ArrayList<>(wordMLPackage.getMainDocumentPart().getContent());
        wordMLPackage.getMainDocumentPart().getContent().clear();
        converter.convert(tblCol.getContent(), wordMLPackage);
        ConverterUtils.replaceListContent(column.getContent(), wordMLPackage.getMainDocumentPart().getContent());
        ConverterUtils.replaceListContent(wordMLPackage.getMainDocumentPart().getContent(), tempContent);
    }

    @Override
    public boolean isRepeatable() {
        return false;
    }
}
