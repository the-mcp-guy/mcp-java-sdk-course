# MCP Java SDK Course — Companion Code

Companion code for the **Building MCP Servers in Java** course at
[themcpguy.com](https://themcpguy.com).

This branch (`class_1`) corresponds to
**[Module 1: Environment Setup](https://themcpguy.com/docs/mcp-java-sdk/environment-setup)**.

The code here is the end state of that module: a Maven project targeting Java 21
with the MCP Java SDK on the classpath, Logback wired up, and a small sanity-check
class that proves the SDK actually resolved.

## Branches

Each class in the course has its own branch, so you can check out the exact state
of the project at any point in the series. `main` holds no code — it's just the
index that points at these.

| Branch    | Module                                                                                    |
| --------- | ----------------------------------------------------------------------------------------- |
| `class_1` | [Module 1 — Environment Setup](https://themcpguy.com/docs/mcp-java-sdk/environment-setup) |

```bash
git clone https://github.com/the-mcp-guy/mcp-java-sdk-course.git
cd mcp-java-sdk-course
git checkout class_1
```

## Prerequisites

- **Java 17 minimum, Java 21 recommended.** The MCP Java SDK requires 17; the
  course targets 21 so we can use virtual threads later on.
- **Maven 3.9+**
- **Claude Desktop** (used from the next module onwards to actually launch the server)

Check your JDK:

```bash
java -version
mvn -version
```

You want `21.0.x` (or at least `17.0.x`) from both — note that `mvn -version`
reports the JDK Maven itself is running on, which is the one that matters.

## Build and run

```bash
mvn clean package
java -jar target/mcp-java-sdk-course-1.0.0-SNAPSHOT.jar
```

Expected output:

```
MCP SDK available: io.modelcontextprotocol.server.McpServer
```

If you see that line, the SDK resolved correctly and the environment is ready.
A `ClassNotFoundException` or `NoClassDefFoundError` here means the dependencies
didn't download — re-run with `mvn -U clean package` to force an update check.

## Project layout

```
pom.xml                                     Maven build
src/main/java/com/themcpguy/HelloMcpServer.java   Sanity check — prints the SDK class name
src/main/resources/logback.xml              Logging config (see the note below)
```

## Notes on the build

A few choices in `pom.xml` are deliberate and worth understanding:

**`mcp-core` + `mcp-json-jackson2`, not the bundled `mcp` artifact.**
The umbrella `mcp` artifact pulls in `mcp-json-jackson3`, which uses Jackson 3's
`tools.jackson.*` packages. We take `mcp-core` together with `mcp-json-jackson2`
so you get the familiar `com.fasterxml.jackson` `ObjectMapper` that most existing
Java code and documentation assumes.

**Logging goes to stderr, never stdout.**
An MCP server over stdio uses **stdout as the protocol channel** — every byte
written there has to be a JSON-RPC message. A stray `System.out.println` will
corrupt the stream and the client will drop the connection. That's why
`logback.xml` pins the console appender to `System.err`. Keep it that way.

**The shade plugin builds a fat JAR.**
Claude Desktop launches your server as a plain `java -jar` process with no
classpath management, so all dependencies have to be inside a single JAR.
Shade also writes a `dependency-reduced-pom.xml` into the project root on every
`mvn package` — that's a build artifact, and it's gitignored.

**`maven.compiler.release`, not `source`/`target`.**
`release` is what the compiler actually honours, and it also restricts the API
you compile against to the target version — so you can't accidentally link a
newer JDK method that would fail at runtime.

## License

MIT
