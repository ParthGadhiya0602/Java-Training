package com.javatraining.encapsulation;

import java.util.*;

/**
 * TOPIC: Builder pattern — three variants
 *
 * Problem: a class with many optional fields leads to telescoping constructors:
 *   new Pizza("large", "thin", true, false, null, "extra", "regular", null, false)
 * Which argument is which? In what order? Are nulls valid?
 *
 * Solution 1 — Classic Builder:
 *   Required fields in the Builder constructor.
 *   Optional fields as fluent setters on the Builder.
 *   build() validates the complete state.
 *
 * Solution 2 — Step Builder (staged interface chain):
 *   Each required field is a separate interface step.
 *   Compiler forces you to set required fields in order.
 *   Cannot call build() until all required fields are set.
 *
 * Solution 3 — Copy Builder (toBuilder()):
 *   Immutable object provides toBuilder() returning a pre-filled Builder.
 *   Change only what you need, call build() for the new instance.
 */
public class BuilderPattern {

    // -------------------------------------------------------------------------
    // 1. Classic Builder — Pizza example
    // -------------------------------------------------------------------------
    static final class Pizza {
        // Required
        private final String  size;      // SMALL / MEDIUM / LARGE
        private final String  crust;     // THIN / THICK / STUFFED

        // Optional
        private final boolean extraCheese;
        private final boolean extraSauce;
        private final List<String> toppings;
        private final String  notes;

        private Pizza(Builder b) {
            this.size         = b.size;
            this.crust        = b.crust;
            this.extraCheese  = b.extraCheese;
            this.extraSauce   = b.extraSauce;
            // Defensive copy of mutable list from builder
            this.toppings     = Collections.unmodifiableList(new ArrayList<>(b.toppings));
            this.notes        = b.notes;
        }

        // Getters
        String       size()         { return size; }
        String       crust()        { return crust; }
        boolean      extraCheese()  { return extraCheese; }
        boolean      extraSauce()   { return extraSauce; }
        List<String> toppings()     { return new ArrayList<>(toppings); } // defensive copy
        String       notes()        { return notes; }

        // Copy builder — return a pre-filled builder for non-destructive updates
        Builder toBuilder() { return new Builder(this); }

        @Override
        public String toString() {
            return String.format("Pizza{%s/%s cheese=%s sauce=%s toppings=%s notes=%s}",
                size, crust, extraCheese, extraSauce, toppings,
                notes != null ? "\"" + notes + "\"" : "none");
        }

        // ── Builder ──────────────────────────────────────────────────────────
        static final class Builder {
            // Required — set in constructor
            private final String size;
            private final String crust;

            // Optional — default values
            private boolean      extraCheese = false;
            private boolean      extraSauce  = false;
            private List<String> toppings    = new ArrayList<>();
            private String       notes       = null;

            // New build — requires the two mandatory fields
            Builder(String size, String crust) {
                if (size  == null || size.isBlank())  throw new IllegalArgumentException("size");
                if (crust == null || crust.isBlank()) throw new IllegalArgumentException("crust");
                this.size  = size.toUpperCase();
                this.crust = crust.toUpperCase();
            }

            // Copy constructor — pre-fill from existing Pizza
            private Builder(Pizza pizza) {
                this.size        = pizza.size;
                this.crust       = pizza.crust;
                this.extraCheese = pizza.extraCheese;
                this.extraSauce  = pizza.extraSauce;
                this.toppings    = new ArrayList<>(pizza.toppings);
                this.notes       = pizza.notes;
            }

            Builder extraCheese()                  { extraCheese = true;    return this; }
            Builder extraSauce()                   { extraSauce  = true;    return this; }
            Builder topping(String topping)        { toppings.add(topping); return this; }
            Builder toppings(String... tops)       {
                Collections.addAll(toppings, tops); return this;
            }
            Builder notes(String n)                { notes = n;             return this; }
            Builder noExtraCheese()                { extraCheese = false;   return this; }

            Pizza build() { return new Pizza(this); }
        }
    }

