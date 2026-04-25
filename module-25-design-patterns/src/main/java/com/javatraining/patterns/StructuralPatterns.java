package com.javatraining.patterns;

import java.util.*;
import java.util.function.Function;

/**
 * Module 25 - Structural Patterns
 *
 * Structural patterns compose objects and classes into larger structures
 * while keeping those structures flexible and efficient.
 *
 * Patterns covered:
 *   Adapter    - convert incompatible interfaces
 *   Decorator  - add behaviour without subclassing
 *   Facade     - simple interface over a complex subsystem
 *   Composite  - tree structure of uniform leaf/branch nodes
 *   Proxy      - controlled access / lazy loading / caching
 *   Bridge     - decouple abstraction from implementation
 *   Flyweight  - share state to reduce memory for many fine-grained objects
 */
public class StructuralPatterns {

    // ── Adapter ───────────────────────────────────────────────────────────────

    /**
     * Adapter wraps an existing class with the interface the client expects.
     * "Make the old code fit the new interface without changing it."
     */
    public interface TemperatureSource {
        double getCelsius();
    }

    /** Legacy library - returns Fahrenheit, can't be changed. */
    public static class LegacyThermometer {
        private final double fahrenheit;
        public LegacyThermometer(double fahrenheit) { this.fahrenheit = fahrenheit; }
        public double getFahrenheit() { return fahrenheit; }
    }

    /** Adapter: wraps LegacyThermometer and exposes TemperatureSource. */
    public static class ThermometerAdapter implements TemperatureSource {
        private final LegacyThermometer legacy;
        public ThermometerAdapter(LegacyThermometer t) { this.legacy = t; }
        @Override public double getCelsius() {
            return (legacy.getFahrenheit() - 32) * 5.0 / 9.0;
        }
    }

    /** Object adapter using a lambda - the functional version. */
    public static TemperatureSource adapt(LegacyThermometer t) {
        return () -> (t.getFahrenheit() - 32) * 5.0 / 9.0;
    }

    // ── Decorator ─────────────────────────────────────────────────────────────

    /**
     * Decorator adds behaviour by wrapping the original object.
     * Prefers composition over inheritance - each decorator adds one concern.
     * Java I/O streams are the classic example: FileInputStream → BufferedInputStream → GZIPInputStream.
     */
    public interface TextProcessor {
        String process(String text);
    }

    public static class IdentityProcessor implements TextProcessor {
        @Override public String process(String text) { return text; }
    }

    /** Base decorator: delegates to wrapped processor. */
    public abstract static class TextProcessorDecorator implements TextProcessor {
        protected final TextProcessor wrapped;
        protected TextProcessorDecorator(TextProcessor wrapped) { this.wrapped = wrapped; }
    }

    public static class TrimDecorator extends TextProcessorDecorator {
        public TrimDecorator(TextProcessor p) { super(p); }
        @Override public String process(String text) { return wrapped.process(text.strip()); }
    }

    public static class UpperCaseDecorator extends TextProcessorDecorator {
        public UpperCaseDecorator(TextProcessor p) { super(p); }
        @Override public String process(String text) { return wrapped.process(text).toUpperCase(); }
    }

    public static class PrefixDecorator extends TextProcessorDecorator {
        private final String prefix;
        public PrefixDecorator(TextProcessor p, String prefix) { super(p); this.prefix = prefix; }
        @Override public String process(String text) { return prefix + wrapped.process(text); }
    }

    public static class SuffixDecorator extends TextProcessorDecorator {
        private final String suffix;
        public SuffixDecorator(TextProcessor p, String suffix) { super(p); this.suffix = suffix; }
        @Override public String process(String text) { return wrapped.process(text) + suffix; }
    }

    /** Functional decorator factory using Function composition. */
    public static TextProcessor decorate(TextProcessor base, Function<String, String>... transforms) {
        TextProcessor result = base;
        for (Function<String, String> fn : transforms) {
            final TextProcessor prev = result;
            result = text -> fn.apply(prev.process(text));
        }
        return result;
    }

    // ── Facade ────────────────────────────────────────────────────────────────

    /**
     * Facade provides a simple interface to a complex subsystem.
     * Hides the complexity; clients only interact with the facade.
     */
    public static class VideoEncoder  {
        public byte[] encode(byte[] raw, String codec) { return raw; /* stub */ }
    }
    public static class AudioNormaliser {
        public byte[] normalise(byte[] audio) { return audio; /* stub */ }
    }
    public static class ThumbnailGenerator {
        public byte[] generate(byte[] video, int second) { return new byte[100]; /* stub */ }
    }
    public static class MetadataWriter {
        public void write(String path, Map<String, String> meta) { /* stub */ }
    }

    /** Facade: single uploadVideo call orchestrates four subsystems. */
    public static class VideoUploadFacade {
        private final VideoEncoder      encoder   = new VideoEncoder();
        private final AudioNormaliser   audio     = new AudioNormaliser();
        private final ThumbnailGenerator thumbs   = new ThumbnailGenerator();
        private final MetadataWriter    metadata  = new MetadataWriter();

        public record UploadResult(String videoId, boolean thumbnailGenerated) {}

