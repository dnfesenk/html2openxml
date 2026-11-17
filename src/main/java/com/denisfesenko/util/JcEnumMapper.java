package com.denisfesenko.util;

import org.docx4j.wml.JcEnumeration;

import java.util.Locale;

/**
 * JcEnumMapper is a utility class that maps string values to their corresponding JcEnumeration constants.
 */
public class JcEnumMapper {

    /**
     * Maps the given string value to the corresponding JcEnumeration constant.
     *
     * @param align The string value representing a JcEnumeration constant.
     * @return The JcEnumeration constant that matches the given string value or null if the input is null.
     * @throws IllegalArgumentException If the input string doesn't match any JcEnumeration constant.
     */
    public static JcEnumeration map(String align) {
        return map(align, true);
    }

    /**
     * Maps an alignment, optionally ignoring unknown values.
     *
     * @param align alignment to normalize and map
     * @param strictValidation whether unknown values cause an exception
     * @return the mapped alignment, or null for absent or ignored values
     * @throws IllegalArgumentException if strict validation is enabled and the value is unknown
     */
    public static JcEnumeration map(String align, boolean strictValidation) {
        if (align == null) {
            return null;
        }

        JcEnumeration jcEnumeration;

        switch (align.trim().toLowerCase(Locale.ROOT)) {
            case "left":
                jcEnumeration = JcEnumeration.LEFT;
                break;
            case "right":
                jcEnumeration = JcEnumeration.RIGHT;
                break;
            case "center":
                jcEnumeration = JcEnumeration.CENTER;
                break;
            case "justify":
                jcEnumeration = JcEnumeration.BOTH;
                break;
            default:
                if (strictValidation) throw new IllegalArgumentException("Unexpected align value: " + align);
                return null;
        }

        return jcEnumeration;
    }

    /**
     * Private constructor to prevent instantiation.
     */
    private JcEnumMapper() {
    }
}
