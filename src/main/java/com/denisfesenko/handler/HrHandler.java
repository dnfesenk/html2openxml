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

/**
 * The HrHandler class is responsible for processing the HTML horizontal rule tag (&lt;hr&gt;).
 * It implements the TagHandler interface.
 */
public class HrHandler implements TagHandler {

    private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();

    /**
     * Handles the HTML horizontal rule tag by creating a horizontal rule in the
     * provided WordprocessingMLPackage.
     *
     * @param node          The HTML node representing the horizontal rule tag.
     * @param wordMLPackage The WordprocessingMLPackage to which the horizontal rule is added.
     */
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

    /**
     * Indicates whether the HrHandler can be applied multiple times to the same content.
     * In this case, it returns false, as the horizontal rule should not be applied multiple times.
     *
     * @return A boolean value, true if the handler is repeatable, false otherwise.
     */
    @Override
    public boolean isRepeatable() {
        return false;
    }
}
