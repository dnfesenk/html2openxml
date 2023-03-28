package com.denisfesenko.util;

import org.apache.commons.lang3.StringUtils;
import org.docx4j.wml.CTShd;
import org.docx4j.wml.STShd;
import org.docx4j.wml.TblWidth;
import org.docx4j.wml.Tc;
import org.docx4j.wml.TcPr;
import org.docx4j.wml.TcPrInner;

import java.math.BigInteger;
import java.util.Optional;

public class CellWrapper {
    private String width;
    private String content;
    private String style;
    private String merge;
    private BigInteger colspan;

    public String getWidth() {
        return width;
    }

    public CellWrapper setWidth(String width) {
        this.width = width;
        return this;
    }

    public String getContent() {
        return content;
    }

    public CellWrapper setContent(String content) {
        this.content = content;
        return this;
    }

    public String getStyle() {
        return style;
    }

    public CellWrapper setStyle(String style) {
        this.style = style;
        return this;
    }

    public String getMerge() {
        return merge;
    }

    public CellWrapper setMerge(String merge) {
        this.merge = merge;
        return this;
    }

    public BigInteger getColspan() {
        return colspan;
    }

    public CellWrapper setColspan(BigInteger colspan) {
        this.colspan = colspan;
        return this;
    }

    public void setCellParams(Tc tableCell) {
        TcPr tableCellProperties = RunUtils.getObjectFactory().createTcPr();
        setTableCellWidth(tableCellProperties);
        setTableCellStyle(tableCellProperties);
        setTableCellMerge(tableCellProperties);
        setTableCellColspan(tableCellProperties);
        tableCell.setTcPr(tableCellProperties);
    }

    private void setTableCellWidth(TcPr tableCellProperties) {
        Optional.ofNullable(width)
                .filter(w -> !w.isBlank())
                .ifPresent(w -> {
                    TblWidth tableWidth = RunUtils.getObjectFactory().createTblWidth();
                    tableWidth.setW(BigInteger.valueOf(Integer.parseInt(w) * 50L));
                    tableWidth.setType("pct");
                    tableCellProperties.setTcW(tableWidth);
                });
    }

    private void setTableCellStyle(TcPr tableCellProperties) {
        Optional.ofNullable(style)
                .filter(s -> !s.isBlank())
                .ifPresent(s -> {
                    CTShd shd = RunUtils.getObjectFactory().createCTShd();
                    shd.setVal(STShd.CLEAR);
                    shd.setColor("auto");
                    shd.setFill(StringUtils.substringBetween(s, "background-color: ", ";"));
                    tableCellProperties.setShd(shd);
                });
    }

    private void setTableCellMerge(TcPr tableCellProperties) {
        Optional.ofNullable(merge)
                .filter(m -> !m.isBlank())
                .ifPresent(m -> {
                    TcPrInner.VMerge vMerge = new TcPrInner.VMerge();
                    if ("restart".equals(m) || "continue".equals(m)) {
                        vMerge.setVal(m);
                    } else {
                        throw new IllegalArgumentException("Merge value must be restart or continue");
                    }
                    tableCellProperties.setVMerge(vMerge);
                });
    }

    private void setTableCellColspan(TcPr tableCellProperties) {
        Optional.ofNullable(colspan)
                .ifPresent(c -> {
                    TcPrInner.GridSpan gridSpan = new TcPrInner.GridSpan();
                    gridSpan.setVal(c);
                    tableCellProperties.setGridSpan(gridSpan);
                });
    }
}
