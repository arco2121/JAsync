# JAsync

A modern, async/await library for Java that brings JavaScript/Python-style asynchronous programming to the JVM. Optimized for both Java 17+ and Java 21+ with Virtual Threads support.

## Features

- **Modern async/await syntax** - Clean, readable asynchronous code
- **Virtual Threads support** - Automatic optimization for Java 21+
- **Parallel operations** - awaitAll, awaitRace, awaitAny
- **Pipeline transformations** - Chain operations elegantly
- **Intervals & delays** - Easy scheduling and timing
- **Async collections** - AsyncList, AsyncArray, AsyncQueue
- **Async I/O** - File, network, and stream operations
- **Annotation support** - @JAsyncable and @JAwaitable with AspectJ
- **Type-safe** - Full generic type support

## Installation

NOTE: If you dont understand how to import the library or dont have credentials, use https://jitpack.io/

### Gradle (Kotlin DSL)

```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/arco2121/JAsync")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("TOKEN")
        }
    }
}

dependencies {
    implementation("com.arco2121.jasync:JAsync:1.0.0")
}
```

### Gradle (Groovy)

```groovy
repositories {
    maven {
        url "https://maven.pkg.github.com/arco2121/JAsync"
        credentials {
            username = project.findProperty("gpr.user") ?: System.getenv("USERNAME")
            password = project.findProperty("gpr.key") ?: System.getenv("TOKEN")
        }
    }
}

dependencies {
    implementation 'com.arco2121.jasync:JAsync:1.0.0'
}
```

### Maven

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/arco2121/JAsync</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.arco2121.jasync</groupId>
        <artifactId>JAsync</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

## Quick Start

### Basic Async/Await

```java
import com.arco2121.jasync.JAsync.Async;
import com.arco2121.jasync.JAsync.Running.Asyncable;

public class Example {
    public static void main(String[] args) {
        // Create an async task
        Asyncable<String> task = Async.async(() -> {
            Thread.sleep(1000);
            return "Hello, Async!";
        });
        
        // Await the result
        String result = Async.await(task);
        System.out.println(result); // Prints after 1 second
    }
}
```

### Chaining Operations

```java
Asyncable<String> result = Async.async(() -> {
    return fetchUserData();
})
.then(userData -> parseUser(userData))
.then(user -> user.getName())
.then(name -> "Welcome, " + name);

String greeting = Async.await(result);
```

### Error Handling

```java
// Using .error()
Asyncable<String> safe = Async.async(() -> {
    return riskyOperation();
}).error(error -> {
    System.err.println("Error: " + error.getMessage());
    return "Default value";
});

// Using tryCatch
String result = Async.await(
    Async.tryCatch(
        () -> dangerousOperation(),
        error -> "Fallback"
    )
);
```

## Core Features

### Parallel Operations

#### awaitAll - Execute all tasks

```java
Asyncable<String> task1 = Async.async(() -> fetchFromDB());
Asyncable<String> task2 = Async.async(() -> fetchFromAPI());
Asyncable<String> task3 = Async.async(() -> fetchFromCache());

// Wait for all to complete
Asyncable<List<String>> all = Async.awaitSafeAll(task1, task2, task3);
List<String> results = Async.await(all);
```

#### awaitRace - First to complete wins

```java
Asyncable<String> fastest = Async.awaitSafeRace(
    Async.async(() -> slowAPI()),
    Async.async(() -> fastCache()),
    Async.async(() -> mediumDatabase())
);

String result = Async.await(fastest); // Returns first completed
```

#### awaitAny - First success or all fail

```java
Asyncable<Data> result = Async.awaitSafeAny(
    Async.async(() -> primarySource()),
    Async.async(() -> secondarySource()),
    Async.async(() -> fallbackSource())
);

Data data = Async.await(result); // First successful result
```

### Pipeline Transformations

```java
Asyncable<String> pipeline = Async.pipe(
    () -> fetchRawData(),           // Step 1: Fetch
    data -> parseData(data),        // Step 2: Parse
    parsed -> validateData(parsed), // Step 3: Validate
    valid -> formatOutput(valid)    // Step 4: Format
);

String output = Async.await(pipeline);
```

### Timing & Scheduling

#### Delayed Execution

```java
// Execute after delay (separate thread)
Async.delayed(() -> {
    System.out.println("Executed after 2 seconds");
}, 2000);

// Wait then execute (same thread)
String result = Async.timeout(() -> {
    return expensiveOperation();
}, 1000); // Wait 1s, then execute
```

#### Intervals

```java
// Create repeating interval
AsyncInterval interval = Async.interval(() -> {
    System.out.println("Tick: " + System.currentTimeMillis());
}, 1000); // Every second

// Stop after 5 seconds
Thread.sleep(5000);
Async.clearInterval(interval);

// Self-stopping interval
Async.interval(self -> {
    System.out.println("Executing...");
    if (shouldStop()) {
        Async.clearInterval(self);
    }
}, 500);

// Clear all intervals
Async.clearAllIntervals();
```

### Async Collections

#### AsyncList - Type-safe async list

