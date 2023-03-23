package com.denisfesenko.util;

import org.jsoup.nodes.Node;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConverterUtils {

    private ConverterUtils() {
    }

    public static String rgbToHex(String rgb) {
        Pattern c = Pattern.compile("rgb *\\( *([0-9]+), *([0-9]+), *([0-9]+) *\\)");
        Matcher m = c.matcher(rgb);
        if (m.matches()) {
            return String.format("#%02x%02x%02x", Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
        }
        return "#ffffff";
    }

    public static Node findParentNode(Node node, String nodeName) {
        if (node.parentNode() != null) {
            if (node.parentNode().nodeName().equalsIgnoreCase(nodeName)) {
                return node.parentNode();
            } else {
                return findParentNode(node.parentNode(), nodeName);
            }
        }
        return null;
    }
}
