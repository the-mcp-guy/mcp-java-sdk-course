# MCP Java SDK Course — Companion Code

Companion code for the **Building MCP Servers in Java** course at
[themcpguy.com](https://themcpguy.com).

This branch (`class_4`) corresponds to
**[Class 4: Implementing Resources](https://themcpguy.com/docs/mcp-java-sdk/implementing-resources)**.

The code here is the end state of that module: a third server, `acme-resources`,
exposing read-only context rather than actions — one fixed resource, two URI
templates, a binary payload, and live subscription updates. The Class 2 and
Class 3 servers are still present.

## Branches

Each class in the course has its own branch, so you can check out the exact state
of the project at any point in the series. `main` holds no code — it's just the
index that points at these.

| Branch    | Module                                                                                            |
| --------- | ------------------------------------------------------------------------------------------------- |
| `class_1` | [Class 1 — Environment Setup](https://themcpguy.com/docs/mcp-java-sdk/environment-setup)           |
| `class_2` | [Class 2 — Your First MCP Server](https://themcpguy.com/docs/mcp-java-sdk/your-first-mcp-server)   |
| `class_3` | [Class 3 — Implementing Tools](https://themcpguy.com/docs/mcp-java-sdk/implementing-tools)         |
| `class_4` | [Class 4 — Implementing Resources](https://themcpguy.com/docs/mcp-java-sdk/implementing-resources) |

```bash
git clone https://github.com/the-mcp-guy/mcp-java-sdk-course.git
cd mcp-java-sdk-course
git checkout class_4
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
```

This branch produces **three** servers from the one JAR:

```bash
# Class 2 — the echo server (the JAR's main class)
java -jar target/mcp-java-sdk-course-1.0.0-SNAPSHOT.jar

# Class 3 — acme-tools, selected explicitly by class name
java -cp target/mcp-java-sdk-course-1.0.0-SNAPSHOT.jar com.themcpguy.tools.ToolsMcpServer

# Class 4 — acme-resources
java -cp target/mcp-java-sdk-course-1.0.0-SNAPSHOT.jar com.themcpguy.resources.ResourcesMcpServer
```

Each prints one startup line **to stderr** and then blocks, waiting for
JSON-RPC messages on stdin:

```
12:00:00 [main] INFO  com.themcpguy.resources.ResourcesMcpServer - acme-resources started (stdio); awaiting messages on stdin
```

That silence is correct — it isn't hung. A stdio MCP server is not meant to be
driven by hand; it waits for a client to speak first. Press `Ctrl+C` to stop it.

If the process exits immediately instead, the dependencies likely didn't resolve —
re-run with `mvn -U clean package` to force an update check.

## Connect it to Claude Desktop

Config file location:

- macOS: `~/Library/Application Support/Claude/claude_desktop_config.json`
- Windows: `%APPDATA%\Claude\claude_desktop_config.json`

Paths must be **absolute** — Claude Desktop does not expand `~` or resolve
relative paths. Rather than typing one by hand, print the correct value and copy
it from your terminal:

```bash
echo "$(pwd)/target/mcp-java-sdk-course-1.0.0-SNAPSHOT.jar"
```

Then substitute it into each entry below:

```json
{
  "mcpServers": {
    "my-first-server": {
      "command": "java",
      "args": ["-jar", "PASTE_PATH_HERE"]
    },
    "acme-tools": {
      "command": "java",
      "args": ["-cp", "PASTE_PATH_HERE", "com.themcpguy.tools.ToolsMcpServer"]
    },
    "acme-resources": {
      "command": "java",
      "args": ["-cp", "PASTE_PATH_HERE", "com.themcpguy.resources.ResourcesMcpServer"]
    }
  }
}
```

Note the difference: `my-first-server` uses `-jar` and runs the JAR's manifest
main class; the others use `-cp` and name their main class explicitly. One JAR,
three entry points.

Fully quit and reopen Claude Desktop — reloading the window is not enough, since
the config is read once at startup.

### If your servers disappear from the config

Claude Desktop writes its own settings into that same file. If your edit leaves
it with a JSON syntax error, the app can't parse `mcpServers`, and its next write
persists the file **without your servers**. Before saving, validate:

```bash
python3 -m json.tool < ~/Library/Application\ Support/Claude/claude_desktop_config.json > /dev/null && echo OK
```

Keep a copy of your server entries somewhere outside that file, so restoring is
a paste rather than an archaeology exercise.

## Trying the tools

Ask for each one by name the first time, to confirm it's wired up:

> Use the calculate tool to work out 2 + 2 * 3

Returns `8`. Then something the model can't answer alone:

> I just got an email from ar@globex.example. Which customer is that?

`search_customers` finds Globex Industries. Then a write:

> Add Dana Wu (dana@globex.example) as a contact there

`add_contact` creates the record and returns it.

**You will notice Claude often answers arithmetic without calling `calculate`.**
That is expected, and it is the most useful thing in this class: a model reaches
for a tool when the tool offers something it cannot do itself. `calculate` is
here to teach the mechanics — typed parameters, schema validation, error results —
while `search_customers` and `add_contact` show why tools exist at all. Neither
the customer list nor the ability to write to it lives inside the model.

## Trying the resources

Resources are **read-only context**, not actions. Where a tool is something the
model decides to *do*, a resource is something the user or client chooses to put
in front of it — closer to attaching a file than to calling a function. In Claude
Desktop they appear under the attachment menu rather than the tools list.

`acme-resources` exposes three, covering the shapes you'll meet in practice:

| URI                                | Kind          | Returns                                    |
| ---------------------------------- | ------------- | ------------------------------------------ |
| `customers://directory`            | fixed         | JSON array of every customer               |
| `customers://{customerId}`         | template      | one customer plus its contacts, as JSON    |
| `customers://{customerId}/badge.png` | template    | a PNG, returned as a base64 blob           |

The fixed resource is listed by `resources/list`; the two templates appear under
`resources/templates/list` and are filled in when read. Reading an unknown
customer returns a `-32002 Resource not found` error rather than an empty result,
so the client can tell "no such thing" from "nothing there".

`NotifyingCustomerRepository` wraps the Class 3 repository and fires a
notification when data changes, which is what makes `.resources(true, true)`
— subscribe and listChanged — more than a declaration. Add a contact through
`acme-tools` and a client subscribed to that customer's resource is told to
re-read it.

## Project layout

```
pom.xml                                    Maven build
src/main/resources/logback.xml             Logging config (see the note below)
src/main/java/com/themcpguy/
├── HelloMcpServer.java                    Class 2 — the 'echo' server, unchanged
├── tools/                                 Class 3 — the 'acme-tools' server
│   ├── ToolsMcpServer.java                Entry point; wires the three tools together
│   ├── CalculateTool.java                 Sync tool — pure computation
│   ├── ExpressionEvaluator.java           Recursive-descent parser behind 'calculate'
│   ├── SearchCustomersTool.java           Async tool — read from the repository
│   ├── AddContactTool.java                Async tool — write that changes state
│   ├── CustomerRepository.java            The seam a real JPA/HTTP backend would replace
│   │                                        (Class 4 adds allAsync + contactsForAsync)
│   ├── AsyncSpecs.java                    Adapts a sync spec so an async server can host it
│   └── Results.java                       Shared error-result helper
└── resources/                             Class 4 — the 'acme-resources' server
    ├── ResourcesMcpServer.java            Entry point; declares subscribe + listChanged
    ├── CustomerDirectoryResource.java     Fixed URI — the whole directory as JSON
    ├── CustomerProfileResource.java       Template URI — one customer, with contacts
    ├── CustomerBadgeResource.java         Template URI — binary PNG as a base64 blob
    └── NotifyingCustomerRepository.java   Decorator that fires change notifications
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