```java
AsyncList<Integer> numbers = new AsyncList<>();

// Producer
Async.async(() -> {
    for (int i = 0; i < 100; i++) {
        numbers.add(i);
        Thread.sleep(10);
    }
    numbers.complete();
    return null;
});

// Wait for specific element
Integer fifth = Async.await(numbers.awaitGet(4));

// Wait for all
List<Integer> all = Async.await(numbers.awaitToList());
```

#### AsyncQueue - Streaming data

```java
AsyncQueue<String> queue = new AsyncQueue<>();

// Producer
Async.async(() -> {
    queue.add("item1");
    queue.add("item2");
    queue.add("item3");
    queue.close();
    return null;
});

// Consumer - process as they arrive
queue.forEach(item -> {
    System.out.println("Processing: " + item);
});

// Or collect all
List<String> items = Async.await(queue.awaitToList());

// Transform stream
AsyncQueue<Integer> lengths = queue.map(String::length);

// Filter stream
AsyncQueue<String> filtered = queue.filter(s -> s.length() > 5);
```

### Async I/O

#### Reading Files

```java
import com.arco2121.jasync.JAsync.IO.AsyncIO;
import com.arco2121.jasync.JAsync.IO.AsyncIO.Resource;

// Read text file
Asyncable<String> content = AsyncIO.fetchText(
    new Resource("data.txt")
);
String text = Async.await(content);

// Read binary file
Asyncable<MyObject> obj = AsyncIO.fetch(
    new Resource("data.bin"),
    raw -> (MyObject) raw
);
MyObject data = Async.await(obj);

// Stream large file line by line
AsyncQueue<String> lines = AsyncIO.fetchLines(
    new Resource("large-file.txt"),
    line -> line.trim()
);

lines.forEach(line -> processLine(line));
```

#### Writing Files

```java
// Write object
AsyncIO.send(
    new Resource("output.bin"),
    myObject
);

// Write text
AsyncIO.sendText(
    new Resource("output.txt"),
    "Content to write"
);
```

#### Network Requests

```java
// Fetch from URL
Asyncable<String> response = AsyncIO.fetchText(
    new Resource(URI.create("https://api.example.com/data"), 
                 AsyncIO.Method.GET)
);

// POST data
AsyncIO.send(
    new Resource(URI.create("https://api.example.com/submit"),
                 AsyncIO.Method.POST),
    requestData
);
```

### Annotations (AspectJ)

Enable AspectJ in your project, then use annotations:

```java
import com.arco2121.jasync.Types.Annotations.JAsyncable;
import com.arco2121.jasync.Types.Annotations.JAwaitable;

public class Service {
    
    @JAsyncable(delay = 1000)
    public String fetchData() {
        // Automatically becomes async
        // Executes after 1 second delay
        return loadData();
    }
    
    @JAwaitable(timeout = 5000)
    public String processData(String data) {
        // Automatically awaits async results
        // 5 second timeout
        return data.toUpperCase();
    }
}

// Usage
Service service = new Service();
Asyncable<String> dataTask = service.fetchData(); // Returns Asyncable
String result = service.processData("input"); // Waits automatically
```

## Advanced Usage

### Custom Async Selection

```java
// Force old-style (CompletableFuture) even on Java 21+
Async.selectCriteria(() -> true);

// Force Virtual Threads even on Java 17 (will fail if not available)
Async.selectCriteria(() -> false);

// Default: Auto-detect (< Java 21 = CompletableFuture, >= 21 = Virtual Threads)
Async.selectCriteria(() -> Runtime.version().feature() < 21);
```

### Complex Workflows

```java
// Multi-stage async workflow
Asyncable<Report> report = Async.async(() -> {
    // Stage 1: Fetch data in parallel
    var userData = Async.async(() -> fetchUsers());
    var orderData = Async.async(() -> fetchOrders());
    var productData = Async.async(() -> fetchProducts());
    
    List<?> allData = Async.await(Async.awaitAll(userData, orderData, productData));
    
    // Stage 2: Process data
    return generateReport(allData);
}).then(report -> {
    // Stage 3: Validate
    validateReport(report);
    return report;
}).error(error -> {
    // Handle any errors
    logger.error("Report generation failed", error);
    return getDefaultReport();
});

Report finalReport = Async.await(report);
```

### Resource Management

```java
// Async with try-with-resources pattern
Asyncable<String> result = Async.async(() -> {
    try (var resource = acquireResource()) {
        return processResource(resource);
    }
});
```

## Performance

- **Java 17-20**: Uses optimized thread pool (CPU cores)
- **Java 21+**: Automatically uses Virtual Threads for better scalability
- **Zero reflection overhead** in hot paths
- **Minimal allocations** with record-based design

## Testing

JAsync comes with comprehensive test coverage:

```bash
./gradlew test
```

See the `src/test/java` directory for example tests.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Inspired by JavaScript's async/await and Python's asyncio
- Built with Java's CompletableFuture and Virtual Threads
- AspectJ for annotation support

## Support

- **Issues**: [GitHub Issues](https://github.com/arco2121/JAsync/issues)
- **Discussions**: [GitHub Discussions](https://github.com/arco2121/JAsync/discussions)


Made by [arco2121](https://github.com/arco2121)
