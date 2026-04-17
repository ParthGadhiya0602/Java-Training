package com.javatraining.patterns;

import java.util.*;
import java.util.function.*;

/**
 * Module 25 — Behavioural Patterns
 *
 * Behavioural patterns deal with algorithms and the assignment of
 * responsibilities between objects.
 *
 * Patterns covered:
 *   Observer          — event pub/sub without tight coupling
 *   Strategy          — swap algorithms at runtime
 *   Command           — encapsulate a request as an object (undo/redo)
 *   Chain of Responsibility — pass request along a handler chain
 *   Template Method   — skeleton algorithm; subclasses fill in steps
 *   Iterator          — sequential access without exposing internals
 *   State             — behaviour changes with internal state
 *   Visitor           — add operations to a hierarchy without modifying it
 *   Mediator          — centralise complex communications
 */
public class BehaviouralPatterns {

    // ── Observer ──────────────────────────────────────────────────────────────

    /**
     * Observer (publish-subscribe): subjects notify registered listeners
     * whenever their state changes.  Listeners are decoupled from the subject.
     * Java equivalents: PropertyChangeListener, Flow.Publisher/Subscriber (reactive streams).
     */
    public interface EventListener<T> {
        void onEvent(T event);
    }

    public static class EventBus<T> {
        private final List<EventListener<T>> listeners = new ArrayList<>();

        public void subscribe(EventListener<T> listener) { listeners.add(listener); }
        public void unsubscribe(EventListener<T> listener) { listeners.remove(listener); }

        public void publish(T event) {
            for (EventListener<T> l : listeners) l.onEvent(event);
        }

        public int listenerCount() { return listeners.size(); }
    }

    public record StockPrice(String symbol, double price) {}

    /** Concrete observer that records the prices it receives. */
    public static class PriceTracker implements EventListener<StockPrice> {
        private final List<StockPrice> history = new ArrayList<>();

        @Override public void onEvent(StockPrice e) { history.add(e); }

        public List<StockPrice> history() { return Collections.unmodifiableList(history); }
        public int count() { return history.size(); }
    }

    // ── Strategy ──────────────────────────────────────────────────────────────

    /**
     * Strategy: define a family of algorithms, encapsulate each one, and make
     * them interchangeable.  The client selects the algorithm at runtime.
     * In modern Java, strategies are often plain functional interfaces (lambdas).
     */
    public interface SortStrategy<T> {
        void sort(List<T> list, Comparator<T> comparator);
    }

    public static class BubbleSortStrategy<T> implements SortStrategy<T> {
        @Override
        public void sort(List<T> list, Comparator<T> cmp) {
            int n = list.size();
            for (int i = 0; i < n - 1; i++) {
                for (int j = 0; j < n - i - 1; j++) {
                    if (cmp.compare(list.get(j), list.get(j + 1)) > 0) {
                        T tmp = list.get(j);
                        list.set(j, list.get(j + 1));
                        list.set(j + 1, tmp);
                    }
                }
            }
        }
    }

    public static class JavaSortStrategy<T> implements SortStrategy<T> {
        @Override
        public void sort(List<T> list, Comparator<T> cmp) {
            list.sort(cmp);
        }
    }

    /** Context: owns the strategy and delegates sorting to it. */
    public static class Sorter<T> {
        private SortStrategy<T> strategy;

        public Sorter(SortStrategy<T> strategy) { this.strategy = strategy; }

        public void setStrategy(SortStrategy<T> s) { this.strategy = s; }

        public List<T> sort(List<T> items, Comparator<T> cmp) {
            List<T> copy = new ArrayList<>(items);
            strategy.sort(copy, cmp);
            return copy;
        }
    }

    // ── Command ───────────────────────────────────────────────────────────────

    /**
     * Command: encapsulate a request as an object, supporting undo/redo,
     * queuing, logging, and transaction-style rollback.
     */
    public interface Command {
        void execute();
        void undo();
    }

    /** A simple text document with undo/redo support. */
    public static class TextDocument {
        private final StringBuilder content = new StringBuilder();
        private final Deque<Command> history = new ArrayDeque<>();
        private final Deque<Command> redoStack = new ArrayDeque<>();

        public void execute(Command cmd) {
            cmd.execute();
            history.push(cmd);
            redoStack.clear();          // new action clears redo history
        }

        public void undo() {
            if (!history.isEmpty()) {
                Command cmd = history.pop();
                cmd.undo();
                redoStack.push(cmd);
            }
        }

