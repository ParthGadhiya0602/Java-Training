---
title: "16 — I/O, NIO.2 & Serialization"
parent: "Phase 2 — Core APIs"
nav_order: 16
render_with_liquid: false
---
{% raw %}

[View source on GitHub](https://github.com/ParthGadhiya0602/Java-Training/tree/main/module-16-io-nio-serialization/src){: .btn .btn-outline }

# Module 16 — I/O, NIO.2 & Serialization
{: .no_toc }

**Goal:** Read and write files confidently using both the classic `java.io` and modern `java.nio.file` APIs. Understand Java serialization, its security implications, and NIO channels for high-throughput I/O.

---

## Table of Contents
{: .no_toc .text-delta }
1. TOC
{:toc}

---

## The Two I/O Stacks

Java has two I/O families. Knowing when to use each saves confusion:

| | `java.io` (classic) | `java.nio.file` (NIO.2) |
|---|---|---|
| Core abstraction | Stream (byte/char sequence) | Path + Files (filesystem operations) |
| Introduced | Java 1.0 | Java 7 |
| Error model | Silent failures possible | Checked exceptions always |
| Directory ops | Clunky `File` API | `Files.walk`, `Files.find` |
| **Verdict** | Fine for simple streams | Prefer for all filesystem work |

---

## Classic java.io

### Stream hierarchy

```
InputStream  ←──── FileInputStream, ByteArrayInputStream
OutputStream ←──── FileOutputStream, ByteArrayOutputStream
Reader       ←──── InputStreamReader(InputStream), StringReader
Writer       ←──── OutputStreamWriter(OutputStream), StringWriter
```

Always wrap with `Buffered*` to reduce syscalls:

```java
// BAD: one syscall per character
new FileWriter(file)

// GOOD: buffer accumulates writes, flushes in blocks
new BufferedWriter(new FileWriter(file))
```

### try-with-resources

```java
// Resource auto-closed even if exception thrown
try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
    bw.write("hello");
    bw.newLine();
}   // bw.close() called here automatically
```

**Never** use `finally` to close I/O. `try-with-resources` is always correct.

Multiple resources: closed in reverse declaration order.

```java
try (InputStream in  = new FileInputStream(src);
     OutputStream out = new FileOutputStream(dst)) {
    in.transferTo(out);
}
```

### In-memory streams

`ByteArrayOutputStream` and `ByteArrayInputStream` let you treat a `byte[]` as a stream — invaluable for testing I/O code without touching the filesystem.

```java
ByteArrayOutputStream baos = new ByteArrayOutputStream();
try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(baos))) {
    pw.println("hello");
}
byte[] bytes = baos.toByteArray();
```

---

## NIO.2 — java.nio.file

### Path

`Path` is immutable, unlike `java.io.File`. Construct with `Path.of()`:

```java
Path p = Path.of("/home/user", "docs", "file.txt");
p.getFileName()   // file.txt
p.getParent()     // /home/user/docs
p.normalize()     // resolves ./ and ../
p.toAbsolutePath()
```

### Files — the utility class

```java
// Reading
List<String> lines = Files.readAllLines(path, UTF_8);
String text        = Files.readString(path);          // Java 11+
Stream<String> s   = Files.lines(path);               // lazy; must close

// Writing
Files.write(path, lines, UTF_8);
Files.writeString(path, text);                        // Java 11+
Files.write(path, lines, UTF_8, StandardOpenOption.APPEND);

// Management
Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
Files.move(src, dst, StandardCopyOption.ATOMIC_MOVE); // atomic on same FS
Files.delete(path);           // throws if not found
Files.deleteIfExists(path);   // silent if absent

// Queries
Files.exists(path)
Files.isRegularFile(path)
Files.isDirectory(path)
Files.size(path)
```

### Directory walking

```java
// Files.walk — depth-first Stream<Path>; always close
try (Stream<Path> walk = Files.walk(root)) {
    walk.filter(Files::isRegularFile)
        .filter(p -> p.toString().endsWith(".java"))
        .forEach(System.out::println);
}
```

{: .warning }
> **Always close `Files.walk` and `Files.lines` streams.** They hold OS file handles. Use `try-with-resources` even though the stream appears lazy.

---

## Java Serialization

### Basics

```java
public class Product implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;  // always declare this

    private final String name;
    private final double price;
    private transient double cachedTax;  // excluded from serialization
}
```

`serialVersionUID` guards against accidental incompatibility. Without it, the JVM auto-generates one from class structure — any field rename breaks deserialization of stored data.

`transient` fields are not serialized and reset to their default value (0, null, false) on deserialization.

### Custom hooks

```java
@Serial
private void writeObject(ObjectOutputStream oos) throws IOException {
    oos.defaultWriteObject();  // serialize normal fields first
    oos.writeObject(encrypt(sensitiveField));
}

@Serial
private void readObject(ObjectInputStream ois)
        throws IOException, ClassNotFoundException {
    ois.defaultReadObject();   // restore normal fields first
    this.sensitiveField = decrypt((String) ois.readObject());
}
```

### Externalizable

Full manual control. Requires a public no-arg constructor.

```java
public class Point3D implements Externalizable {
    public Point3D() {}  // required

    @Override public void writeExternal(ObjectOutput out) throws IOException {
        out.writeDouble(x); out.writeDouble(y); out.writeDouble(z);
    }

    @Override public void readExternal(ObjectInput in) throws IOException {
        x = in.readDouble(); y = in.readDouble(); z = in.readDouble();
    }
}
```

Use `Externalizable` when: schema must be stable across class changes, or you need maximum performance.

### Security — ObjectInputFilter

{: .warning }
> **Never deserialize untrusted bytes without a filter.** Gadget-chain attacks can achieve RCE via deserialization.

```java
try (ObjectInputStream ois = new ObjectInputStream(stream)) {
    ois.setObjectInputFilter(info -> {
        Class<?> c = info.serialClass();
        if (c == null) return ObjectInputFilter.Status.UNDECIDED;
        if (c.getName().startsWith("com.myapp.")) return ALLOWED;
        return REJECTED;
    });
    return (MyClass) ois.readObject();
}
```

---

## NIO Channels and Buffers

### Buffer state machine

```
[write into buffer]  position=N, limit=capacity
      ↓ flip()
[read from buffer]   position=0, limit=N
      ↓ clear() or compact()
[ready for writes]   position=0, limit=capacity
```

### FileChannel

```java
try (FileChannel fc = FileChannel.open(path, READ)) {
    ByteBuffer buf = ByteBuffer.allocate(4096);
    while (fc.read(buf) != -1) {
        buf.flip();
        // process buf.remaining() bytes
        buf.clear();
    }
}
```

### Zero-copy: transferTo

On Linux this maps to `sendfile(2)` — data never enters JVM heap:

```java
src.transferTo(0, src.size(), dst);  // OS copies directly
```

### Memory-mapped files

```java
MappedByteBuffer mbb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
// mbb behaves like a ByteBuffer backed by the file's OS page cache
byte b = mbb.get(offset);  // random access without read() call
```

---

## Source Files

| File | What it covers |
|---|---|
| `ClassicIODemo.java` | BufferedReader/Writer, byte streams, in-memory streams, StreamTokenizer, PrintWriter |
| `NioFilesDemo.java` | Path ops, Files read/write, directory walking, file attributes, temp files |
| `SerializationDemo.java` | Serializable, transient, serialVersionUID, custom hooks, Externalizable, ObjectInputFilter |
| `NioChannelsDemo.java` | ByteBuffer lifecycle, FileChannel, zero-copy transferTo, memory-mapped files, Pipe |

---

## Common Mistakes

{: .warning }
> **`java.io.File.delete()` returns false silently.** Use `Files.delete(path)` which throws `IOException` on failure so you never miss a deletion error.

{: .warning }
> **Forgetting to flush a BufferedWriter.** `try-with-resources` calls `close()` which flushes, but if you hold the writer open and don't call `flush()`, data stays in the buffer.

{: .tip }
> **Prefer `Files.readAllLines` for small files, `Files.lines` for large ones.** `readAllLines` loads everything into memory; `Files.lines` is lazy but must be closed.
{% endraw %}
