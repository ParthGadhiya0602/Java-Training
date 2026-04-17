package com.javatraining.io;

import java.io.*;
import java.util.List;

/**
 * Module 16 — Java Serialization
 *
 * Java serialization converts an object graph to bytes and back.
 * It is the foundation of RMI, JMX, and some caching systems —
 * but is largely replaced by JSON/Protobuf in modern code.
 *
 * Key concepts:
 *   Serializable         — marker interface; JVM handles field serialization
 *   serialVersionUID     — version guard; prevent accidental incompatibility
 *   transient            — exclude a field from serialization
 *   Externalizable       — full manual control over read/writeExternal()
 *   readObject/writeObject — custom hook inside Serializable classes
 *   ObjectInputFilter    — defend against deserialization attacks (Java 9+)
 *
 * WARNING: Never deserialize data from untrusted sources without a filter.
 */
public class SerializationDemo {

    // ── Basic Serializable ────────────────────────────────────────────────────

    /**
     * Any Serializable class should declare serialVersionUID explicitly.
     * Without it the JVM generates one from class structure — a field rename
     * silently breaks deserialization of stored data.
     */
    public static class Product implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private final String name;
        private final double price;
        private final String category;
        private transient double cachedTax;   // transient — not serialized

        public Product(String name, double price, String category) {
            this.name     = name;
            this.price    = price;
            this.category = category;
        }

        public String getName()     { return name; }
        public double getPrice()    { return price; }
        public String getCategory() { return category; }
        public double getCachedTax(){ return cachedTax; }
        public void   setCachedTax(double t) { cachedTax = t; }

        @Override public String toString() {
            return "Product{name='%s', price=%.2f, category='%s'}".formatted(name, price, category);
        }
    }

    // ── Custom readObject/writeObject ─────────────────────────────────────────

    /**
     * Custom serialization hooks let you validate, encrypt, or transform
     * data during serialization without switching to Externalizable.
     *
     * readObject must call defaultReadObject() first, then do custom work.
     */
    public static class SecureConfig implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private String host;
        private int    port;
        private String encryptedPassword;   // stored encrypted
        private transient String plainPassword; // live field, never serialized raw

        public SecureConfig(String host, int port, String plainPassword) {
            this.host = host;
            this.port = port;
            this.plainPassword   = plainPassword;
            this.encryptedPassword = rot13(plainPassword); // trivial "encryption" for demo
        }

        @Serial
        private void writeObject(ObjectOutputStream oos) throws IOException {
            oos.defaultWriteObject();  // writes host, port, encryptedPassword
        }

        @Serial
        private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
            ois.defaultReadObject();   // restores host, port, encryptedPassword
            this.plainPassword = rot13(encryptedPassword);  // decrypt on load
        }

        public String getHost()          { return host; }
        public int    getPort()          { return port; }
        public String getPlainPassword() { return plainPassword; }

        /** ROT13 — trivial reversible transform for demo only. */
        static String rot13(String s) {
            StringBuilder sb = new StringBuilder(s.length());
            for (char c : s.toCharArray()) {
                if      (c >= 'a' && c <= 'z') sb.append((char)('a' + (c - 'a' + 13) % 26));
                else if (c >= 'A' && c <= 'Z') sb.append((char)('A' + (c - 'A' + 13) % 26));
                else sb.append(c);
            }
            return sb.toString();
        }
    }

    // ── Externalizable ────────────────────────────────────────────────────────

    /**
     * Externalizable gives full control: you must serialize every field.
     * The class must have a public no-arg constructor (JVM calls it before readExternal).
     *
     * Use when: you need maximum performance, selective serialization,
     * or a stable binary format regardless of field changes.
     */
    public static class Point3D implements Externalizable {
        @Serial
        private static final long serialVersionUID = 1L;

        private double x, y, z;

        public Point3D() {}   // required by Externalizable

        public Point3D(double x, double y, double z) {
            this.x = x; this.y = y; this.z = z;
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeDouble(x);
            out.writeDouble(y);
            out.writeDouble(z);
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException {
            x = in.readDouble();
            y = in.readDouble();
            z = in.readDouble();
        }

        public double getX() { return x; }
        public double getY() { return y; }
        public double getZ() { return z; }

        @Override public String toString() {
            return "Point3D(%.2f, %.2f, %.2f)".formatted(x, y, z);
        }
    }

    // ── Serialize / deserialize helpers ──────────────────────────────────────

    /** Serialize a single object to a byte array. */
    public static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
        }
        return baos.toByteArray();
    }

    /**
     * Deserialize from bytes with an ObjectInputFilter to block unexpected types.
     * The filter rejects any class not in the allowed list.
     */
    @SuppressWarnings("unchecked")
    public static <T> T deserialize(byte[] data, Class<T> expected) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data))) {
            // Allowlist filter — reject anything not explicitly permitted
            ois.setObjectInputFilter(filterInfo -> {
                Class<?> clazz = filterInfo.serialClass();
                if (clazz == null) return ObjectInputFilter.Status.UNDECIDED;
                String name = clazz.getName();
                if (name.startsWith("com.javatraining.io.")
                        || name.startsWith("java.lang.")
                        || name.startsWith("java.util.")
                        || clazz.isArray()) {
                    return ObjectInputFilter.Status.ALLOWED;
                }
                return ObjectInputFilter.Status.REJECTED;
            });
            return expected.cast(ois.readObject());
        }
    }

    /** Serialize a list of objects to a single byte stream. */
    public static byte[] serializeList(List<?> items) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeInt(items.size());
            for (Object item : items) {
                oos.writeObject(item);
            }
        }
        return baos.toByteArray();
    }

    /** Deserialize a list written by serializeList. */
    @SuppressWarnings("unchecked")
    public static <T> List<T> deserializeList(byte[] data, Class<T> elementType)
            throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data))) {
            int size = ois.readInt();
            List<T> result = new java.util.ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                result.add(elementType.cast(ois.readObject()));
            }
            return result;
        }
    }

    // ── Deep copy via serialization ───────────────────────────────────────────

    /**
     * Serialization can clone an entire object graph — every reachable
     * Serializable object is copied.  Slow, but works for any depth.
     * Only use this as a last resort; prefer copy constructors.
     */
    public static <T extends Serializable> T deepCopy(T obj) throws IOException, ClassNotFoundException {
        byte[] bytes = serialize(obj);
        @SuppressWarnings("unchecked")
        T copy = (T) deserialize(bytes, obj.getClass());
        return copy;
    }
}