        public void redo() {
            if (!redoStack.isEmpty()) {
                Command cmd = redoStack.pop();
                cmd.execute();
                history.push(cmd);
            }
        }

        public String content() { return content.toString(); }
        public int historySize() { return history.size(); }

        // ── Concrete commands ──────────────────────────────────────────────

        public Command appendCommand(String text) {
            return new Command() {
                @Override public void execute() { content.append(text); }
                @Override public void undo()    { content.delete(content.length() - text.length(), content.length()); }
            };
        }

        public Command replaceCommand(String oldText, String newText) {
            return new Command() {
                private int pos = -1;
                @Override public void execute() {
                    pos = content.indexOf(oldText);
                    if (pos >= 0) content.replace(pos, pos + oldText.length(), newText);
                }
                @Override public void undo() {
                    if (pos >= 0) content.replace(pos, pos + newText.length(), oldText);
                }
            };
        }
    }

    // ── Chain of Responsibility ───────────────────────────────────────────────

    /**
     * Chain of Responsibility: pass a request along a chain of handlers.
     * Each handler decides to process it or pass it to the next handler.
     * Used in: servlet filter chains, logging frameworks, middleware pipelines.
     */
    public record HttpRequest(String method, String path, Map<String, String> headers, String body) {}
    public record HttpResponse(int statusCode, String body) {}

    public interface RequestHandler {
        HttpResponse handle(HttpRequest request);
        void setNext(RequestHandler next);
    }

    public abstract static class BaseHandler implements RequestHandler {
        private RequestHandler next;

        @Override public void setNext(RequestHandler next) { this.next = next; }

        protected HttpResponse passToNext(HttpRequest request) {
            return next != null ? next.handle(request) : new HttpResponse(501, "Not implemented");
        }
    }

    public static class AuthHandler extends BaseHandler {
        private final Set<String> validTokens;
        public AuthHandler(String... tokens) { this.validTokens = new HashSet<>(Arrays.asList(tokens)); }

        @Override public HttpResponse handle(HttpRequest req) {
            String token = req.headers().get("Authorization");
            if (token == null || !validTokens.contains(token))
                return new HttpResponse(401, "Unauthorized");
            return passToNext(req);
        }
    }

    public static class RateLimitHandler extends BaseHandler {
        private final int maxRequests;
        private int count = 0;

        public RateLimitHandler(int maxRequests) { this.maxRequests = maxRequests; }

        @Override public HttpResponse handle(HttpRequest req) {
            if (++count > maxRequests)
                return new HttpResponse(429, "Too many requests");
            return passToNext(req);
        }

        public void reset() { count = 0; }
    }

    public static class LoggingHandler extends BaseHandler {
        private final List<String> log = new ArrayList<>();

        @Override public HttpResponse handle(HttpRequest req) {
            log.add(req.method() + " " + req.path());
            HttpResponse resp = passToNext(req);
            log.add("-> " + resp.statusCode());
            return resp;
        }

        public List<String> log() { return Collections.unmodifiableList(log); }
    }

    public static class EchoHandler extends BaseHandler {
        @Override public HttpResponse handle(HttpRequest req) {
            return new HttpResponse(200, "OK: " + req.path());
        }
    }

    /** Utility: wire handlers left-to-right and return the head. */
    public static RequestHandler chain(RequestHandler... handlers) {
        for (int i = 0; i < handlers.length - 1; i++)
            handlers[i].setNext(handlers[i + 1]);
        return handlers[0];
    }

    // ── Template Method ───────────────────────────────────────────────────────

    /**
     * Template Method: define the skeleton of an algorithm in the base class;
     * subclasses override specific steps without changing the overall structure.
     * The classic GoF pattern expressed through abstract methods.
     */
    public abstract static class ReportGenerator {
        /** Template method — defines the invariant sequence. */
        public final String generate(String title, List<String> rows) {
            StringBuilder sb = new StringBuilder();
            sb.append(formatHeader(title));
            sb.append(formatDivider());
            for (String row : rows) sb.append(formatRow(row));
            sb.append(formatFooter(rows.size()));
            return sb.toString();
        }

        protected abstract String formatHeader(String title);
        protected abstract String formatDivider();
        protected abstract String formatRow(String row);
        protected abstract String formatFooter(int rowCount);
    }

    public static class PlainTextReport extends ReportGenerator {
        @Override protected String formatHeader(String title)  { return "=== " + title + " ===\n"; }
        @Override protected String formatDivider()             { return "---\n"; }
        @Override protected String formatRow(String row)       { return "  " + row + "\n"; }
        @Override protected String formatFooter(int n)        { return "Total: " + n + " rows\n"; }
    }

