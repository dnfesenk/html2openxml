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
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

/**
 * This class handles the conversion of HTML tables to WordprocessingMLPackage format.
 */
public class TableHandler implements TagHandler {

    /**
     * Reserved implementation tag for inherited cell styles; not a custom-handler extension point.
     */
    public static final String INHERITED_STYLE_TAG = "html2openxml-inherited-style";

    private static final int MAX_TABLE_GRID_CELLS = 100_000;

    private final boolean strictValidation;
    private final ThreadLocal<TableInvocation> invocation = new ThreadLocal<>();
    private volatile HtmlToOpenXMLConverter standaloneConverter;

    private static final class TableInvocation {
        private final Node node;
        private final WordprocessingMLPackage target;
        private final HtmlToOpenXMLConverter converter;

        private TableInvocation(Node node, WordprocessingMLPackage target, HtmlToOpenXMLConverter converter) {
            this.node = node;
            this.target = target;
            this.converter = converter;
        }
    }

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

    /**
     * Converts a table using the current invocation's converter or the converter
     * configured for standalone calls with {@link #addConverter}.
     *
     * @param node          The input HTML node to be processed.
     * @param wordMLPackage The WordprocessingMLPackage instance where the converted content
     *                      will be added.
     */
    @Override
    public void handleTag(Node node, WordprocessingMLPackage wordMLPackage) {
        TableInvocation current = invocation.get();
        HtmlToOpenXMLConverter converter = current == null ? null : current.converter;
        renderTable(node, wordMLPackage, converter != null ? converter : standaloneConverter);
    }

    /**
     * Converts a table using the converter associated with this invocation.
     * The default implementation delegates to the two-argument overload so existing
     * subclasses can customize table handling through either overload.
     *
     * @param node the HTML table node
     * @param wordMLPackage the target document
     * @param converter the converter to use for table cell content
     */
    public void handleTag(Node node, WordprocessingMLPackage wordMLPackage, HtmlToOpenXMLConverter converter) {
        TableInvocation previous = invocation.get();
        // A two-argument override can delegate back to this overload for the same table.
        if (previous != null && previous.node == node && previous.target == wordMLPackage) {
            renderTable(node, wordMLPackage, converter != null ? converter : standaloneConverter);
            return;
        }
        invocation.set(new TableInvocation(node, wordMLPackage, converter));
        try {
            handleTag(node, wordMLPackage);
        } finally {
            if (previous == null) {
                invocation.remove();
            } else {
                invocation.set(previous);
            }
        }
    }

    private void renderTable(Node node, WordprocessingMLPackage wordMLPackage, HtmlToOpenXMLConverter converter) {
        Element rootNode = (Element) node;
        Element docTable = rootNode.selectFirst("table");
        if (docTable != null) {
            CellWrapper[][] cellMatrix = parseTable(new Elements(docTable.select("tr").stream()
                    .filter(tr -> tr.closest("table") == docTable).collect(java.util.stream.Collectors.toList())));
            int maxCols = getMaxColumns(cellMatrix);
            if (maxCols == 0) return;
            int cellWidthTwips = wordMLPackage.getDocumentModel().getSections().get(0).getPageDimensions().getWritableWidthTwips() / maxCols;
            Tbl table = createTableWithSettings(cellMatrix, maxCols, cellWidthTwips, docTable, wordMLPackage, converter);
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
     * Sets the converter used by the two-argument handleTag method.
     *
     * @param converter The HtmlToOpenXMLConverter instance.
     * @return The TableHandler instance.
     */
    public TableHandler addConverter(HtmlToOpenXMLConverter converter) {
        this.standaloneConverter = converter;
        return this;
    }

    private Tbl createTableWithSettings(CellWrapper[][] cellMatrix, int maxCols, int cellWidthTwips, Element docTable,
                                        WordprocessingMLPackage wordMLPackage, HtmlToOpenXMLConverter converter) {
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
                processTableCell(tblCol, row, d, wordMLPackage, converter);
                d++;
            }
        }
        return table;
    }

    private int getMaxColumns(CellWrapper[][] cellMatrix) {
        return Arrays.stream(cellMatrix)
                .mapToInt(row -> Arrays.stream(row).mapToInt(TableHandler::columnSpan).sum())
                .max().orElse(0);
    }

