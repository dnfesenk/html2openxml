package com.denisfesenko.util;

import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.BooleanDefaultTrue;
import org.docx4j.wml.ObjectFactory;
import org.docx4j.wml.P;
import org.docx4j.wml.R;
import org.docx4j.wml.RPr;

import java.util.List;

public class RunUtils {

    private RunUtils() {
    }

    private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();

    public static P getCurrentParagraph(WordprocessingMLPackage wordMLPackage) {
        List<Object> content = wordMLPackage.getMainDocumentPart().getContent();
        if (content.isEmpty()) {
            createParagraph(wordMLPackage);
        }
        Object lastElement = content.get(content.size() - 1);
        return lastElement instanceof P ? (P) lastElement : null;
    }

    public static R getCurrentRun(WordprocessingMLPackage wordMLPackage) {
        R result = null;
        P lastElement = getCurrentParagraph(wordMLPackage);
        if (lastElement != null) {
            List<Object> paragraphContent = lastElement.getContent();
            if (paragraphContent.isEmpty()) {
                lastElement.getContent().add(OBJECT_FACTORY.createR());
            }
            Object lastElementOfParagraph = paragraphContent.get(paragraphContent.size() - 1);
            result = lastElementOfParagraph instanceof R ? (R) lastElementOfParagraph : null;
        }
        return result;
    }

    public static RPr getCurrentRPr(WordprocessingMLPackage wordMLPackage) {
        RPr result = null;
        R run = getCurrentRun(wordMLPackage);
        if (run != null) {
            if (run.getRPr() == null) {
                RPr rPr = OBJECT_FACTORY.createRPr();
                run.setRPr(rPr);
                result = rPr;
            } else {
                result = run.getRPr();
            }
        }
        return result;
    }

    public static void createParagraph(WordprocessingMLPackage wordMLPackage) {
        P paragraph = OBJECT_FACTORY.createP();
        wordMLPackage.getMainDocumentPart().getContent().add(paragraph);
    }

    public static BooleanDefaultTrue createBooleanDefaultTrue() {
        BooleanDefaultTrue booleanDefaultTrue = OBJECT_FACTORY.createBooleanDefaultTrue();
        booleanDefaultTrue.setVal(true);
        return booleanDefaultTrue;
    }
}