    public static class CsvReport extends ReportGenerator {
        private final String header;
        public CsvReport(String header) { this.header = header; }
        @Override protected String formatHeader(String title)  { return header + "\n"; }
        @Override protected String formatDivider()             { return ""; }
        @Override protected String formatRow(String row)       { return row + "\n"; }
        @Override protected String formatFooter(int n)        { return ""; }
    }

    public static class HtmlReport extends ReportGenerator {
        @Override protected String formatHeader(String title)  { return "<h1>" + title + "</h1>\n<ul>\n"; }
        @Override protected String formatDivider()             { return ""; }
        @Override protected String formatRow(String row)       { return "<li>" + row + "</li>\n"; }
        @Override protected String formatFooter(int n)        { return "</ul>\n<p>Total: " + n + "</p>\n"; }
    }

    // ── Iterator ──────────────────────────────────────────────────────────────

    /**
     * Iterator: provide a way to sequentially access the elements of an
     * aggregate object without exposing its underlying representation.
     * Java's Iterable/Iterator is the canonical example.
     * Here: a range iterator and a tree in-order iterator.
     */
    public static class IntRange implements Iterable<Integer> {
        private final int start, end, step;

        public IntRange(int start, int end, int step) {
            if (step <= 0) throw new IllegalArgumentException("step must be > 0");
            this.start = start;
            this.end   = end;
            this.step  = step;
        }

        @Override
        public Iterator<Integer> iterator() {
            return new Iterator<>() {
                private int current = start;
                @Override public boolean hasNext() { return current < end; }
                @Override public Integer  next()   {
                    if (!hasNext()) throw new NoSuchElementException();
                    int val = current;
                    current += step;
                    return val;
                }
            };
        }
    }

    /** A minimal binary tree with an in-order iterator. */
    public static class BinaryTree<T extends Comparable<T>> implements Iterable<T> {
        private Node<T> root;

        private record Node<T>(T value, Node<T> left, Node<T> right) {}

        public void insert(T value) { root = insert(root, value); }

        private Node<T> insert(Node<T> node, T value) {
            if (node == null) return new Node<>(value, null, null);
            int cmp = value.compareTo(node.value());
            if (cmp < 0) return new Node<>(node.value(), insert(node.left(), value), node.right());
            if (cmp > 0) return new Node<>(node.value(), node.left(), insert(node.right(), value));
            return node; // duplicate — ignored
        }

        @Override
        public Iterator<T> iterator() {
            // Iterative in-order using explicit stack
            Deque<Node<T>> stack = new ArrayDeque<>();
            pushLeft(stack, root);
            return new Iterator<>() {
                @Override public boolean hasNext() { return !stack.isEmpty(); }
                @Override public T next() {
                    if (!hasNext()) throw new NoSuchElementException();
                    Node<T> node = stack.pop();
                    pushLeft(stack, node.right());
                    return node.value();
                }
            };
        }

        private void pushLeft(Deque<Node<T>> stack, Node<T> node) {
            while (node != null) { stack.push(node); node = node.left(); }
        }
    }

    // ── State ─────────────────────────────────────────────────────────────────

    /**
     * State: allow an object to alter its behaviour when its internal state
     * changes.  The object will appear to change its class.
     * Replaces large if/else or switch chains with state objects.
     */
    public enum OrderStatus { PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED }

    public static class Order {
        private OrderStatus status = OrderStatus.PENDING;
        private final List<String> history = new ArrayList<>();

        public void confirm() {
            if (status == OrderStatus.PENDING) transition(OrderStatus.CONFIRMED, "Order confirmed");
            else throw new IllegalStateException("Cannot confirm from " + status);
        }

        public void ship() {
            if (status == OrderStatus.CONFIRMED) transition(OrderStatus.SHIPPED, "Order shipped");
            else throw new IllegalStateException("Cannot ship from " + status);
        }

        public void deliver() {
            if (status == OrderStatus.SHIPPED) transition(OrderStatus.DELIVERED, "Order delivered");
            else throw new IllegalStateException("Cannot deliver from " + status);
        }

        public void cancel() {
            if (status == OrderStatus.PENDING || status == OrderStatus.CONFIRMED)
                transition(OrderStatus.CANCELLED, "Order cancelled");
            else throw new IllegalStateException("Cannot cancel from " + status);
        }

