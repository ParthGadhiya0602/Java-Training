package com.javatraining.io;

import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SerializationDemo")
class SerializationDemoTest {

    @Nested
    @DisplayName("Basic Serializable")
    class Basic {
        @Test void product_roundtrip_preserves_fields() throws Exception {
            SerializationDemo.Product p = new SerializationDemo.Product("Widget", 9.99, "tools");
            byte[] bytes = SerializationDemo.serialize(p);
            SerializationDemo.Product p2 = SerializationDemo.deserialize(bytes, SerializationDemo.Product.class);
            assertEquals("Widget", p2.getName());
            assertEquals(9.99,     p2.getPrice(), 0.001);
            assertEquals("tools",  p2.getCategory());
        }

        @Test void transient_field_is_zero_after_deserialization() throws Exception {
            SerializationDemo.Product p = new SerializationDemo.Product("X", 1.0, "cat");
            p.setCachedTax(0.2);
            byte[] bytes = SerializationDemo.serialize(p);
            SerializationDemo.Product p2 = SerializationDemo.deserialize(bytes, SerializationDemo.Product.class);
            assertEquals(0.0, p2.getCachedTax(), 0.0001); // transient — reset to default
        }
    }

    @Nested
    @DisplayName("Custom readObject/writeObject")
    class CustomHooks {
        @Test void secureConfig_password_encrypted_on_wire() throws Exception {
            SerializationDemo.SecureConfig cfg =
                new SerializationDemo.SecureConfig("db.host", 5432, "secret");
            byte[] bytes = SerializationDemo.serialize(cfg);

            // Raw bytes should NOT contain "secret" in plaintext
            String raw = new String(bytes);
            assertFalse(raw.contains("secret"), "plaintext password must not appear in serialized form");
        }

        @Test void secureConfig_password_decrypted_on_load() throws Exception {
            SerializationDemo.SecureConfig cfg =
                new SerializationDemo.SecureConfig("db.host", 5432, "secret");
            byte[] bytes = SerializationDemo.serialize(cfg);
            SerializationDemo.SecureConfig cfg2 =
                SerializationDemo.deserialize(bytes, SerializationDemo.SecureConfig.class);
            assertEquals("secret", cfg2.getPlainPassword());
            assertEquals("db.host", cfg2.getHost());
            assertEquals(5432, cfg2.getPort());
        }

        @Test void rot13_is_its_own_inverse() {
            assertEquals("secret", SerializationDemo.SecureConfig.rot13(
                SerializationDemo.SecureConfig.rot13("secret")));
        }
    }

    @Nested
    @DisplayName("Externalizable")
    class Externalizable {
        @Test void point3d_roundtrip() throws Exception {
            SerializationDemo.Point3D p = new SerializationDemo.Point3D(1.5, 2.5, 3.5);
            byte[] bytes = SerializationDemo.serialize(p);
            SerializationDemo.Point3D p2 =
                SerializationDemo.deserialize(bytes, SerializationDemo.Point3D.class);
            assertEquals(1.5, p2.getX(), 0.0001);
            assertEquals(2.5, p2.getY(), 0.0001);
            assertEquals(3.5, p2.getZ(), 0.0001);
        }

        @Test void point3d_origin_roundtrip() throws Exception {
            SerializationDemo.Point3D origin = new SerializationDemo.Point3D(0, 0, 0);
            byte[] bytes = SerializationDemo.serialize(origin);
            SerializationDemo.Point3D p2 =
                SerializationDemo.deserialize(bytes, SerializationDemo.Point3D.class);
            assertEquals(0.0, p2.getX(), 0.0001);
        }
    }

    @Nested
    @DisplayName("List serialization")
    class ListSerialization {
        @Test void serializeList_deserializeList_roundtrip() throws Exception {
            List<SerializationDemo.Product> products = List.of(
                new SerializationDemo.Product("A", 1.0, "cat1"),
                new SerializationDemo.Product("B", 2.0, "cat2")
            );
            byte[] bytes = SerializationDemo.serializeList(products);
            List<SerializationDemo.Product> result =
                SerializationDemo.deserializeList(bytes, SerializationDemo.Product.class);
            assertEquals(2, result.size());
            assertEquals("A", result.get(0).getName());
            assertEquals("B", result.get(1).getName());
        }
    }

    @Nested
    @DisplayName("Deep copy")
    class DeepCopy {
        @Test void deepCopy_creates_independent_copy() throws Exception {
            SerializationDemo.Product original = new SerializationDemo.Product("X", 5.0, "tools");
            SerializationDemo.Product copy = SerializationDemo.deepCopy(original);
            assertNotSame(original, copy);
            assertEquals(original.getName(),  copy.getName());
            assertEquals(original.getPrice(), copy.getPrice(), 0.001);
        }
    }
}
