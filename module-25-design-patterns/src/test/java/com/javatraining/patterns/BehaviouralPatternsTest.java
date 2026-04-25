package com.javatraining.patterns;

import org.junit.jupiter.api.Test;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class BehaviouralPatternsTest {

    // ── Observer ──────────────────────────────────────────────────────────────

    @Test
    void observer_receives_published_events() {
        BehaviouralPatterns.EventBus<BehaviouralPatterns.StockPrice> bus = new BehaviouralPatterns.EventBus<>();
        BehaviouralPatterns.PriceTracker tracker = new BehaviouralPatterns.PriceTracker();
        bus.subscribe(tracker);

        bus.publish(new BehaviouralPatterns.StockPrice("AAPL", 150.0));
        bus.publish(new BehaviouralPatterns.StockPrice("GOOG", 2800.0));

        assertEquals(2, tracker.count());
        assertEquals("AAPL", tracker.history().get(0).symbol());
    }

    @Test
    void observer_unsubscribe_stops_delivery() {
        BehaviouralPatterns.EventBus<String> bus = new BehaviouralPatterns.EventBus<>();
        List<String> received = new ArrayList<>();
        BehaviouralPatterns.EventListener<String> listener = received::add;

        bus.subscribe(listener);
        bus.publish("first");
        bus.unsubscribe(listener);
        bus.publish("second");

        assertEquals(List.of("first"), received);
    }

    @Test
    void observer_multiple_subscribers() {
        BehaviouralPatterns.EventBus<Integer> bus = new BehaviouralPatterns.EventBus<>();
        List<Integer> a = new ArrayList<>(), b = new ArrayList<>();
        bus.subscribe(a::add);
        bus.subscribe(b::add);
        bus.publish(42);
        assertEquals(List.of(42), a);
        assertEquals(List.of(42), b);
    }

    // ── Strategy ──────────────────────────────────────────────────────────────

    @Test
    void strategy_bubble_sort() {
        BehaviouralPatterns.Sorter<Integer> sorter =
            new BehaviouralPatterns.Sorter<>(new BehaviouralPatterns.BubbleSortStrategy<>());
        List<Integer> result = sorter.sort(List.of(3, 1, 4, 1, 5), Comparator.naturalOrder());
        assertEquals(List.of(1, 1, 3, 4, 5), result);
    }

    @Test
    void strategy_java_sort() {
        BehaviouralPatterns.Sorter<String> sorter =
            new BehaviouralPatterns.Sorter<>(new BehaviouralPatterns.JavaSortStrategy<>());
        List<String> result = sorter.sort(List.of("banana", "apple", "cherry"), Comparator.naturalOrder());
        assertEquals(List.of("apple", "banana", "cherry"), result);
    }

    @Test
    void strategy_swap_at_runtime() {
        BehaviouralPatterns.Sorter<Integer> sorter =
            new BehaviouralPatterns.Sorter<>(new BehaviouralPatterns.BubbleSortStrategy<>());
        sorter.setStrategy(new BehaviouralPatterns.JavaSortStrategy<>());
        List<Integer> result = sorter.sort(List.of(5, 2, 8), Comparator.naturalOrder());
        assertEquals(List.of(2, 5, 8), result);
    }

    @Test
    void strategy_does_not_mutate_input() {
        List<Integer> original = new ArrayList<>(List.of(3, 1, 2));
        BehaviouralPatterns.Sorter<Integer> sorter =
            new BehaviouralPatterns.Sorter<>(new BehaviouralPatterns.JavaSortStrategy<>());
        sorter.sort(original, Comparator.naturalOrder());
        assertEquals(List.of(3, 1, 2), original, "input list should not be mutated");
    }

    // ── Command ───────────────────────────────────────────────────────────────

    @Test
    void command_append_and_undo() {
        BehaviouralPatterns.TextDocument doc = new BehaviouralPatterns.TextDocument();
        doc.execute(doc.appendCommand("Hello"));
        assertEquals("Hello", doc.content());
        doc.undo();
        assertEquals("", doc.content());
    }

    @Test
    void command_multiple_appends_undo_one() {
        BehaviouralPatterns.TextDocument doc = new BehaviouralPatterns.TextDocument();
        doc.execute(doc.appendCommand("Hello "));
        doc.execute(doc.appendCommand("World"));
        doc.undo();
        assertEquals("Hello ", doc.content());
    }

    @Test
    void command_redo() {
        BehaviouralPatterns.TextDocument doc = new BehaviouralPatterns.TextDocument();
        doc.execute(doc.appendCommand("Hello"));
        doc.undo();
        doc.redo();
        assertEquals("Hello", doc.content());
    }

    @Test
    void command_replace_and_undo() {
        BehaviouralPatterns.TextDocument doc = new BehaviouralPatterns.TextDocument();
        doc.execute(doc.appendCommand("Hello World"));
        doc.execute(doc.replaceCommand("World", "Java"));
        assertEquals("Hello Java", doc.content());
        doc.undo();
        assertEquals("Hello World", doc.content());
    }

    @Test
    void command_new_action_clears_redo() {
        BehaviouralPatterns.TextDocument doc = new BehaviouralPatterns.TextDocument();
        doc.execute(doc.appendCommand("A"));
        doc.undo();
        doc.execute(doc.appendCommand("B"));
        doc.redo(); // nothing to redo
        assertEquals("B", doc.content());
    }

    // ── Chain of Responsibility ───────────────────────────────────────────────

    @Test
    void chain_authorized_request_passes_through() {
        BehaviouralPatterns.RequestHandler pipeline = BehaviouralPatterns.chain(
            new BehaviouralPatterns.AuthHandler("token123"),
            new BehaviouralPatterns.RateLimitHandler(10),
            new BehaviouralPatterns.EchoHandler()
        );
        BehaviouralPatterns.HttpRequest req = new BehaviouralPatterns.HttpRequest(
            "GET", "/api/data", Map.of("Authorization", "token123"), null);
        BehaviouralPatterns.HttpResponse resp = pipeline.handle(req);
        assertEquals(200, resp.statusCode());
    }

    @Test
    void chain_missing_auth_returns_401() {
        BehaviouralPatterns.RequestHandler pipeline = BehaviouralPatterns.chain(
            new BehaviouralPatterns.AuthHandler("token123"),
            new BehaviouralPatterns.EchoHandler()
        );
        BehaviouralPatterns.HttpRequest req = new BehaviouralPatterns.HttpRequest(
            "GET", "/secret", Map.of(), null);
        assertEquals(401, pipeline.handle(req).statusCode());
    }

    @Test
    void chain_rate_limit_returns_429() {
        BehaviouralPatterns.RateLimitHandler limiter = new BehaviouralPatterns.RateLimitHandler(2);
        BehaviouralPatterns.RequestHandler pipeline = BehaviouralPatterns.chain(
            new BehaviouralPatterns.AuthHandler("tok"),
            limiter,
            new BehaviouralPatterns.EchoHandler()
        );
        BehaviouralPatterns.HttpRequest req = new BehaviouralPatterns.HttpRequest(
            "GET", "/", Map.of("Authorization", "tok"), null);
        pipeline.handle(req);
        pipeline.handle(req);
        BehaviouralPatterns.HttpResponse resp = pipeline.handle(req);
        assertEquals(429, resp.statusCode());
    }

    @Test
    void chain_logging_records_entries() {
        BehaviouralPatterns.LoggingHandler logger = new BehaviouralPatterns.LoggingHandler();
        BehaviouralPatterns.RequestHandler pipeline = BehaviouralPatterns.chain(
            logger,
            new BehaviouralPatterns.EchoHandler()
        );
        BehaviouralPatterns.HttpRequest req = new BehaviouralPatterns.HttpRequest(
            "GET", "/ping", Map.of(), null);
        pipeline.handle(req);
        assertFalse(logger.log().isEmpty());
        assertTrue(logger.log().stream().anyMatch(s -> s.contains("GET")));
    }

    // ── Template Method ───────────────────────────────────────────────────────

    @Test
    void template_plain_text_report() {
        BehaviouralPatterns.ReportGenerator gen = new BehaviouralPatterns.PlainTextReport();
        String report = gen.generate("Sales", List.of("row1", "row2"));
        assertTrue(report.contains("Sales"));
        assertTrue(report.contains("row1"));
        assertTrue(report.contains("row2"));
        assertTrue(report.contains("2 rows"));
    }

    @Test
    void template_html_report() {
        BehaviouralPatterns.ReportGenerator gen = new BehaviouralPatterns.HtmlReport();
        String report = gen.generate("Inventory", List.of("item1", "item2"));
        assertTrue(report.contains("<h1>Inventory</h1>"));
        assertTrue(report.contains("<li>item1</li>"));
        assertTrue(report.contains("</ul>"));
    }

    @Test
    void template_csv_report() {
        BehaviouralPatterns.ReportGenerator gen = new BehaviouralPatterns.CsvReport("name,qty");
        String report = gen.generate("Stock", List.of("apple,5", "pear,3"));
        assertTrue(report.startsWith("name,qty"));
        assertTrue(report.contains("apple,5"));
    }

    // ── Iterator ──────────────────────────────────────────────────────────────

    @Test
    void iterator_range_default_step() {
        BehaviouralPatterns.IntRange range = new BehaviouralPatterns.IntRange(0, 5, 1);
        List<Integer> result = new ArrayList<>();
        for (int i : range) result.add(i);
        assertEquals(List.of(0, 1, 2, 3, 4), result);
    }

    @Test
    void iterator_range_step_2() {
        BehaviouralPatterns.IntRange range = new BehaviouralPatterns.IntRange(1, 10, 2);
        List<Integer> result = new ArrayList<>();
        for (int i : range) result.add(i);
        assertEquals(List.of(1, 3, 5, 7, 9), result);
    }

    @Test
    void iterator_range_invalid_step_throws() {
        assertThrows(IllegalArgumentException.class,
            () -> new BehaviouralPatterns.IntRange(0, 10, 0));
    }

    @Test
    void iterator_binary_tree_inorder() {
        BehaviouralPatterns.BinaryTree<Integer> tree = new BehaviouralPatterns.BinaryTree<>();
        for (int v : new int[]{5, 3, 7, 1, 4}) tree.insert(v);
        List<Integer> result = new ArrayList<>();
        for (int v : tree) result.add(v);
        assertEquals(List.of(1, 3, 4, 5, 7), result);
    }

    @Test
    void iterator_tree_duplicates_ignored() {
        BehaviouralPatterns.BinaryTree<Integer> tree = new BehaviouralPatterns.BinaryTree<>();
        tree.insert(5);
        tree.insert(5);
        tree.insert(5);
        List<Integer> result = new ArrayList<>();
        for (int v : tree) result.add(v);
        assertEquals(List.of(5), result);
    }

    // ── State ─────────────────────────────────────────────────────────────────

    @Test
    void state_happy_path() {
        BehaviouralPatterns.Order order = new BehaviouralPatterns.Order();
        assertEquals(BehaviouralPatterns.OrderStatus.PENDING, order.status());
        order.confirm();
        assertEquals(BehaviouralPatterns.OrderStatus.CONFIRMED, order.status());
        order.ship();
        assertEquals(BehaviouralPatterns.OrderStatus.SHIPPED, order.status());
        order.deliver();
        assertEquals(BehaviouralPatterns.OrderStatus.DELIVERED, order.status());
    }

    @Test
    void state_cancel_from_pending() {
        BehaviouralPatterns.Order order = new BehaviouralPatterns.Order();
        order.cancel();
        assertEquals(BehaviouralPatterns.OrderStatus.CANCELLED, order.status());
    }

    @Test
    void state_cancel_from_confirmed() {
        BehaviouralPatterns.Order order = new BehaviouralPatterns.Order();
        order.confirm();
        order.cancel();
        assertEquals(BehaviouralPatterns.OrderStatus.CANCELLED, order.status());
    }

    @Test
    void state_invalid_transition_throws() {
        BehaviouralPatterns.Order order = new BehaviouralPatterns.Order();
        assertThrows(IllegalStateException.class, order::ship); // can't ship before confirm
    }

    @Test
    void state_history_recorded() {
        BehaviouralPatterns.Order order = new BehaviouralPatterns.Order();
        order.confirm();
        order.ship();
        assertEquals(2, order.history().size());
        assertTrue(order.history().get(0).contains("PENDING"));
    }

    // ── Visitor ───────────────────────────────────────────────────────────────

    @Test
    void visitor_area_circle() {
        BehaviouralPatterns.Shape circle = new BehaviouralPatterns.Circle(5);
        double area = BehaviouralPatterns.visit(circle, new BehaviouralPatterns.AreaVisitor());
        assertEquals(Math.PI * 25, area, 0.001);
    }

    @Test
    void visitor_area_rectangle() {
        BehaviouralPatterns.Shape rect = new BehaviouralPatterns.Rectangle(4, 6);
        double area = BehaviouralPatterns.visit(rect, new BehaviouralPatterns.AreaVisitor());
        assertEquals(24.0, area, 0.001);
    }

    @Test
    void visitor_area_triangle() {
        BehaviouralPatterns.Shape tri = new BehaviouralPatterns.Triangle(6, 4);
        double area = BehaviouralPatterns.visit(tri, new BehaviouralPatterns.AreaVisitor());
        assertEquals(12.0, area, 0.001);
    }

    @Test
    void visitor_describe_all_shapes() {
        BehaviouralPatterns.DescribeVisitor desc = new BehaviouralPatterns.DescribeVisitor();
        assertEquals("Circle r=3.0",   BehaviouralPatterns.visit(new BehaviouralPatterns.Circle(3), desc));
        assertEquals("Rect 4.0x5.0",   BehaviouralPatterns.visit(new BehaviouralPatterns.Rectangle(4, 5), desc));
        assertEquals("Triangle b=6.0 h=8.0", BehaviouralPatterns.visit(new BehaviouralPatterns.Triangle(6, 8), desc));
    }

    @Test
    void visitor_different_visitors_same_structure() {
        List<BehaviouralPatterns.Shape> shapes = List.of(
            new BehaviouralPatterns.Circle(1),
            new BehaviouralPatterns.Rectangle(2, 3)
        );
        BehaviouralPatterns.AreaVisitor area = new BehaviouralPatterns.AreaVisitor();
        BehaviouralPatterns.DescribeVisitor desc = new BehaviouralPatterns.DescribeVisitor();

        // Same shapes, two visitors - no modification to shapes needed
        for (BehaviouralPatterns.Shape s : shapes) {
            assertNotNull(BehaviouralPatterns.visit(s, area));
            assertNotNull(BehaviouralPatterns.visit(s, desc));
        }
    }

    // ── Mediator ──────────────────────────────────────────────────────────────

    @Test
    void mediator_message_delivered_to_others() {
        BehaviouralPatterns.ChatRoom room = new BehaviouralPatterns.ChatRoom();
        BehaviouralPatterns.ChatUser alice = new BehaviouralPatterns.ChatUser("Alice", room);
        BehaviouralPatterns.ChatUser bob   = new BehaviouralPatterns.ChatUser("Bob",   room);

        alice.join();
        bob.join();
        alice.send("Hello Bob!");

        assertEquals(1, bob.receivedCount());
        assertTrue(bob.received().get(0).contains("Hello Bob!"));
    }

    @Test
    void mediator_sender_does_not_receive_own_message() {
        BehaviouralPatterns.ChatRoom room = new BehaviouralPatterns.ChatRoom();
        BehaviouralPatterns.ChatUser alice = new BehaviouralPatterns.ChatUser("Alice", room);
        BehaviouralPatterns.ChatUser bob   = new BehaviouralPatterns.ChatUser("Bob",   room);

        alice.join();
        bob.join();
        alice.send("Hi there");

        assertEquals(0, alice.receivedCount(), "sender should not receive their own message");
    }

    @Test
    void mediator_leave_stops_delivery() {
        BehaviouralPatterns.ChatRoom room = new BehaviouralPatterns.ChatRoom();
        BehaviouralPatterns.ChatUser alice = new BehaviouralPatterns.ChatUser("Alice", room);
        BehaviouralPatterns.ChatUser bob   = new BehaviouralPatterns.ChatUser("Bob",   room);

        alice.join();
        bob.join();
        bob.leave();
        alice.send("Anyone home?");

        assertEquals(0, bob.receivedCount(), "left user should receive nothing");
        assertEquals(1, room.userCount());
    }

    @Test
    void mediator_transcript_records_all_messages() {
        BehaviouralPatterns.ChatRoom room = new BehaviouralPatterns.ChatRoom();
        BehaviouralPatterns.ChatUser alice = new BehaviouralPatterns.ChatUser("Alice", room);
        BehaviouralPatterns.ChatUser bob   = new BehaviouralPatterns.ChatUser("Bob",   room);

        alice.join();
        bob.join();
        alice.send("msg1");
        bob.send("msg2");

        List<String> transcript = room.transcript();
        assertTrue(transcript.stream().anyMatch(s -> s.contains("msg1")));
        assertTrue(transcript.stream().anyMatch(s -> s.contains("msg2")));
    }
}
