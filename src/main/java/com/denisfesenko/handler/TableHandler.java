package com.denisfesenko.handler;

import com.denisfesenko.converter.HtmlToOpenXMLConverter;
import com.denisfesenko.core.TagHandler;
import com.denisfesenko.util.CellWrapper;
import com.denisfesenko.util.ConverterUtils;
import com.denisfesenko.util.RunUtils;
import org.docx4j.model.table.TblFactory;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.Tbl;
import org.docx4j.wml.TblWidth;
import org.docx4j.wml.Tc;
import org.docx4j.wml.Tr;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

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
            wordMLPackage.getMainDocumentPart().getContent().add(table);
        }
    }

    public TableHandler addConverter(HtmlToOpenXMLConverter converter) {
        this.converter = converter;
        return this;
    }

    private Tbl createTableWithSettings(CellWrapper[][] cellMatrix, int maxCols, int cellWidthTwips, Element docTable, WordprocessingMLPackage wordMLPackage) {
        Tbl table = TblFactory.createTable(cellMatrix.length, maxCols, cellWidthTwips);
        TblWidth tableWidth = RunUtils.getObjectFactory().createTblWidth();
        tableWidth.setW(BigInteger.valueOf(docTable.attr("width").isBlank() ? 5000L : Long.parseLong(docTable.attr("width")) * 50L));
        tableWidth.setType("pct");
        table.getTblPr().setTblW(tableWidth);
        ConverterUtils.fillTblLook(table.getTblPr().getTblLook());
        ConverterUtils.fillTblGrid(table, cellMatrix[0]);
        var i = 0;
        for (CellWrapper[] tblRow : cellMatrix) {
            Tr row = (Tr) table.getContent().get(i);
            i++;
            var d = 0;
            for (CellWrapper tblCol : tblRow) {
                Tc column = (Tc) row.getContent().get(d);
                var colspan = tblCol.getColspan();
                if (!tblCol.getWidth().isBlank() || !tblCol.getStyle().isBlank() || !tblCol.getMerge().isBlank()
                        || colspan != null) {
                    tblCol.setCellParams(column);
                    if (colspan != null) {
                        IntStream.range(1, colspan.intValue()).forEach(value -> row.getContent().remove(row.getContent().size() - 1));
                    }
                }
                d++;
                List<Object> tempContent = new ArrayList<>(wordMLPackage.getMainDocumentPart().getContent());
                wordMLPackage.getMainDocumentPart().getContent().clear();
                converter.traverseDocument(Jsoup.parseBodyFragment(tblCol.getContent()), wordMLPackage);
                column.getContent().clear();
                column.getContent().addAll(wordMLPackage.getMainDocumentPart().getContent());
                wordMLPackage.getMainDocumentPart().getContent().clear();
                wordMLPackage.getMainDocumentPart().getContent().addAll(tempContent);
            }
        }
        return table;
    }

    private int getMaxColumns(CellWrapper[][] cellMatrix) {
        return Arrays.stream(cellMatrix).mapToInt(cellWrappers -> cellWrappers.length).max().orElse(cellMatrix[0].length);
    }

    private CellWrapper[][] parseTable(Elements trs) {
        CellWrapper[][] cellMatrix = new CellWrapper[trs.size()][];
        for (var i = 0; i < trs.size(); i++) {
            Elements tds = trs.get(i).select("td");
            cellMatrix[i] = new CellWrapper[tds.size()];
            for (var j = 0; j < tds.size(); j++) {
                cellMatrix[i][j] = new CellWrapper().setContent(tds.get(j).html()).setWidth(tds.get(j).attr("width"))
                        .setStyle(tds.get(j).attr("style")).setAlign(tds.get(j).attr("align"))
                        .setMerge(tds.get(j).attr("merge")).setColspan(!tds.get(j).attr("colspan").isBlank()
                                ? new BigInteger(tds.get(j).attr("colspan")) : null);
            }
        }
        return cellMatrix;
    }

    @Override
    public boolean isRepeatable() {
        return false;
    }
}
