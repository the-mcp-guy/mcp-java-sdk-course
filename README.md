# MCP Java SDK Course — Companion Code

Companion code for **[Building MCP Servers in Java](https://themcpguy.com/docs/mcp-java-sdk/environment-setup)**
at [themcpguy.com](https://themcpguy.com), a nine-class course on writing MCP
servers with the MCP Java SDK.

Start here: **[Class 1 — Environment Setup](https://themcpguy.com/docs/mcp-java-sdk/environment-setup)**.

`main` holds the finished project — the end state of all nine classes, with every
server the course builds. If you are following along, check out the branch for
the class you are on instead.

## What each class teaches

| Class                                                                                              | Branch    | Coverage                              |
| -------------------------------------------------------------------------------------------------- | --------- | ------------------------------------- |
| [Class 1 — Environment Setup](https://themcpguy.com/docs/mcp-java-sdk/environment-setup)           | `class_1` | Prerequisites and environment setup   |
| [Class 2 — Your First MCP Server](https://themcpguy.com/docs/mcp-java-sdk/your-first-mcp-server)   | `class_2` | Echo server over stdio                |
| [Class 3 — Implementing Tools](https://themcpguy.com/docs/mcp-java-sdk/implementing-tools)         | `class_3` | Three tools and model selection logic |
| [Class 4 — Implementing Resources](https://themcpguy.com/docs/mcp-java-sdk/implementing-resources) | `class_4` | Resources, templates, binary content  |
| [Class 5 — Implementing Prompts](https://themcpguy.com/docs/mcp-java-sdk/implementing-prompts)     | `class_5` | Prompts and argument validation       |
| [Class 6 — Error Handling](https://themcpguy.com/docs/mcp-java-sdk/error-handling)                 | `class_6` | Error handling and failed responses   |
| [Class 7 — Testing MCP Servers](https://themcpguy.com/docs/mcp-java-sdk/testing)                   | `class_7` | Unit and integration tests            |
| [Class 8 — Security](https://themcpguy.com/docs/mcp-java-sdk/security)                             | —         | Security considerations               |
| [Class 9 — MCP over HTTP](https://themcpguy.com/docs/mcp-java-sdk/mcp-over-http)                   | `class_9` | Same server over HTTP                 |

Every class that adds code has its own branch, so you can check out the exact
state of the project at any point in the series. Class 8 is the exception: it is
a reading class, and the one server it asks you to write is meant to be deleted
once you have seen it work.

Each branch carries its own README with that module's build and run
instructions, and is a complete, self-contained project — the **end state** of
the module, not a diff you have to apply.

## Getting the code

```bash
git clone https://github.com/the-mcp-guy/mcp-java-sdk-course.git
cd mcp-java-sdk-course
git checkout class_1        # or whichever class you're on
```

To see what changed between two classes:

```bash
git diff class_6 class_7
```

## Prerequisites

These hold for every class in the series:

- **Java 17 minimum, Java 21 recommended.** The MCP Java SDK requires 17; the
  course targets 21 for virtual threads.
- **Maven 3.9+**
- **Claude Desktop**, used as the MCP client that launches your stdio server.
- **Claude Code or `curl`**, for the HTTP server in Class 9.

```bash
java -version
mvn -version
```

Note that `mvn -version` reports the JDK Maven itself runs on — that's the one
that determines your build, so check it as well as `java -version`.

## Building and running this branch

```bash
mvn clean package
```

That produces one JAR containing five stdio servers, each with its own entry
point:

```bash
# Class 2 — echo, the JAR's manifest main class
java -jar target/mcp-java-sdk-course-1.0.0-SNAPSHOT.jar

# Classes 3 to 6 — named explicitly on the classpath
java -cp target/mcp-java-sdk-course-1.0.0-SNAPSHOT.jar com.themcpguy.tools.ToolsMcpServer
java -cp target/mcp-java-sdk-course-1.0.0-SNAPSHOT.jar com.themcpguy.resources.ResourcesMcpServer
java -cp target/mcp-java-sdk-course-1.0.0-SNAPSHOT.jar com.themcpguy.prompts.PromptsMcpServer
java -cp target/mcp-java-sdk-course-1.0.0-SNAPSHOT.jar com.themcpguy.errors.ErrorsMcpServer BROKEN
```

Each one blocks after a single startup line on stderr, waiting for a client to
speak first over stdin. That silence is correct.

The sixth server is the Class 9 one, over HTTP on `http://127.0.0.1:8080/mcp`:

```bash
mvn spring-boot:run
```

This one will not run from the JAR — the shade plugin overwrites the `META-INF`
descriptors Spring Boot uses to find its own configuration, so `application.yml`
is never read.

Run the tests with:

```bash
mvn verify        # 17 unit tests, then package, then 11 integration tests
mvn test          # unit tests only
```

The [`class_9` README](https://github.com/the-mcp-guy/mcp-java-sdk-course/blob/class_9/README.md)
covers all of this in detail, including connecting each server to Claude Desktop
and driving the HTTP one from `curl`.

## License

MIT — see [LICENSE](LICENSE).
