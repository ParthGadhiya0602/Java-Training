---
title: "Module 25 — Design Patterns"
nav_order: 25
render_with_liquid: false
---
{% raw %}

[View source on GitHub](https://github.com/ParthGadhiya0602/Java-Training/tree/main/module-25-design-patterns/src){: .btn .btn-outline }

# Module 25 — Design Patterns

Design patterns are reusable solutions to recurring software design problems.
This module covers all three GoF categories — creational, structural, and
behavioural — with idiomatic Java 21 implementations and practical commentary.

---

## Creational Patterns

Creational patterns abstract the object-creation process, decoupling callers
from the concrete classes they need.

### Singleton — init-on-demand holder

```java
public static class AppConfig {
    private AppConfig() { /* load defaults */ }

    private static class Holder {
        static final AppConfig INSTANCE = new AppConfig();
    }

    public static AppConfig getInstance() { return Holder.INSTANCE; }
}
```

Thread-safe lazy initialisation without `synchronized`.
The inner `Holder` class is not loaded until `getInstance()` is first called;
the JVM class-loading guarantee provides atomicity for free.

### Factory Method

```java
public abstract static class NotificationService {
    protected abstract Notification createNotification();  // factory method

    public void notify(String recipient, String message) {
        createNotification().send(recipient, message);     // uses the product
    }
}
```

The creator declares the factory method; subclasses (`EmailService`,
`SmsService`) decide which concrete product to instantiate.
A static factory variant (`notificationFor("EMAIL")`) achieves the same
result without subclassing.

### Abstract Factory

```java
public interface UIFactory {
    Button   createButton();
    Checkbox createCheckbox();
}
// LightThemeFactory / DarkThemeFactory implement UIFactory
List<String> rendered = renderUI(new DarkThemeFactory());
```

Creates entire *families* of related objects without specifying concrete
classes.  Swap one factory to swap the entire look & feel.

### Builder

```java
HttpRequest req = HttpRequest.newBuilder("POST", "http://api.example.com")
    .header("Content-Type", "application/json")
    .body("{\"key\":\"value\"}")
    .timeoutMs(10_000)
    .followRedirects(false)
    .build();
```

Solves the "telescoping constructor" anti-pattern.
Required fields are constructor parameters of `Builder`; everything else is
optional with sensible defaults.  The result (`HttpRequest`) is immutable.

### Prototype

```java
DocumentTemplate copy = original.copy();       // deep copy
DocumentTemplate variant = original.withTitle("New Title");
```

Clones an existing object instead of re-creating it from scratch.
`copy()` performs a deep copy of mutable collections so mutations in one
instance do not affect the other.

### Object Pool

```java
ObjectPool<StringBuilder> pool = new ObjectPool<>(StringBuilder::new, 10);
StringBuilder sb = pool.acquire();   // borrow
// … use sb …
pool.release(sb);                    // return
```

Pre-creates expensive objects and reuses them.
`acquire()` pulls from the available queue or creates up to `maxSize`;
throws `IllegalStateException` if the pool is exhausted.
Used in practice for DB connections, ByteBuffers, and thread pools.

---

## Structural Patterns

Structural patterns compose objects and classes into larger structures while
keeping those structures flexible and efficient.

### Adapter

```java
// Legacy library returns Fahrenheit; our interface expects Celsius
TemperatureSource adapted = new ThermometerAdapter(legacyThermometer);
double celsius = adapted.getCelsius();

// Lambda variant
TemperatureSource adapted = StructuralPatterns.adapt(legacyThermometer);
```

Wraps an incompatible interface without modifying the legacy class.
The lambda variant works naturally when `TemperatureSource` is a
functional interface.

### Decorator

```java
TextProcessor p = new UpperCaseDecorator(
    new TrimDecorator(
        new IdentityProcessor()));
p.process("  hello  ");  // → "HELLO"
```

Adds behaviour by wrapping the original object — each decorator adds one
concern.  Decorators compose freely in any order.
Java I/O streams (`FileInputStream → BufferedInputStream → GZIPInputStream`)
are the canonical real-world example.

### Facade

```java
VideoUploadFacade facade = new VideoUploadFacade();
UploadResult result = facade.upload("My Video", rawVideo, rawAudio);
```

Hides four subsystems (`VideoEncoder`, `AudioNormaliser`,
`ThumbnailGenerator`, `MetadataWriter`) behind a single method call.
Clients never need to know the subsystems exist.

### Composite

```java
DirectoryNode root = new DirectoryNode("project");
root.add(new FileNode("README.md", 1024));
root.add(new DirectoryNode("src").add(new FileNode("Main.java", 3000)));
long total = root.size();   // recurses through the whole tree
```

Lets clients treat leaf nodes (`FileNode`) and composite nodes
(`DirectoryNode`) identically through the `FileSystemNode` interface.

### Proxy

```java
CachingImageProxy proxy = new CachingImageProxy();
proxy.load("hero.png");  // disk I/O
proxy.load("hero.png");  // served from cache
proxy.diskLoads();       // 1
```

Controls access to the real subject.  The caching proxy (`computeIfAbsent`)
ensures disk I/O happens at most once per path.

### Bridge

```java
Shape s1 = new BridgeCircle(new VectorRenderer(), 5.0);
Shape s2 = new BridgeCircle(new RasterRenderer(), 5.0);
```

Decouples the *abstraction* (`Shape`) from the *implementation*
(`Renderer`) so both can vary independently.
Avoids the combinatorial class explosion that inheritance would cause
(2 shapes × 2 renderers = 4 subclasses; Bridge needs 2 + 2 = 4 classes total
regardless of how many shapes or renderers are added later).

### Flyweight

```java
GlyphType type = GlyphFactory.get('A', "Arial", 12);  // shared
RenderedGlyph rg = new RenderedGlyph(type, x, y, "black");  // unique per position
```

Shares *intrinsic* state (character, font, size) across thousands of
rendered glyphs.  *Extrinsic* state (position, colour) is passed at use time.
Classic example: character glyphs in a text editor.

---

## Behavioural Patterns

Behavioural patterns deal with algorithms and the assignment of
responsibilities between objects.

### Observer

```java
EventBus<StockPrice> bus = new EventBus<>();
PriceTracker tracker = new PriceTracker();
bus.subscribe(tracker);
bus.publish(new StockPrice("AAPL", 150.0));  // tracker notified
bus.unsubscribe(tracker);
```

Subjects notify registered listeners without knowing who they are.
Java equivalents: `PropertyChangeListener`, reactive `Flow.Publisher`.

### Strategy

```java
Sorter<Integer> sorter = new Sorter<>(new BubbleSortStrategy<>());
sorter.setStrategy(new JavaSortStrategy<>());   // swap at runtime
List<Integer> sorted = sorter.sort(items, Comparator.naturalOrder());
```

Encapsulates a family of algorithms and makes them interchangeable.
The `Sorter` context delegates to whichever `SortStrategy` is currently
configured.  In modern Java, strategies are often plain lambdas.

### Command

```java
TextDocument doc = new TextDocument();
doc.execute(doc.appendCommand("Hello "));
doc.execute(doc.appendCommand("World"));
doc.undo();   // → "Hello "
doc.redo();   // → "Hello World"
```

Encapsulates a request as an object.
Each `Command` pairs `execute()` with `undo()`.
The `TextDocument` maintains a history stack and a redo stack.

### Chain of Responsibility

```java
RequestHandler pipeline = chain(
    new AuthHandler("token123"),
    new RateLimitHandler(100),
    new LoggingHandler(),
    new EchoHandler()
);
HttpResponse resp = pipeline.handle(request);
```

Passes a request along a chain until a handler processes it.
Each handler either handles the request or calls `passToNext()`.
Servlet filter chains and middleware pipelines use this pattern.

### Template Method

```java
ReportGenerator gen = new HtmlReport();
String report = gen.generate("Sales Q1", List.of("row1", "row2"));
// <h1>Sales Q1</h1> <ul> <li>row1</li> ... </ul>
```

Defines a skeleton algorithm (`generate`) in the base class; subclasses
(`PlainTextReport`, `CsvReport`, `HtmlReport`) override the hook methods
without changing the overall sequence.

### Iterator

```java
// Range iterator
for (int i : new IntRange(1, 10, 2)) { /* 1, 3, 5, 7, 9 */ }

// In-order BST iterator
BinaryTree<Integer> tree = new BinaryTree<>();
for (int v : List.of(5, 3, 7)) tree.insert(v);
for (int v : tree) { /* 3, 5, 7 */ }
```

Provides sequential access without exposing internal structure.
The iterative BST iterator uses an explicit stack to avoid recursion.

### State

```java
Order order = new Order();
order.confirm();   // PENDING → CONFIRMED
order.ship();      // CONFIRMED → SHIPPED
order.deliver();   // SHIPPED → DELIVERED
order.cancel();    // throws — cannot cancel a delivered order
```

Object behaviour changes with its internal state.
Illegal transitions throw `IllegalStateException` rather than silently
succeeding.  The history list records every transition.

### Visitor

```java
// Same shapes, multiple operations — no modification to Shape classes needed
double area  = visit(new Circle(5), new AreaVisitor());
double perim = visit(new Circle(5), new PerimeterVisitor());
String desc  = visit(new Circle(5), new DescribeVisitor());
```

Adds new operations to an object hierarchy without modifying it.
Modern Java: sealed classes + `switch` expressions replace the traditional
`accept(visitor)` double-dispatch, as shown here.

### Mediator

```java
ChatRoom room = new ChatRoom();
ChatUser alice = new ChatUser("Alice", room);
ChatUser bob   = new ChatUser("Bob",   room);
alice.join();
bob.join();
alice.send("Hi!");   // Bob receives; Alice does not
bob.leave();
```

Centralises communication between objects through a single mediator.
Reduces N² direct connections to N connections (each user ↔ room only).

---

## Key Takeaways

| Category    | Pattern                 | One-line summary                                       |
|-------------|-------------------------|--------------------------------------------------------|
| Creational  | Singleton               | One instance, thread-safe via init-on-demand holder    |
| Creational  | Factory Method          | Subclass decides which object to create                |
| Creational  | Abstract Factory        | Create families of related objects                     |
| Creational  | Builder                 | Fluent construction of complex immutable objects       |
| Creational  | Prototype               | Clone an existing object as a starting point           |
| Creational  | Object Pool             | Reuse expensive-to-create objects                      |
| Structural  | Adapter                 | Convert incompatible interfaces                        |
| Structural  | Decorator               | Add behaviour by wrapping, not subclassing             |
| Structural  | Facade                  | Simple interface over a complex subsystem              |
| Structural  | Composite               | Uniform treatment of leaf and branch nodes             |
| Structural  | Proxy                   | Controlled access, caching, or lazy loading            |
| Structural  | Bridge                  | Decouple abstraction from implementation               |
| Structural  | Flyweight               | Share intrinsic state to reduce memory footprint       |
| Behavioural | Observer                | Event pub/sub without tight coupling                   |
| Behavioural | Strategy                | Swap algorithms at runtime                             |
| Behavioural | Command                 | Encapsulate requests; enable undo/redo                 |
| Behavioural | Chain of Responsibility | Pass request along a handler chain                     |
| Behavioural | Template Method         | Skeleton algorithm; subclasses fill in steps           |
| Behavioural | Iterator                | Sequential access without exposing internals           |
| Behavioural | State                   | Behaviour changes with internal state                  |
| Behavioural | Visitor                 | Add operations to a hierarchy without modifying it     |
| Behavioural | Mediator                | Centralise complex communications                      |
{% endraw %}
