package com.denisfesenko.handler;

import com.denisfesenko.core.TagHandler;
import com.denisfesenko.util.RunUtils;
import jakarta.xml.bind.JAXBElement;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.vml.CTRect;
import org.docx4j.vml.ObjectFactory;
import org.docx4j.vml.officedrawing.STHrAlign;
import org.docx4j.vml.officedrawing.STTrueFalse;
import org.docx4j.wml.Pict;
import org.jsoup.nodes.Node;

public class HrHandler implements TagHandler {

    private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();

    @Override
    public void handleTag(Node node, WordprocessingMLPackage wordMLPackage) {
        Pict pict = RunUtils.getObjectFactory().createPict();
        CTRect rect = OBJECT_FACTORY.createCTRect();
        rect.setStyle("width:0;height:1.5pt");
        rect.setHralign(STHrAlign.CENTER);
        rect.setHrstd(STTrueFalse.T);
        rect.setHr(STTrueFalse.T);
        rect.setFillcolor("#a0a0a0");
        rect.setStroked(org.docx4j.vml.STTrueFalse.F);
        JAXBElement<CTRect> rectWrapped = OBJECT_FACTORY.createRect(rect);
        pict.getAnyAndAny().add(rectWrapped);
        RunUtils.getCurrentRun(wordMLPackage).getContent().add(pict);
    }

    @Override
    public boolean isRepeatable() {
        return false;
    }
}
