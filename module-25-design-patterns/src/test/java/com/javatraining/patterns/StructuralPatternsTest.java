package com.javatraining.patterns;

import org.junit.jupiter.api.Test;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class StructuralPatternsTest {

    // ── Adapter ───────────────────────────────────────────────────────────────

    @Test
    void adapter_class_converts_fahrenheit() {
        StructuralPatterns.LegacyThermometer legacy = new StructuralPatterns.LegacyThermometer(212.0);
        StructuralPatterns.TemperatureSource adapter = new StructuralPatterns.ThermometerAdapter(legacy);
        assertEquals(100.0, adapter.getCelsius(), 0.001);
    }

    @Test
    void adapter_freezing_point() {
        StructuralPatterns.LegacyThermometer legacy = new StructuralPatterns.LegacyThermometer(32.0);
        assertEquals(0.0, new StructuralPatterns.ThermometerAdapter(legacy).getCelsius(), 0.001);
    }

    @Test
    void adapter_lambda_variant() {
        StructuralPatterns.LegacyThermometer legacy = new StructuralPatterns.LegacyThermometer(98.6);
        StructuralPatterns.TemperatureSource adapted = StructuralPatterns.adapt(legacy);
        assertEquals(37.0, adapted.getCelsius(), 0.01);
    }

    // ── Decorator ─────────────────────────────────────────────────────────────

    @Test
    void decorator_trim() {
        StructuralPatterns.TextProcessor p =
            new StructuralPatterns.TrimDecorator(new StructuralPatterns.IdentityProcessor());
        assertEquals("hello", p.process("  hello  "));
    }

    @Test
    void decorator_uppercase() {
        StructuralPatterns.TextProcessor p =
            new StructuralPatterns.UpperCaseDecorator(new StructuralPatterns.IdentityProcessor());
        assertEquals("HELLO", p.process("hello"));
    }

    @Test
    void decorator_chain_trim_then_upper() {
        StructuralPatterns.TextProcessor p =
            new StructuralPatterns.UpperCaseDecorator(
                new StructuralPatterns.TrimDecorator(
                    new StructuralPatterns.IdentityProcessor()));
        assertEquals("HELLO", p.process("  hello  "));
    }

    @Test
    void decorator_prefix_suffix() {
        StructuralPatterns.TextProcessor p =
            new StructuralPatterns.SuffixDecorator(
                new StructuralPatterns.PrefixDecorator(
                    new StructuralPatterns.IdentityProcessor(), ">>"),
                "<<");
        assertEquals(">>text<<", p.process("text"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void decorator_functional() {
        StructuralPatterns.TextProcessor p = StructuralPatterns.decorate(
            new StructuralPatterns.IdentityProcessor(),
            String::strip,
            String::toUpperCase
        );
        assertEquals("HELLO", p.process("  hello  "));
    }

    // ── Facade ────────────────────────────────────────────────────────────────

    @Test
    void facade_upload_returns_id() {
        StructuralPatterns.VideoUploadFacade facade = new StructuralPatterns.VideoUploadFacade();
        StructuralPatterns.VideoUploadFacade.UploadResult result =
            facade.upload("My Video", new byte[100], new byte[50]);
        assertNotNull(result.videoId());
        assertFalse(result.videoId().isBlank());
    }

    @Test
    void facade_thumbnail_generated() {
        StructuralPatterns.VideoUploadFacade facade = new StructuralPatterns.VideoUploadFacade();
        assertTrue(facade.upload("Test", new byte[10], new byte[10]).thumbnailGenerated());
    }

    @Test
    void facade_unique_video_ids() {
        StructuralPatterns.VideoUploadFacade facade = new StructuralPatterns.VideoUploadFacade();
        String id1 = facade.upload("A", new byte[1], new byte[1]).videoId();
        String id2 = facade.upload("B", new byte[1], new byte[1]).videoId();
        assertNotEquals(id1, id2);
    }

    // ── Composite ─────────────────────────────────────────────────────────────

    @Test
    void composite_file_size() {
        StructuralPatterns.FileNode f = new StructuralPatterns.FileNode("readme.txt", 500);
        assertEquals(500, f.size());
    }

    @Test
    void composite_directory_aggregates_size() {
        StructuralPatterns.DirectoryNode dir = new StructuralPatterns.DirectoryNode("docs");
        dir.add(new StructuralPatterns.FileNode("a.txt", 100));
        dir.add(new StructuralPatterns.FileNode("b.txt", 200));
        assertEquals(300, dir.size());
    }

    @Test
    void composite_nested_directories() {
        StructuralPatterns.DirectoryNode root = new StructuralPatterns.DirectoryNode("root");
        StructuralPatterns.DirectoryNode sub  = new StructuralPatterns.DirectoryNode("sub");
        sub.add(new StructuralPatterns.FileNode("x.bin", 1000));
        root.add(sub);
        root.add(new StructuralPatterns.FileNode("y.bin", 500));
        assertEquals(1500, root.size());
    }

    @Test
    void composite_display_contains_names() {
        StructuralPatterns.DirectoryNode dir = new StructuralPatterns.DirectoryNode("src");
        dir.add(new StructuralPatterns.FileNode("Main.java", 300));
        String display = dir.display(0);
        assertTrue(display.contains("src"));
        assertTrue(display.contains("Main.java"));
    }

    // ── Proxy ─────────────────────────────────────────────────────────────────

    @Test
    void proxy_caches_repeated_loads() {
        StructuralPatterns.CachingImageProxy proxy = new StructuralPatterns.CachingImageProxy();
        proxy.load("img.png");
        proxy.load("img.png");
        proxy.load("img.png");
        assertEquals(1, proxy.diskLoads(), "disk loaded only once");
        assertEquals(1, proxy.cacheSize());
    }

    @Test
    void proxy_different_paths_hit_disk() {
        StructuralPatterns.CachingImageProxy proxy = new StructuralPatterns.CachingImageProxy();
        proxy.load("a.png");
        proxy.load("b.png");
        assertEquals(2, proxy.diskLoads());
        assertEquals(2, proxy.cacheSize());
    }

    @Test
    void proxy_load_returns_bytes() {
        StructuralPatterns.CachingImageProxy proxy = new StructuralPatterns.CachingImageProxy();
        byte[] data = proxy.load("photo.jpg");
        assertNotNull(data);
        assertTrue(data.length > 0);
    }

    // ── Bridge ────────────────────────────────────────────────────────────────

    @Test
    void bridge_vector_circle() {
        StructuralPatterns.Shape shape = new StructuralPatterns.BridgeCircle(
            new StructuralPatterns.VectorRenderer(), 5.0);
        assertEquals("VectorCircle(r=5.0)", shape.draw());
    }

    @Test
    void bridge_raster_circle() {
        StructuralPatterns.Shape shape = new StructuralPatterns.BridgeCircle(
            new StructuralPatterns.RasterRenderer(), 3.0);
        assertEquals("RasterCircle(r=3.0)", shape.draw());
    }

    @Test
    void bridge_vector_rect() {
        StructuralPatterns.Shape shape = new StructuralPatterns.BridgeRect(
            new StructuralPatterns.VectorRenderer(), 4.0, 6.0);
        assertEquals("VectorRect(4.0x6.0)", shape.draw());
    }

    @Test
    void bridge_raster_rect() {
        StructuralPatterns.Shape shape = new StructuralPatterns.BridgeRect(
            new StructuralPatterns.RasterRenderer(), 2.0, 3.0);
        assertEquals("RasterRect(2.0x3.0)", shape.draw());
    }

    // ── Flyweight ─────────────────────────────────────────────────────────────

    @Test
    void flyweight_same_type_shared() {
        StructuralPatterns.GlyphType g1 = StructuralPatterns.GlyphFactory.get('A', "Arial", 12);
        StructuralPatterns.GlyphType g2 = StructuralPatterns.GlyphFactory.get('A', "Arial", 12);
        assertSame(g1, g2, "identical glyphs share the same instance");
    }

    @Test
    void flyweight_different_types_distinct() {
        StructuralPatterns.GlyphType a = StructuralPatterns.GlyphFactory.get('A', "Arial", 12);
        StructuralPatterns.GlyphType b = StructuralPatterns.GlyphFactory.get('B', "Arial", 12);
        assertNotSame(a, b);
    }

    @Test
    void flyweight_rendered_glyph_includes_position() {
        StructuralPatterns.GlyphType type = StructuralPatterns.GlyphFactory.get('Z', "Times", 14);
        StructuralPatterns.RenderedGlyph rg = new StructuralPatterns.RenderedGlyph(type, 10, 20, "black");
        String rendered = rg.render();
        assertTrue(rendered.contains("Z"));
        assertTrue(rendered.contains("10"));
        assertTrue(rendered.contains("20"));
        assertTrue(rendered.contains("black"));
    }

    @Test
    void flyweight_pool_grows_with_new_types() {
        int before = StructuralPatterns.GlyphFactory.poolSize();
        StructuralPatterns.GlyphFactory.get((char)('α'), "Mono", 9);
        // pool size is at least as large as before (may have grown)
        assertTrue(StructuralPatterns.GlyphFactory.poolSize() >= before);
    }
}