    private CellWrapper[][] parseTable(Elements trs) {
        CellWrapper[][] cellMatrix = new CellWrapper[trs.size()][];
        if (trs.isEmpty()) return cellMatrix;

        // TblFactory allocates a rectangular grid, including slots covered by spans.
        int columnLimit = MAX_TABLE_GRID_CELLS / trs.size();
        NavigableMap<Integer, VerticalSpan> activeSpans = new TreeMap<>();
        CellWrapper blank = new CellWrapper().setStyle("").setMerge("");
        int groupEnd = 0;
        for (int i = 0; i < trs.size(); i++) {
            Element tr = trs.get(i);
            if (i == groupEnd) {
                do {
                    groupEnd++;
                } while (groupEnd < trs.size() && trs.get(groupEnd).parent() == tr.parent());
            }
            int rowIndex = i;
            activeSpans.values().removeIf(span -> span.endRow <= rowIndex);
            List<CellWrapper> row = new ArrayList<>();
            int column = 0;
            for (Element td : tr.select("> td, > th")) {
                BigInteger colspan = parseColspan(td.attr("colspan"));
                int width = colspan == null ? 1 : colspan.intValueExact();
                Map.Entry<Integer, VerticalSpan> next = activeSpans.ceilingEntry(column);
                // Keep a cell contiguous even if its colspan would overlap a vertical span.
                while (next != null && next.getKey() < column + width) {
                    column = appendContinuation(row, column, next, blank);
                    next = activeSpans.ceilingEntry(column);
                }
                if (column + width > columnLimit) {
                    throw new IllegalArgumentException("Table exceeds the limit of " + MAX_TABLE_GRID_CELLS + " grid cells");
                }

                CellWrapper cell = new CellWrapper().setContent(cellContentWithInheritedStyles(td))
                        .setStyle(td.attr(Constants.STYLE)).setMerge(td.attr("merge")).setColspan(colspan);
                String merge = cell.getMerge();
                if (!merge.isBlank() && !"restart".equals(merge) && !"continue".equals(merge)) {
                    if (strictValidation) {
                        throw new IllegalArgumentException("Merge value must be restart or continue");
                    }
                    cell.setMerge("");
                }
                int rowspan = parseSpan(td.attr("rowspan"), 65_534);
                int remainingRows = groupEnd - i;
                int height = rowspan == 0 ? remainingRows : Math.min(rowspan, remainingRows);
                if (height > 1) {
                    cell.setMerge("restart");
                    CellWrapper continuation = new CellWrapper().setStyle(cell.getStyle())
                            .setMerge("continue").setColspan(colspan);
                    activeSpans.put(column, new VerticalSpan(i + height, continuation));
                }
                row.add(cell);
                column += width;
            }
            for (Map.Entry<Integer, VerticalSpan> next : activeSpans.tailMap(column).entrySet()) {
                column = appendContinuation(row, column, next, blank);
            }
            cellMatrix[i] = row.toArray(new CellWrapper[0]);
        }
        return cellMatrix;
    }

    private static int appendContinuation(List<CellWrapper> row, int column,
                                          Map.Entry<Integer, VerticalSpan> span, CellWrapper blank) {
        while (column < span.getKey()) {
            row.add(blank);
            column++;
        }
        CellWrapper continuation = span.getValue().continuation;
        row.add(continuation);
        return column + columnSpan(continuation);
    }

    private static int columnSpan(CellWrapper cell) {
        return cell.getColspan() == null ? 1 : cell.getColspan().intValueExact();
    }

    private static final class VerticalSpan {
        private final int endRow;
        private final CellWrapper continuation;

        private VerticalSpan(int endRow, CellWrapper continuation) {
            this.endRow = endRow;
            this.continuation = continuation;
        }
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
        int span = parseSpan(value, 1000);
        return span > 1 ? BigInteger.valueOf(span) : null;
    }

    private static int parseSpan(String value, int limit) {
        String digits = value.trim();
        if (digits.isEmpty()) return 1;
        int span = 0;
        for (int i = 0; i < digits.length(); i++) {
            char digit = digits.charAt(i);
            if (digit < '0' || digit > '9') return 1;
            span = Math.min(limit, span * 10 + digit - '0');
        }
        return span;
    }

    private void processTableCell(CellWrapper tblCol, Tr row, int cellIndex,
                                  WordprocessingMLPackage wordMLPackage, HtmlToOpenXMLConverter converter) {
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
        if (tblCol.getContent() == null) return;
        Objects.requireNonNull(converter, "Call addConverter or pass a converter to handleTag");
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