        private void transition(OrderStatus next, String message) {
            history.add(status + " → " + next + ": " + message);
            status = next;
        }

        public OrderStatus status()       { return status; }
        public List<String> history()     { return Collections.unmodifiableList(history); }
    }

    // ── Visitor ───────────────────────────────────────────────────────────────

    /**
     * Visitor: represent an operation to be performed on elements of an object
     * structure.  Visitor lets you define a new operation without changing the
     * classes of the elements it operates on.
     * Modern Java: sealed classes + switch expressions can replace traditional Visitor.
     */
    public sealed interface Shape permits BehaviouralPatterns.Circle, BehaviouralPatterns.Rectangle, BehaviouralPatterns.Triangle {}

    public record Circle(double radius)                      implements Shape {}
    public record Rectangle(double width, double height)     implements Shape {}
    public record Triangle(double base, double height)       implements Shape {}

    public interface ShapeVisitor<R> {
        R visitCircle(Circle c);
        R visitRectangle(Rectangle r);
        R visitTriangle(Triangle t);
    }

    /** Dispatch: route the sealed hierarchy to the visitor. */
    public static <R> R visit(Shape shape, ShapeVisitor<R> visitor) {
        return switch (shape) {
            case Circle    c -> visitor.visitCircle(c);
            case Rectangle r -> visitor.visitRectangle(r);
            case Triangle  t -> visitor.visitTriangle(t);
        };
    }

    public static class AreaVisitor implements ShapeVisitor<Double> {
        @Override public Double visitCircle(Circle c)        { return Math.PI * c.radius() * c.radius(); }
        @Override public Double visitRectangle(Rectangle r)  { return r.width() * r.height(); }
        @Override public Double visitTriangle(Triangle t)    { return 0.5 * t.base() * t.height(); }
    }

    public static class PerimeterVisitor implements ShapeVisitor<Double> {
        @Override public Double visitCircle(Circle c)        { return 2 * Math.PI * c.radius(); }
        @Override public Double visitRectangle(Rectangle r)  { return 2 * (r.width() + r.height()); }
        @Override public Double visitTriangle(Triangle t)    {
            // isoceles assumption for simplicity
            double side = Math.sqrt((t.base() / 2) * (t.base() / 2) + t.height() * t.height());
            return t.base() + 2 * side;
        }
    }

    public static class DescribeVisitor implements ShapeVisitor<String> {
        @Override public String visitCircle(Circle c)        { return "Circle r=" + c.radius(); }
        @Override public String visitRectangle(Rectangle r)  { return "Rect " + r.width() + "x" + r.height(); }
        @Override public String visitTriangle(Triangle t)    { return "Triangle b=" + t.base() + " h=" + t.height(); }
    }

    // ── Mediator ──────────────────────────────────────────────────────────────

    /**
     * Mediator: define an object that encapsulates how a set of objects interact.
     * Reduces the number of direct object-to-object connections (N² → N).
     * Classic example: chat room where users send messages through the room.
     */
    public interface ChatMediator {
        void send(String from, String message);
        void join(ChatUser user);
        void leave(String username);
    }

    public static class ChatRoom implements ChatMediator {
        private final Map<String, ChatUser> users = new LinkedHashMap<>();
        private final List<String> transcript = new ArrayList<>();

        @Override
        public void join(ChatUser user) {
            users.put(user.username(), user);
            transcript.add("[SYSTEM] " + user.username() + " joined");
        }

        @Override
        public void leave(String username) {
            users.remove(username);
            transcript.add("[SYSTEM] " + username + " left");
        }

        @Override
        public void send(String from, String message) {
            broadcast(from, message);
        }

        private void broadcast(String from, String text) {
            String line = "[" + from + "] " + text;
            transcript.add(line);
            for (ChatUser u : users.values()) {
                if (!u.username().equals(from)) u.receive(line);
            }
        }

        public List<String> transcript() { return Collections.unmodifiableList(transcript); }
        public int userCount() { return users.size(); }
    }

    public static class ChatUser {
        private final String username;
        private final ChatMediator room;
        private final List<String> received = new ArrayList<>();

        public ChatUser(String username, ChatMediator room) {
            this.username = username;
            this.room     = room;
        }

        public void join()                    { room.join(this); }
        public void send(String message)      { room.send(username, message); }
        public void leave()                   { room.leave(username); }
        public void receive(String message)   { received.add(message); }

        public String       username()         { return username; }
        public List<String> received()         { return Collections.unmodifiableList(received); }
        public int          receivedCount()    { return received.size(); }
    }
}
