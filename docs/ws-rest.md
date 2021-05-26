# WebSocket & REST 

Magma is the name of our WebSocket & REST server, we use [Ktor](https://ktor.io) because it is completely in Kotlin and is super fast!

## Conventions

Conventions used by Obsidian.

### Naming

Everything should use `snake_case`, this includes payloads that are being sent and received.

<sub>For payloads that are being sent, the serialization library used by Obsidian can detect pascal & camel case fields. This does not mean you should use said naming conventions.</sub>

