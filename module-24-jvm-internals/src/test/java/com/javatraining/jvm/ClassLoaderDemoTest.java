package com.javatraining.jvm;

import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ClassLoaderDemo")
class ClassLoaderDemoTest {

    @Nested
    @DisplayName("Loader names")
    class LoaderNames {
        @Test void bootstrap_loads_string() {
            // java.lang.String is loaded by the bootstrap loader → null getClassLoader()
            assertEquals("bootstrap", ClassLoaderDemo.loaderName(String.class));
        }

        @Test void app_loader_loads_our_class() {
            String name = ClassLoaderDemo.loaderName(ClassLoaderDemo.class);
            // AppClassLoader or its name variant
            assertFalse(name.isBlank());
        }
    }

    @Nested
    @DisplayName("Delegation chain")
    class DelegationChain {
        @Test void chain_ends_with_bootstrap() {
            ClassLoader cl = ClassLoaderDemo.systemLoader();
            List<String> chain = ClassLoaderDemo.delegationChain(cl);
            assertFalse(chain.isEmpty());
            assertEquals("bootstrap", chain.get(chain.size() - 1));
        }

        @Test void chain_contains_system_loader() {
            ClassLoader cl = ClassLoaderDemo.systemLoader();
            List<String> chain = ClassLoaderDemo.delegationChain(cl);
            assertTrue(chain.size() >= 2);  // at least AppClassLoader + bootstrap
        }
    }

    @Nested
    @DisplayName("Same loader check")
    class SameLoaderCheck {
        @Test void same_package_classes_share_loader() {
            assertTrue(ClassLoaderDemo.sameLoader(
                ClassLoaderDemo.class, MemoryDemo.class));
        }

        @Test void jdk_classes_share_bootstrap() {
            assertTrue(ClassLoaderDemo.sameLoader(String.class, Integer.class));
        }
    }

    @Nested
    @DisplayName("System/Platform/Context loaders")
    class LoaderAccessors {
        @Test void system_loader_not_null() {
            assertNotNull(ClassLoaderDemo.systemLoader());
        }

        @Test void platform_loader_not_null() {
            assertNotNull(ClassLoaderDemo.platformLoader());
        }

        @Test void context_loader_not_null() {
            assertNotNull(ClassLoaderDemo.contextLoader());
        }
    }

    @Nested
    @DisplayName("tryLoadClass")
    class TryLoad {
        @Test void loads_existing_class() {
            Optional<Class<?>> opt = ClassLoaderDemo.tryLoadClass("java.lang.String");
            assertTrue(opt.isPresent());
            assertEquals(String.class, opt.get());
        }

        @Test void returns_empty_for_missing_class() {
            Optional<Class<?>> opt = ClassLoaderDemo.tryLoadClass("com.example.NoSuchClass");
            assertTrue(opt.isEmpty());
        }
    }

    @Nested
    @DisplayName("tryLoadWithLoader")
    class TryLoadWithLoader {
        @Test void loads_existing_class_with_system_loader() {
            Optional<Class<?>> opt = ClassLoaderDemo.tryLoadWithLoader(
                "java.lang.Integer", ClassLoaderDemo.systemLoader());
            assertTrue(opt.isPresent());
        }
    }

    @Nested
    @DisplayName("ByteArrayClassLoader")
    class ByteArrayLoader {
        @Test void loader_created_with_parent() {
            var loader = new ClassLoaderDemo.ByteArrayClassLoader(
                ClassLoader.getSystemClassLoader());
            assertNotNull(loader);
            assertEquals(ClassLoader.getSystemClassLoader(), loader.getParent());
        }
    }

    @Nested
    @DisplayName("describe")
    class Describe {
        @Test void describe_contains_expected_keys() {
            Map<String, Object> info = ClassLoaderDemo.describe(
                ClassLoaderDemo.systemLoader());
            assertTrue(info.containsKey("type"));
            assertTrue(info.containsKey("name"));
            assertTrue(info.containsKey("parent"));
        }

        @Test void type_is_not_blank() {
            Map<String, Object> info = ClassLoaderDemo.describe(
                ClassLoaderDemo.systemLoader());
            assertFalse(info.get("type").toString().isBlank());
        }
    }
}
