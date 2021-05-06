# Web Socket &bull; Protocol

Document describing the web-socket protocol.

## Opening a Connection

Opening a connection to the web-socket server is pretty straightforward. The following headers\* must be supplied.

```
Authorization: Password matching the obsidian.yml file**
User-Id: The user id of the bot you're playing music with
Client-Name: Name of your bot or project**
Resume-Key: The resume key (like lavalink), however this is only needed if the session needs to be resumed.
```

## Close Codes

| Close Code | Reason                                               |
| ---------- | ---------------------------------------------------- |
| 4001       | Invalid or missing authorization                     |
| 4002       | Missing `Client-Name` header or query-parameter      |
| 4003       | Missing `User-Id` header or query-parameter          |
| 4005       | A session for the supplied user already exists.      |
| 4006       | An error occurred while handling a received payload. |

## Payload Structure

- **op**: numeric op code
- **d**: payload data

```json
{
  "op": 69,
  "d": {}
}
```

See [**/payloads.md**](/payloads.md) for all available payloads

---

<sub>
\* query parameters will be used if a header isn't found  
\*\* varies depending on the configuration of the node you're connecting to
</sub>

