package com.denisfesenko.util;

import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.BinaryPartAbstractImage;

import java.util.IdentityHashMap;
import java.util.Map;

/** Tracks image bytes during a conversion, sharing the count with nested table conversions. */
public final class ImageBudget {
    private static final ThreadLocal<Map<WordprocessingMLPackage, State>> ACTIVE = new ThreadLocal<>();

    private ImageBudget() { }

    /**
     * @param document the target document
     * @return a scope which releases its cached count at the end of the outermost conversion
     */
    public static Scope open(WordprocessingMLPackage document) {
        Map<WordprocessingMLPackage, State> states = ACTIVE.get();
        if (states == null) {
            states = new IdentityHashMap<>();
            ACTIVE.set(states);
        }
        State state = states.computeIfAbsent(document, ignored -> new State());
        state.depth++;
        return new Scope(document, state);
    }

    /**
     * @param document the target document
     * @param incomingBytes decoded bytes about to be added
     * @param limit maximum embedded image bytes
     */
    public static void check(WordprocessingMLPackage document, long incomingBytes, long limit) {
        if (incomingBytes > limit) throw exceeded(limit);
        State state = state(document);
        int parts = document.getParts().getParts().size();
        long used;
        if (state != null && state.valid && state.partCount == parts) {
            used = state.bytes;
        } else {
            used = countImages(document);
            if (state != null) {
                state.bytes = used;
                state.partCount = parts;
                state.valid = true;
            }
        }
        if (used > limit || incomingBytes > limit - used) throw exceeded(limit);
    }

    /**
     * Records an image part immediately after creation, including parts left behind by later failures.
     *
     * @param document the target document
     * @param image the newly created part
     */
    public static void imageAdded(WordprocessingMLPackage document, BinaryPartAbstractImage image) {
        State state = state(document);
        if (state != null && state.valid) {
            state.bytes = Math.addExact(state.bytes, imageBytes(image));
            state.partCount = document.getParts().getParts().size();
        }
    }

    /**
     * Invalidates the count around custom handlers which may change image parts or their bytes.
     *
     * @param document the target document
     */
    public static void invalidate(WordprocessingMLPackage document) {
        State state = state(document);
        if (state != null) state.valid = false;
    }

    private static State state(WordprocessingMLPackage document) {
        Map<WordprocessingMLPackage, State> states = ACTIVE.get();
        return states == null ? null : states.get(document);
    }

    private static long countImages(WordprocessingMLPackage document) {
        long bytes = 0;
        for (var part : document.getParts().getParts().values()) {
            if (part instanceof BinaryPartAbstractImage) {
                BinaryPartAbstractImage image = (BinaryPartAbstractImage) part;
                if (image.getExternalTarget() == null) bytes = Math.addExact(bytes, imageBytes(image));
            }
        }
        return bytes;
    }

    private static int imageBytes(BinaryPartAbstractImage image) {
        var buffer = image.getBuffer();
        if (buffer == null) throw new IllegalStateException("Cannot read an existing image to check the document budget");
        return buffer.remaining();
    }

    private static IllegalArgumentException exceeded(long limit) {
        return new IllegalArgumentException("Embedded images exceed the document limit of " + limit + " bytes");
    }

    private static final class State {
        private long bytes;
        private int partCount;
        private int depth;
        private boolean valid;
    }

    /** A conversion scope; closing the outermost scope discards all cached package state. */
    public static final class Scope implements AutoCloseable {
        private final WordprocessingMLPackage document;
        private final State state;
        private boolean closed;

        private Scope(WordprocessingMLPackage document, State state) {
            this.document = document;
            this.state = state;
        }

        @Override
        public void close() {
            if (closed) return;
            closed = true;
            if (--state.depth == 0) {
                Map<WordprocessingMLPackage, State> states = ACTIVE.get();
                states.remove(document);
                if (states.isEmpty()) ACTIVE.remove();
            }
        }
    }
}
