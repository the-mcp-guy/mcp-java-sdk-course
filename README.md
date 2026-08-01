# MCP Java SDK Course — Companion Code

Companion code for **[Building MCP Servers in Java](https://themcpguy.com/docs/mcp-java-sdk/environment-setup)**
at [themcpguy.com](https://themcpguy.com).

> **This branch has no code.** `main` is just the index. The code for each class
> lives on its own branch — pick one from the table below.

## Classes

| Branch    | Module                                                                                    | What you build                                              |
| --------- | ----------------------------------------------------------------------------------------- | ----------------------------------------------------------- |
| `class_1` | [Module 1 — Environment Setup](https://themcpguy.com/docs/mcp-java-sdk/environment-setup) | A Java 21 Maven project with the MCP SDK on the classpath   |

More classes are added as the course progresses.

## Getting the code

```bash
git clone https://github.com/the-mcp-guy/mcp-java-sdk-course.git
cd mcp-java-sdk-course
git checkout class_1
```

Each class branch is a complete, self-contained project — the **end state** of
that module, not a diff you have to apply. Check out the branch for the class
you're on and it should build and run as-is. Every class branch carries its own
README with that module's build and run instructions.

To see exactly what changed between two classes:

```bash
git diff class_1 class_2
```

## Prerequisites

These hold for every class in the series:

- **Java 17 minimum, Java 21 recommended.** The MCP Java SDK requires 17; the
  course targets 21 for virtual threads.
- **Maven 3.9+**
- **Claude Desktop**, used as the MCP client that launches your server.

```bash
java -version
mvn -version
```

Note that `mvn -version` reports the JDK Maven itself runs on — that's the one
that determines your build, so check it as well as `java -version`.

## License

MIT — see [LICENSE](LICENSE).
