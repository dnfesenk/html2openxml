package com.denisfesenko.util;

import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.BooleanDefaultTrue;
import org.docx4j.wml.ObjectFactory;
import org.docx4j.wml.P;
import org.docx4j.wml.PPr;
import org.docx4j.wml.R;
import org.docx4j.wml.RPr;

import java.util.List;
import java.util.function.Function;

/**
 * The RunUtils class provides utility methods for working with runs in WordprocessingMLPackage.
 * This class is not meant to be instantiated.
 */
public final class RunUtils {

    /**
     * The ObjectFactory instance used to create new elements.
     */
    private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();

    /**
     * Creates a new paragraph element with the given paragraph properties and adds it to the main document part of the
     * WordprocessingMLPackage.
     *
     * @param wordMLPackage the WordprocessingMLPackage to add the paragraph to
     * @param pPr           the paragraph properties to set on the paragraph
     */
    public static void createParagraph(WordprocessingMLPackage wordMLPackage, PPr pPr) {
        P paragraph = OBJECT_FACTORY.createP();
        if (pPr != null) {
            paragraph.setPPr(pPr);
        }
        wordMLPackage.getMainDocumentPart().getContent().add(paragraph);
    }

    /**
     * Gets the current paragraph element from the main document part of the WordprocessingMLPackage. If the main
     * document part is empty, a new paragraph element is created and returned.
     *
     * @param wordMLPackage the WordprocessingMLPackage to get the current paragraph from
     * @return the current paragraph element
     */
    public static P getCurrentParagraph(WordprocessingMLPackage wordMLPackage) {
        List<Object> content = wordMLPackage.getMainDocumentPart().getContent();
        if (content.isEmpty()) {
            return createAndAddParagraph(wordMLPackage);
        }
        Object lastElement = content.get(content.size() - 1);
        if (lastElement instanceof P) {
            return (P) lastElement;
        }
        return createAndAddParagraph(wordMLPackage);
    }

    /**
     * Gets the current run element from the current paragraph element in the main document part of the
     * WordprocessingMLPackage. If the current paragraph is empty, a new run element is created and returned.
     *
     * @param wordMLPackage the WordprocessingMLPackage to get the current run from
     * @return the current run element
     */
    public static R getCurrentRun(WordprocessingMLPackage wordMLPackage) {
        P lastElement = getCurrentParagraph(wordMLPackage);
        List<Object> paragraphContent = lastElement.getContent();
        if (paragraphContent.isEmpty()) {
            return createAndAddRun(lastElement);
        }
        Object lastElementOfParagraph = paragraphContent.get(paragraphContent.size() - 1);
        return lastElementOfParagraph instanceof R ? (R) lastElementOfParagraph : createAndAddRun(lastElement);
    }

    /**
     * Gets the current run properties from the current run element in the main document part of the WordprocessingMLPackage.
     * If the current run has no properties, a new RPr element is created and set on the current run element.
     *
     * @param wordMLPackage the WordprocessingMLPackage to get the current run properties from
     * @return the current run properties
     */
    public static RPr getCurrentRPr(WordprocessingMLPackage wordMLPackage) {
        R run = getCurrentRun(wordMLPackage);
        if (run.getRPr() == null) {
            RPr rPr = OBJECT_FACTORY.createRPr();
            run.setRPr(rPr);
            return rPr;
        }
        return run.getRPr();
    }

    /**
     * Creates a new BooleanDefaultTrue element with the value set to true.
     *
     * @return a new BooleanDefaultTrue element with the value set to true
     */
    public static BooleanDefaultTrue createBooleanDefaultTrue() {
        BooleanDefaultTrue booleanDefaultTrue = OBJECT_FACTORY.createBooleanDefaultTrue();
        booleanDefaultTrue.setVal(true);
        return booleanDefaultTrue;
    }

    /**
     * Gets the ObjectFactory instance used by this class to create new elements.
     *
     * @return the ObjectFactory instance used by this class
     */
    public static ObjectFactory getObjectFactory() {
        return OBJECT_FACTORY;
    }

    /**
     * Creates a new paragraph element and adds it to the main document part of the WordprocessingMLPackage.
     *
     * @param wordMLPackage the WordprocessingMLPackage to add the paragraph to
     * @return the new paragraph element
     */
    private static P createAndAddParagraph(WordprocessingMLPackage wordMLPackage) {
        return createAndAddElement(wordMLPackage.getMainDocumentPart().getContent(), ObjectFactory::createP);
    }

    /**
     * Creates a new run element and adds it to the given paragraph element.
     *
     * @param paragraph the paragraph element to add the run to
     * @return the new run element
     */
    private static R createAndAddRun(P paragraph) {
        return createAndAddElement(paragraph.getContent(), ObjectFactory::createR);
    }

    /**
     * Creates a new element using the given factory method, adds it to the given list, and returns it.
     *
     * @param contentList   the list to add the new element to
     * @param factoryMethod the factory method to use to create the new element
     * @param <T>           the type of the new element
     * @return the new element
     */
    private static <T> T createAndAddElement(List<Object> contentList, Function<ObjectFactory, T> factoryMethod) {
        T element = factoryMethod.apply(OBJECT_FACTORY);
        contentList.add(element);
        return element;
    }

    /**
     * Private constructor to prevent instantiation.
     */
    private RunUtils() {
    }
}