    // -------------------------------------------------------------------------
    // 2. Step Builder — compile-time enforcement of required fields
    //    Interfaces form a chain: NameStep → EmailStep → BuildStep
    //    You cannot skip a step or call build() early.
    // -------------------------------------------------------------------------
    static final class Employee {
        private final String  name;       // required
        private final String  email;      // required
        private final String  department; // required
        private final String  phone;      // optional
        private final String  title;      // optional
        private final double  salary;     // optional (0 = unset)
        private final boolean remote;     // optional

        private Employee(StepBuilder b) {
            this.name       = b.name;
            this.email      = b.email;
            this.department = b.department;
            this.phone      = b.phone;
            this.title      = b.title;
            this.salary     = b.salary;
            this.remote     = b.remote;
        }

        String  name()       { return name; }
        String  email()      { return email; }
        String  department() { return department; }
        String  phone()      { return phone; }
        String  title()      { return title; }
        double  salary()     { return salary; }
        boolean remote()     { return remote; }

        // ── Step interfaces ──────────────────────────────────────────────────
        interface NameStep       { EmailStep      name(String name); }
        interface EmailStep      { DepartmentStep email(String email); }
        interface DepartmentStep { BuildStep      department(String dept); }

        // BuildStep: all optional fields + build()
        interface BuildStep {
            BuildStep phone(String phone);
            BuildStep title(String title);
            BuildStep salary(double salary);
            BuildStep remote(boolean remote);
            Employee  build();
        }

        // ── Concrete builder implementing all steps ───────────────────────────
        static final class StepBuilder implements NameStep, EmailStep, DepartmentStep, BuildStep {
            private String  name;
            private String  email;
            private String  department;
            private String  phone  = null;
            private String  title  = null;
            private double  salary = 0;
            private boolean remote = false;

            @Override public EmailStep      name(String n)       { name       = validate(n, "name");  return this; }
            @Override public DepartmentStep email(String e)      { email      = validate(e, "email"); return this; }
            @Override public BuildStep      department(String d) { department = validate(d, "dept");  return this; }
            @Override public BuildStep      phone(String p)      { phone      = p;                    return this; }
            @Override public BuildStep      title(String t)      { title      = t;                    return this; }
            @Override public BuildStep      salary(double s)     { salary     = s;                    return this; }
            @Override public BuildStep      remote(boolean r)    { remote     = r;                    return this; }
            @Override public Employee       build()              { return new Employee(this); }

            private String validate(String v, String field) {
                if (v == null || v.isBlank())
                    throw new IllegalArgumentException(field + " must not be blank");
                return v;
            }
        }

        // Entry point — returns the FIRST step (NameStep); nothing else is visible
        static NameStep builder() { return new StepBuilder(); }

        @Override
        public String toString() {
            return String.format(
                "Employee{name=%s, email=%s, dept=%s, title=%s, salary=%.0f, remote=%s}",
                name, email, department, title, salary, remote);
        }
    }

    // -------------------------------------------------------------------------
    // 3. HTTP Request — builder with validation in build(), copy builder
    // -------------------------------------------------------------------------
    static final class HttpRequest {
        private final String         method;
        private final String         url;
        private final Map<String,String> headers;
        private final String         body;
        private final int            timeoutMs;

        private HttpRequest(Builder b) {
            this.method    = b.method;
            this.url       = b.url;
            this.headers   = Collections.unmodifiableMap(new LinkedHashMap<>(b.headers));
            this.body      = b.body;
            this.timeoutMs = b.timeoutMs;
        }

        String              method()    { return method; }
        String              url()       { return url; }
        Map<String,String>  headers()   { return headers; }
        String              body()      { return body; }
        int                 timeoutMs() { return timeoutMs; }

        Builder toBuilder()             { return new Builder(this); }

        @Override
        public String toString() {
            return String.format("HttpRequest{%s %s timeout=%dms headers=%s body=%s}",
                method, url, timeoutMs, headers,
                body != null ? "\"" + body.substring(0, Math.min(body.length(), 20)) + "...\"" : "none");
        }

        static final class Builder {
            private String              method    = "GET";
            private String              url;
            private Map<String,String>  headers   = new LinkedHashMap<>();
            private String              body      = null;
            private int                 timeoutMs = 5_000;

