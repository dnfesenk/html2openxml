package com.denisfesenko.util;

import org.docx4j.wml.JcEnumeration;

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
        if (align == null) {
            return null;
        }

        JcEnumeration jcEnumeration;

        switch (align) {
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
                throw new IllegalArgumentException("Unexpected align value: " + align);
        }

        return jcEnumeration;
    }

    /**
     * Private constructor to prevent instantiation.
     */
    private JcEnumMapper() {
    }
}