        public UploadResult upload(String title, byte[] rawVideo, byte[] rawAudio) {
            byte[] encoded   = encoder.encode(rawVideo, "H264");
            byte[] normAudio = audio.normalise(rawAudio);
            byte[] thumb     = thumbs.generate(encoded, 5);
            String videoId   = UUID.randomUUID().toString();
            metadata.write(videoId, Map.of("title", title));
            return new UploadResult(videoId, thumb.length > 0);
        }
    }

    // ── Composite ─────────────────────────────────────────────────────────────

    /**
     * Composite lets clients treat single objects and compositions uniformly.
     * Classic example: file system (File and Directory both implement FileSystemNode).
     */
    public interface FileSystemNode {
        String  name();
        long    size();
        String  display(int indent);
    }

    public record FileNode(String name, long size) implements FileSystemNode {
        @Override public String display(int indent) {
            return " ".repeat(indent) + name + " (" + size + " bytes)";
        }
    }

    public static class DirectoryNode implements FileSystemNode {
        private final String name;
        private final List<FileSystemNode> children = new ArrayList<>();

        public DirectoryNode(String name) { this.name = name; }

        public DirectoryNode add(FileSystemNode node) { children.add(node); return this; }

        @Override public String name() { return name; }
        @Override public long   size() { return children.stream().mapToLong(FileSystemNode::size).sum(); }

        @Override public String display(int indent) {
            StringBuilder sb = new StringBuilder(" ".repeat(indent)).append(name).append("/\n");
            for (FileSystemNode child : children) sb.append(child.display(indent + 2)).append("\n");
            return sb.toString().stripTrailing();
        }
    }

    // ── Proxy ─────────────────────────────────────────────────────────────────

    /**
     * Proxy controls access to the real subject.
     * Types:
     *   Virtual proxy  - lazy initialisation (create the heavy object on first use)
     *   Caching proxy  - memoize results
     *   Protection proxy - access control
     *   Remote proxy   - represents an object in another JVM/network
     */
    public interface ImageLoader {
        byte[] load(String path);
    }

    public static class DiskImageLoader implements ImageLoader {
        private int loadCount = 0;
        @Override public byte[] load(String path) {
            loadCount++;
            return ("image:" + path).getBytes(); // simulate disk load
        }
        public int loadCount() { return loadCount; }
    }

    /** Caching proxy: loads from disk once, returns cached bytes thereafter. */
    public static class CachingImageProxy implements ImageLoader {
        private final DiskImageLoader real   = new DiskImageLoader();
        private final Map<String, byte[]> cache = new HashMap<>();

        @Override public byte[] load(String path) {
            return cache.computeIfAbsent(path, real::load);
        }

        public int cacheSize() { return cache.size(); }
        public int diskLoads() { return real.loadCount(); }
    }

    // ── Bridge ────────────────────────────────────────────────────────────────

    /**
     * Bridge decouples an abstraction from its implementation so both can vary independently.
     * Abstraction: Shape  (what it is)
     * Implementation: Renderer  (how it's drawn)
     * Any Shape works with any Renderer without a combinatorial class explosion.
     */
    public interface Renderer {
        String renderCircle(double radius);
        String renderRect(double w, double h);
    }

    public static class VectorRenderer implements Renderer {
        @Override public String renderCircle(double r)      { return "VectorCircle(r=" + r + ")"; }
        @Override public String renderRect(double w, double h) { return "VectorRect(" + w + "x" + h + ")"; }
    }

    public static class RasterRenderer implements Renderer {
        @Override public String renderCircle(double r)      { return "RasterCircle(r=" + r + ")"; }
        @Override public String renderRect(double w, double h) { return "RasterRect(" + w + "x" + h + ")"; }
    }

    public abstract static class Shape {
        protected final Renderer renderer;
        protected Shape(Renderer renderer) { this.renderer = renderer; }
        public abstract String draw();
    }

    public static class BridgeCircle extends Shape {
        private final double radius;
        public BridgeCircle(Renderer r, double radius) { super(r); this.radius = radius; }
        @Override public String draw() { return renderer.renderCircle(radius); }
    }

    public static class BridgeRect extends Shape {
        private final double w, h;
        public BridgeRect(Renderer r, double w, double h) { super(r); this.w = w; this.h = h; }
        @Override public String draw() { return renderer.renderRect(w, h); }
    }

    // ── Flyweight ─────────────────────────────────────────────────────────────

    /**
     * Flyweight shares common (intrinsic) state across many objects.
     * Extrinsic state (position, colour) is passed in at use time.
     * Classic example: character glyphs in a text editor.
     */
    public record GlyphType(char character, String fontFamily, int fontSize) {
        // Intrinsic (shared) state
    }

    public static class GlyphFactory {
        private static final Map<String, GlyphType> pool = new HashMap<>();

        public static GlyphType get(char ch, String font, int size) {
            String key = ch + ":" + font + ":" + size;
            return pool.computeIfAbsent(key, k -> new GlyphType(ch, font, size));
        }

        public static int poolSize() { return pool.size(); }
    }

    /** Extrinsic state - unique per rendered glyph instance. */
    public record RenderedGlyph(GlyphType type, int x, int y, String colour) {
        public String render() {
            return String.format("%c@(%d,%d) [%s/%d, %s]",
                type.character(), x, y, type.fontFamily(), type.fontSize(), colour);
        }
    }
}
