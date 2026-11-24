package com.denisfesenko.util;

import org.docx4j.XmlUtils;
import org.docx4j.openpackaging.exceptions.InvalidFormatException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.StyleDefinitionsPart;
import org.docx4j.wml.BooleanDefaultTrue;
import org.docx4j.wml.ObjectFactory;
import org.docx4j.wml.P;
import org.docx4j.wml.PPr;
import org.docx4j.wml.R;
import org.docx4j.wml.RPr;
import org.docx4j.wml.Style;

import java.util.List;
import java.util.function.Consumer;
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
     * Returns the main document's style definitions part, creating it with the built-in
     * Normal paragraph style if the document has no style definitions part.
     *
     * @param wordPackage the document package whose styles to access
     * @return the existing or newly attached style definitions part
     * @throws IllegalStateException if a new styles part cannot be created or attached
     */
    public static StyleDefinitionsPart getOrCreateStyles(WordprocessingMLPackage wordPackage) {
        var main = wordPackage.getMainDocumentPart();
        var styles = main.getStyleDefinitionsPart();
        if (styles != null) return styles;

        try {
            styles = new StyleDefinitionsPart();
            var definitions = OBJECT_FACTORY.createStyles();
            definitions.getStyle().add(XmlUtils.deepCopy(StyleDefinitionsPart.getKnownStyles().get("Normal")));
            styles.setJaxbElement(definitions);
            main.addTargetPart(styles);
            return styles;
        } catch (InvalidFormatException e) {
            throw new IllegalStateException("Unable to create Word styles part", e);
        }
    }

    /**
     * Finds the first style of the requested type whose name matches case-insensitively
     * and whose ID is nonblank.
     *
     * @param styles the style definitions to search
     * @param type the Word style type, such as {@code paragraph} or {@code character}
     * @param name the style name to match
     * @param defaultId the ID to return when there is no matching style with a usable ID
     * @return the matched style ID or {@code defaultId}
     */
    public static String findStyleId(StyleDefinitionsPart styles, String type, String name, String defaultId) {
        return styles.getJaxbElement().getStyle().stream()
                .filter(style -> type.equals(style.getType()) && style.getName() != null)
                .filter(style -> name.equalsIgnoreCase(style.getName().getVal()))
                .map(Style::getStyleId)
                .filter(id -> id != null && !id.isBlank())
                .findFirst().orElse(defaultId);
    }

    /**
     * Resolves a built-in style against a template without replacing an incompatible style.
     *
     * @param styles the document styles
     * @param type the required Word style type
     * @param name the name used to recognize a matching template style
     * @param defaultId the built-in style ID
     * @param prepare customization applied to a new copy of the built-in style
     * @return the ID to use in the document
     */
    public static String getOrCreateStyleId(StyleDefinitionsPart styles, String type, String name,
                                            String defaultId, Consumer<Style> prepare) {
        String namedId = findStyleId(styles, type, name, null);
        if (namedId != null) return namedId;

        Style existing = styles.getStyleById(defaultId);
        if (existing != null && type.equals(existing.getType())) return defaultId;

        String styleId = defaultId;
        for (int suffix = 1; styles.getStyleById(styleId) != null; suffix++) {
            styleId = defaultId + "_" + suffix;
        }

        Style style = XmlUtils.deepCopy(StyleDefinitionsPart.getKnownStyles().get(defaultId));
        style.setStyleId(styleId);
        if (style.getBasedOn() != null) {
            Style base = styles.getStyleById(style.getBasedOn().getVal());
            if (base == null || !type.equals(base.getType())) {
                style.setBasedOn(null);
            }
        }
        prepare.accept(style);
        styles.getJaxbElement().getStyle().add(style);
        return styleId;
    }

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
