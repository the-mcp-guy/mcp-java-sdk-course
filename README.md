# MCP Java SDK Course — Companion Code

Companion code for the **Building MCP Servers in Java** course at
[themcpguy.com](https://themcpguy.com).

This branch (`class_2`) corresponds to
**[Class 2: Your First MCP Server](https://themcpguy.com/docs/mcp-java-sdk/your-first-mcp-server)**.

The code here is the end state of that module: a working MCP server speaking
JSON-RPC over stdio, advertising a single `echo` tool that a client such as
Claude Desktop can discover and call.

## Branches

Each class in the course has its own branch, so you can check out the exact state
of the project at any point in the series. `main` holds no code — it's just the
index that points at these.

| Branch    | Module                                                                                          |
| --------- | ----------------------------------------------------------------------------------------------- |
| `class_1` | [Class 1 — Environment Setup](https://themcpguy.com/docs/mcp-java-sdk/environment-setup)         |
| `class_2` | [Class 2 — Your First MCP Server](https://themcpguy.com/docs/mcp-java-sdk/your-first-mcp-server) |

```bash
git clone https://github.com/the-mcp-guy/mcp-java-sdk-course.git
cd mcp-java-sdk-course
git checkout class_2
```

## Prerequisites

- **Java 17 minimum, Java 21 recommended.** The MCP Java SDK requires 17; the
  course targets 21 so we can use virtual threads later on.
- **Maven 3.9+**
- **Claude Desktop**, which launches the server and acts as the MCP client

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

The server prints one startup line **to stderr** and then blocks, waiting for
JSON-RPC messages on stdin:

```
12:00:00 [main] INFO  com.themcpguy.HelloMcpServer - hello-mcp-server started (stdio); awaiting messages on stdin
```

That silence is correct — it isn't hung. A stdio MCP server is not meant to be
driven by hand; it waits for a client to speak first. Press `Ctrl+C` to stop it.

If the process exits immediately instead, the dependencies likely didn't resolve —
re-run with `mvn -U clean package` to force an update check.

## Connect it to Claude Desktop

Add the server to `claude_desktop_config.json`, using an **absolute** path to the
JAR (Claude Desktop does not expand `~` or resolve relative paths):

- macOS: `~/Library/Application Support/Claude/claude_desktop_config.json`
- Windows: `%APPDATA%\Claude\claude_desktop_config.json`

```json
{
  "mcpServers": {
    "my-first-server": {
      "command": "java",
      "args": ["-jar", "/absolute/path/to/mcp-java-sdk-course/target/mcp-java-sdk-course-1.0.0-SNAPSHOT.jar"]
    }
  }
}
```

Fully quit and reopen Claude Desktop — reloading the window is not enough, since
the config is read once at startup. Open a new conversation, check that `echo`
appears in the tools list, then try:

> Use the echo tool to send me 'Hello from Java'

The server replies `Echo: Hello from Java`. The point isn't the output — it's that
a round trip just completed through the whole path: capability negotiation,
`tools/list`, `tools/call`, and back.

## Project layout

```
pom.xml                                           Maven build
src/main/java/com/themcpguy/HelloMcpServer.java   The MCP server — one 'echo' tool over stdio
src/main/resources/logback.xml                    Logging config (see the note below)
```

## Notes on the code

**The tool's input schema is passed to `Tool.builder(...)`, not set fluently.**
In SDK 2.0.0 the no-arg `Tool.builder()`, the `.inputSchema(JsonSchema)` setter,
and the `JsonSchema` record are all deprecated. The supported forms take the name
and schema together:

```java
Tool.builder(name, jsonMapper, schemaJsonString)   // JSON text — used here
Tool.builder(name, Map<String, Object> schema)     // pre-built map
```

A tool without an input schema isn't valid MCP, so the API makes it impossible to
build one. Older snippets that call `Tool.builder().name(...).inputSchema(...)`
still compile, but emit deprecation warnings. Passing the schema as a Java text
block also means you can paste JSON Schema straight from the MCP spec, instead of
hand-translating it into nested `Map.of(...)` calls where a typo'd key compiles
fine and only misbehaves at runtime.

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
