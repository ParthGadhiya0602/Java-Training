package com.javatraining.reflection;

import com.javatraining.reflection.SampleClasses.*;
import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FieldAndMethodAccess")
class FieldAndMethodAccessTest {

    @Nested
    @DisplayName("Reading fields")
    class ReadFields {
        @Test void read_private_field() throws Exception {
            Person p = new Person("Alice", 30);
            assertEquals("Alice", FieldAndMethodAccess.readField(p, "name"));
            assertEquals(30, FieldAndMethodAccess.readField(p, "age"));
        }

        @Test void read_static_field() throws Exception {
            Object val = FieldAndMethodAccess.readStaticField(Person.class, "SPECIES");
            assertEquals("Homo sapiens", val);
        }
    }

    @Nested
    @DisplayName("Writing fields")
    class WriteFields {
        @Test void write_private_field() throws Exception {
            Person p = new Person("Bob", 25);
            FieldAndMethodAccess.writeField(p, "name", "Charlie");
            assertEquals("Charlie", p.getName());
        }

        @Test void write_int_field() throws Exception {
            Person p = new Person("Dave", 20);
            FieldAndMethodAccess.writeField(p, "age", 99);
            assertEquals(99, p.getAge());
        }
    }

    @Nested
    @DisplayName("Invoking methods")
    class InvokeMethods {
        @Test void invoke_private_method() throws Exception {
            SecretBox box = new SecretBox();
            Object result = FieldAndMethodAccess.invokeNoArgMethod(box, "getSecret");
            assertEquals("initial", result);
        }

        @Test void invoke_private_setter_then_read() throws Exception {
            SecretBox box = new SecretBox();
            FieldAndMethodAccess.invokeMethod(box, "setSecret", "changed");
            Object result = FieldAndMethodAccess.invokeNoArgMethod(box, "getSecret");
            assertEquals("changed", result);
        }

        @Test void invoke_public_method() throws Exception {
            Person p = new Person("Eve", 28);
            Object result = FieldAndMethodAccess.invokeNoArgMethod(p, "getName");
            assertEquals("Eve", result);
        }
    }

    @Nested
    @DisplayName("Constructor invocation")
    class ConstructorInvocation {
        @Test void new_instance_with_args() throws Exception {
            Person p = FieldAndMethodAccess.newInstance(Person.class, "Frank", 40);
            assertEquals("Frank", p.getName());
            assertEquals(40, p.getAge());
        }

        @Test void new_instance_no_arg() throws Exception {
            Person p = FieldAndMethodAccess.newInstanceNoArg(Person.class);
            assertNotNull(p);
            assertNull(p.getName());
        }
    }

    @Nested
    @DisplayName("Generic type introspection")
    class GenericTypes {
        @Test void generic_superclass_type_arg() {
            Class<?> typeArg = FieldAndMethodAccess.getGenericSuperclassTypeArg(
                StringContainer.class, 0);
            assertEquals(String.class, typeArg);
        }
    }

    @Nested
    @DisplayName("Snapshot and copy")
    class SnapshotCopy {
        @Test void snapshot_captures_all_fields() throws Exception {
            Person p = new Person("Grace", 35);
            Map<String, Object> snap = FieldAndMethodAccess.snapshot(p);
            assertEquals("Grace", snap.get("name"));
            assertEquals(35, snap.get("age"));
        }

        @Test void shallow_copy_fields() throws Exception {
            Person src = new Person("Heidi", 22);
            Person dst = new Person();
            FieldAndMethodAccess.shallowCopyFields(src, dst);
            assertEquals("Heidi", dst.getName());
            assertEquals(22, dst.getAge());
        }

        @Test void shallow_copy_requires_same_class() {
            assertThrows(IllegalArgumentException.class, () ->
                FieldAndMethodAccess.shallowCopyFields(new Person(), new SecretBox()));
        }
    }
}
