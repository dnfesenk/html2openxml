package com.denisfesenko.util;

import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.ObjectFactory;
import org.docx4j.wml.P;
import org.docx4j.wml.R;
import org.docx4j.wml.Text;

public class RunUtils {

    private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();

    public static R createRun(String text) {
        R run = OBJECT_FACTORY.createR();
        Text runText = OBJECT_FACTORY.createText();
        runText.setValue(text);
        runText.setSpace("preserve");
        run.getContent().add(runText);
        return run;
    }

    public static void addRunToParagraph(R run, WordprocessingMLPackage wordMLPackage) {
        P currentParagraph = getCurrentParagraph(wordMLPackage);
        if (currentParagraph != null) {
            currentParagraph.getContent().add(run);
        }
    }

    public static void createParagraph(WordprocessingMLPackage wordMLPackage) {
        P paragraph = OBJECT_FACTORY.createP();
        wordMLPackage.getMainDocumentPart().getContent().add(paragraph);
    }

    private static P getCurrentParagraph(WordprocessingMLPackage wordMLPackage) {
        if (wordMLPackage.getMainDocumentPart().getContent().isEmpty()) {
            createParagraph(wordMLPackage);
        }
        Object lastElement = wordMLPackage.getMainDocumentPart().getContent()
                .get(wordMLPackage.getMainDocumentPart().getContent().size() - 1);

        return lastElement instanceof P ? (P) lastElement : null;
    }
}
