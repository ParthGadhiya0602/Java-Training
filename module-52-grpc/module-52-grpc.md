---
title: "Module 52 — gRPC with Spring Boot"
parent: "Phase 6 — Production & Architecture"
nav_order: 52
render_with_liquid: false
---
{% raw %}

[View source on GitHub](https://github.com/ParthGadhiya0602/Java-Training/tree/main/module-52-grpc/src){: .btn .btn-outline }

# Module 52 — gRPC with Spring Boot

## What this module covers

Protocol Buffers schema definition, code generation via `protobuf-maven-plugin`,
a Spring Boot gRPC server with `net.devh:grpc-server-spring-boot-starter`,
a `@GrpcGlobalServerInterceptor` for cross-cutting concerns, and
in-process unit testing with `InProcessServerBuilder` — no Spring context, no port binding.

---

## Project structure

```
src/main/proto/
└── book.proto                       # schema: GetBook (unary), ListBooks (server-streaming)

src/main/java/com/javatraining/grpc/
├── GrpcApplication.java
├── service/
│   └── BookServiceImpl.java         # @GrpcService, in-memory data, unary + streaming
└── interceptor/
    └── LoggingInterceptor.java      # @GrpcGlobalServerInterceptor

src/test/java/com/javatraining/grpc/service/
└── BookServiceImplTest.java         # pure JUnit 5, InProcessServerBuilder, 4 tests
```

---

## Proto schema

```protobuf
syntax = "proto3";

option java_multiple_files = true;
option java_package = "com.javatraining.grpc.proto";

service BookService {
    rpc GetBook(GetBookRequest) returns (BookResponse);
    rpc ListBooks(ListBooksRequest) returns (stream BookResponse);
}

message GetBookRequest  { int64  id    = 1; }
message ListBooksRequest { string genre = 1; }
message BookResponse    { int64 id = 1; string title = 2; string genre = 3; string author = 4; }
```

`protobuf-maven-plugin` runs `protoc` at `generate-sources`, producing Java stubs in
`target/generated-sources/protobuf/`. `os-maven-plugin` detects the OS/arch so Maven
pulls the correct `protoc` and `protoc-gen-grpc-java` binaries automatically.

---

## Service implementation

```java
@GrpcService
public class BookServiceImpl extends BookServiceGrpc.BookServiceImplBase {

    @Override
    public void getBook(GetBookRequest request, StreamObserver<BookResponse> responseObserver) {
        BookResponse book = BOOKS.get(request.getId());
        if (book == null) {
            responseObserver.onError(
                Status.NOT_FOUND.withDescription("Book not found: " + request.getId())
                                .asRuntimeException());
            return;
        }
        responseObserver.onNext(book);
        responseObserver.onCompleted();
    }

    @Override
    public void listBooks(ListBooksRequest request, StreamObserver<BookResponse> responseObserver) {
        BOOKS.values().stream()
             .filter(b -> request.getGenre().isBlank() || b.getGenre().equalsIgnoreCase(request.getGenre()))
             .forEach(responseObserver::onNext);
        responseObserver.onCompleted();
    }
}
```

The gRPC streaming contract:
- Unary: call `onNext` once, then `onCompleted`
- Server-streaming: call `onNext` N times, then `onCompleted`
- Errors: call `onError` instead of `onCompleted`

---

## Server interceptor

```java
@Slf4j
@Component
@GrpcGlobalServerInterceptor
public class LoggingInterceptor implements ServerInterceptor {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        log.info("gRPC call: {}", call.getMethodDescriptor().getFullMethodName());
        return next.startCall(call, headers);
    }
}
```

`@GrpcGlobalServerInterceptor` registers the bean as an interceptor for every service
on the server — equivalent to adding it manually to every `ServerBuilder`. No explicit
wiring in `BookServiceImpl` needed.

---

## In-process testing

```java
@BeforeEach
void setUp() throws IOException {
    server = InProcessServerBuilder
            .forName(SERVER_NAME)
            .directExecutor()
            .addService(new BookServiceImpl())
            .intercept(new LoggingInterceptor())
            .build().start();

    channel = InProcessChannelBuilder.forName(SERVER_NAME).directExecutor().build();
    stub    = BookServiceGrpc.newBlockingStub(channel);
}

@AfterEach
void tearDown() throws InterruptedException {
    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    server.shutdown().awaitTermination(5, TimeUnit.SECONDS);
}
```

`InProcessServerBuilder` wires the service and interceptor in-memory — no TCP,
no Spring context. `directExecutor()` makes calls synchronous, so assertions run
on the same thread with no async coordination.

Blocking stub for unary:

```java
BookResponse response = stub.getBook(GetBookRequest.newBuilder().setId(1L).build());
assertThat(response.getTitle()).isEqualTo("Effective Java");
```

Blocking stub for server-streaming:

```java
List<BookResponse> responses = new ArrayList<>();
stub.listBooks(ListBooksRequest.newBuilder().setGenre("Programming").build())
    .forEachRemaining(responses::add);
assertThat(responses).hasSize(3);
```

Error assertion:

```java
assertThatThrownBy(() -> stub.getBook(GetBookRequest.newBuilder().setId(999L).build()))
    .isInstanceOf(StatusRuntimeException.class)
    .hasMessageContaining("NOT_FOUND");
```

---

## gRPC vs REST trade-offs

| Dimension        | gRPC                              | REST/HTTP             |
|------------------|-----------------------------------|-----------------------|
| Protocol         | HTTP/2, binary (Protobuf)         | HTTP/1.1+, text (JSON)|
| Schema           | Mandatory (`.proto`)              | Optional (OpenAPI)    |
| Code generation  | Yes — client + server stubs       | Optional              |
| Streaming        | Unary, server, client, bidi       | SSE / WebSocket       |
| Browser support  | Needs grpc-web proxy              | Native                |
| Latency          | Lower (binary, multiplexed)       | Higher                |
| Discoverability  | Reflection API                    | Swagger UI            |

gRPC is the right choice for internal service-to-service calls with high throughput
or streaming requirements; REST is better for public-facing APIs where browser or
third-party client support matters.

---

## Tests

| Class                  | Type        | Count |
|------------------------|-------------|-------|
| `BookServiceImplTest`  | JUnit 5     | 4     |

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk@21 mvn test`
Result: **4/4 pass**

---

## Key decisions

| Decision | Reason |
|---|---|
| `InProcessServerBuilder` over `@SpringBootTest` | No Spring context needed; service has no Spring dependencies in production logic; tests are faster and hermetic |
| `directExecutor()` | Keeps calls synchronous — no `CountDownLatch` or `CompletableFuture` needed in tests |
| `grpc-server-spring-boot-starter` over manual `ServerBuilder` | Auto-configures port, TLS, health check, and interceptor discovery from Spring beans |
| `@GrpcGlobalServerInterceptor` over per-service registration | Interceptor applies automatically to all current and future services; zero wiring |
| `Status.NOT_FOUND.asRuntimeException()` | gRPC status codes are the standard error signalling mechanism; clients check `StatusRuntimeException.getStatus().getCode()` |
{% endraw %}