            Builder() {}

            private Builder(HttpRequest req) {
                this.method    = req.method;
                this.url       = req.url;
                this.headers   = new LinkedHashMap<>(req.headers);
                this.body      = req.body;
                this.timeoutMs = req.timeoutMs;
            }

            Builder get(String url)    { this.method = "GET";    this.url = url; return this; }
            Builder post(String url)   { this.method = "POST";   this.url = url; return this; }
            Builder put(String url)    { this.method = "PUT";    this.url = url; return this; }
            Builder delete(String url) { this.method = "DELETE"; this.url = url; return this; }
            Builder url(String url)    { this.url    = url;                      return this; }
            Builder method(String m)   { this.method = m.toUpperCase();          return this; }
            Builder header(String k, String v) { headers.put(k, v);             return this; }
            Builder bearer(String token)       { return header("Authorization", "Bearer " + token); }
            Builder json()                     { return header("Content-Type", "application/json"); }
            Builder body(String body)  { this.body      = body;                  return this; }
            Builder timeout(int ms)    { this.timeoutMs = ms;                    return this; }

            HttpRequest build() {
                if (url == null || url.isBlank())
                    throw new IllegalStateException("URL is required");
                return new HttpRequest(this);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Demonstrations
    // -------------------------------------------------------------------------
    static void pizzaDemo() {
        System.out.println("=== Classic Builder (Pizza) ===");

        Pizza margherita = new Pizza.Builder("large", "thin")
            .extraCheese()
            .topping("Tomato")
            .topping("Basil")
            .build();

        Pizza bbqChicken = new Pizza.Builder("medium", "stuffed")
            .extraSauce()
            .toppings("BBQ Chicken", "Onion", "Peppers")
            .notes("Well done please")
            .build();

        System.out.println(margherita);
        System.out.println(bbqChicken);

        // Copy builder — make a variant without extra cheese
        Pizza lighter = margherita.toBuilder().noExtraCheese().build();
        System.out.println("Lighter copy: " + lighter);
    }

    static void stepBuilderDemo() {
        System.out.println("\n=== Step Builder (Employee — compile-time required fields) ===");

        // Compiler enforces: name → email → department → (optionals) → build()
        // You cannot skip to build() without going through name/email/dept first.
        Employee alice = Employee.builder()
            .name("Alice Sharma")
            .email("alice@company.com")
            .department("Engineering")
            .title("Senior Engineer")
            .salary(120_000)
            .remote(true)
            .build();

        Employee bob = Employee.builder()
            .name("Bob Patel")
            .email("bob@company.com")
            .department("Marketing")
            .build();  // optional fields omitted — defaults apply

        System.out.println(alice);
        System.out.println(bob);

        // Validation
        try {
            Employee.builder().name("").email("x@y.com").department("HR").build();
        } catch (IllegalArgumentException e) {
            System.out.println("Caught: " + e.getMessage());
        }
    }

    static void httpRequestDemo() {
        System.out.println("\n=== HTTP Request Builder + Copy Builder ===");

        HttpRequest getReq = new HttpRequest.Builder()
            .get("https://api.example.com/users")
            .bearer("my-token-123")
            .timeout(10_000)
            .build();
        System.out.println(getReq);

        HttpRequest postReq = new HttpRequest.Builder()
            .post("https://api.example.com/users")
            .json()
            .bearer("my-token-123")
            .body("{\"name\":\"Alice\",\"email\":\"alice@x.com\"}")
            .build();
        System.out.println(postReq);

        // Copy builder — reuse the POST config but change the URL and body
        HttpRequest putReq = postReq.toBuilder()
            .put("https://api.example.com/users/42")
            .body("{\"name\":\"Alice Updated\"}")
            .build();
        System.out.println(putReq);

        try { new HttpRequest.Builder().build(); }
        catch (IllegalStateException e) { System.out.println("Caught: " + e.getMessage()); }
    }

    public static void main(String[] args) {
        pizzaDemo();
        stepBuilderDemo();
        httpRequestDemo();
    }
}
